package com.maple.api.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProdFileLoggingForkIntegrationTest {

    private static final String ACTIVE_FILE = "mapleland-api.json";
    private static final int MAX_SAFE_STACK_TRACE_CHARS = 16 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void prodProfileWritesEcsJsonLinesAndRotatesTheRealFileAppender() throws Exception {
        Path logDirectory = Files.createDirectory(temporaryDirectory.resolve("logs"));
        Path forkOutput = temporaryDirectory.resolve("fork-output.txt");

        ProcessBuilder processBuilder = new ProcessBuilder(
            javaExecutable().toString(),
            "-cp",
            forkClasspath(),
            ProdLoggingForkFixture.class.getName());
        processBuilder.environment().put("LOG_DIR", logDirectory.toString());
        processBuilder.environment().put("SERVICE_VERSION", "fork-test-version");
        processBuilder.environment().put("MANAGEMENT_SCRAPE_TOKEN", "fork-test-scrape-token");
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(forkOutput.toFile());

        Process process = processBuilder.start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        String processOutput = Files.exists(forkOutput)
            ? Files.readString(forkOutput, StandardCharsets.UTF_8)
            : "";
        assertThat(finished)
            .withFailMessage("forked logging fixture timed out: %s", processOutput)
            .isTrue();
        assertThat(process.exitValue())
            .withFailMessage("forked logging fixture failed: %s", processOutput)
            .isZero();

        List<Path> logFiles;
        try (Stream<Path> files = Files.list(logDirectory)) {
            logFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted()
                .toList();
        }

        assertThat(logFiles)
            .extracting(path -> path.getFileName().toString())
            .contains(ACTIVE_FILE)
            .anyMatch(name -> !ACTIVE_FILE.equals(name));

        List<JsonNode> events = new ArrayList<>();
        StringBuilder retainedLogText = new StringBuilder();
        for (Path logFile : logFiles) {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                assertThat(line).doesNotContain("\n", "\r");
                retainedLogText.append(line).append('\n');
                events.add(objectMapper.readTree(line));
            }
        }
        assertThat(events).isNotEmpty();

        JsonNode marker = events.stream()
            .filter(event -> "observability.prod-file-fixture"
                .equals(event.path("event.action").asText()))
            .findFirst()
            .orElseThrow();
        assertThat(marker.path("@timestamp").asText()).isNotBlank();
        assertThat(marker.path("log.level").asText()).isEqualTo("WARN");
        assertThat(marker.path("log.logger").asText())
            .isEqualTo(ProdLoggingForkFixture.class.getName());
        assertThat(marker.path("process.pid").isNumber()).isTrue();
        assertThat(marker.path("process.thread.name").asText()).isNotBlank();
        assertThat(marker.path("service.name").asText()).isEqualTo("mapleland-api");
        assertThat(marker.path("service.environment").asText()).isEqualTo("prod");
        assertThat(marker.path("service.version").asText()).isEqualTo("fork-test-version");
        assertThat(marker.path("message").asText())
            .isEqualTo("Production file logging fixture completed");

        List<JsonNode> safeExceptionEvents = events.stream()
            .filter(event -> "observability.safe-exception-fixture"
                .equals(event.path("event.action").asText()))
            .toList();
        assertThat(safeExceptionEvents).hasSize(1);

        JsonNode safeException = safeExceptionEvents.getFirst();
        assertThat(safeException.path("error.type").asText())
            .isEqualTo(IllegalStateException.class.getName());
        assertThat(safeException.path("error.message").asText())
            .isEqualTo("Exception message redacted by logging policy");
        assertThat(safeException.path("error.stack_trace").asText())
            .contains(IllegalStateException.class.getName())
            .contains("Caused by: " + IllegalArgumentException.class.getName())
            .contains("com.maple.api.auth.application.AuthService.reissue(AuthService.java:55)")
            .hasSizeLessThanOrEqualTo(MAX_SAFE_STACK_TRACE_CHARS);
        assertThat(retainedLogText.toString())
            .doesNotContain(ProdLoggingForkFixture.SENSITIVE_MEMBER_ID)
            .doesNotContain(ProdLoggingForkFixture.SENSITIVE_ACCESS_TOKEN)
            .doesNotContain(ProdLoggingForkFixture.SENSITIVE_AUTHORIZATION);

        long retainedBytes = 0;
        for (Path logFile : logFiles) {
            retainedBytes += Files.size(logFile);
        }
        assertThat(retainedBytes).isLessThan(128 * 1024);
    }

    private Path javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java");
    }

    private String forkClasspath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        String configuredClasspath = System.getProperty("java.class.path", "");
        if (!configuredClasspath.isBlank()) {
            entries.addAll(Arrays.asList(configuredClasspath.split(
                Pattern.quote(File.pathSeparator))));
        }

        for (ClassLoader loader = Thread.currentThread().getContextClassLoader();
             loader != null;
             loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    if ("file".equals(url.getProtocol())) {
                        URI location = url.toURI();
                        entries.add(Path.of(location).toString());
                    }
                }
            }
        }

        assertThat(entries)
            .anyMatch(entry -> entry.contains("classes/java/test"));
        return String.join(File.pathSeparator, entries);
    }
}
