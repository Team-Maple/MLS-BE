package com.maple.api.alrim.application.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.maple.api.auth.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AlrimFcmCredentialsTest {
  @TempDir Path directory;

  @AfterEach
  void clearFirebase() {
    FirebaseApp.getApps().forEach(FirebaseApp::delete);
  }

  @Test
  void initializesFromExternalFileWithoutBundledCredentials() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pem = "-----BEGIN PRIVATE KEY-----\n"
        + Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
        + "\n-----END PRIVATE KEY-----\n";
    var path = directory.resolve("firebase.json");
    new ObjectMapper().writeValue(path.toFile(), Map.of(
        "type", "service_account", "project_id", "test-project",
        "private_key_id", "test-key", "private_key", pem,
        "client_email", "test@test-project.iam.gserviceaccount.com",
        "client_id", "123456789", "token_uri", "https://oauth2.googleapis.com/token"));
    var manager = manager(path);
    manager.init();
    assertThat(FirebaseApp.getInstance().getOptions().getProjectId()).isEqualTo("test-project");
  }

  @Test
  void rejectsMissingOrInvalidFileInsteadOfSilentlyDisablingNotifications() throws Exception {
    var path = directory.resolve("firebase.json");
    assertThatThrownBy(() -> manager(path).init()).isInstanceOf(IllegalStateException.class);
    Files.writeString(path, "{}");
    assertThatThrownBy(() -> manager(path).init()).isInstanceOf(IllegalStateException.class);
    assertThat(FirebaseApp.getApps()).isEmpty();
  }

  private AlrimFcmManager manager(Path path) {
    var manager = new AlrimFcmManager(mock(MemberRepository.class));
    ReflectionTestUtils.setField(manager, "firebaseCredentials", new FileSystemResource(path));
    return manager;
  }
}
