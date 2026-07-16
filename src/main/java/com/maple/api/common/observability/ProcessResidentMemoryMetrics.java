package com.maple.api.common.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exposes Linux process RSS with no additional native or agent dependency. */
@Component
public final class ProcessResidentMemoryMetrics implements MeterBinder {

    private static final Path PROCESS_STATUS = Path.of("/proc/self/status");

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("process.resident.memory", this, ProcessResidentMemoryMetrics::residentBytes)
            .baseUnit("bytes")
            .description("Resident set size of the application process")
            .register(registry);
    }

    double residentBytes() {
        try (BufferedReader reader = Files.newBufferedReader(PROCESS_STATUS, StandardCharsets.UTF_8)) {
            return parseResidentBytes(reader);
        } catch (IOException | NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    static long parseResidentBytes(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("VmRSS:")) {
                String[] fields = line.trim().split("\\s+");
                if (fields.length >= 3 && "kB".equals(fields[2])) {
                    return Math.multiplyExact(Long.parseLong(fields[1]), 1024L);
                }
            }
        }
        throw new IOException("VmRSS is not available in /proc/self/status");
    }
}
