package com.maple.api.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {
  @Id
  private String id; // = Social ID

  @Enumerated(EnumType.STRING)
  private Provider provider;

  @Nullable
  private String nickname;

  // private String role;
  public String getRole() {
    return "ROLE_MEMBER";
  }


  @CreationTimestamp
  private LocalDateTime createdAt = LocalDateTime.now();

  @CreationTimestamp
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Member(
    String providerId, Provider provider, @Nullable String nickname
  ) {
    this.id = providerId;
    this.provider = provider;
    this.nickname = nickname != null ? nickname :"용감한 주황버섯";
  }

  public Member update(String nickname){
    this.nickname = nickname;
    return this;
  }
}
