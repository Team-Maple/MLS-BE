package com.maple.api.auth.domain;

import jakarta.persistence.Entity;
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

  private String email;

  private Provider provider;

  @Nullable
  private String userName;

  // private String role;
  public String getRole() {
    return "ROLE_MEMBER";
  }


  @CreationTimestamp
  private LocalDateTime createdAt = LocalDateTime.now();

  @CreationTimestamp
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Member(
    String providerId, String email, Provider provider
  ) {
    this.id = providerId;
    this.email = email;
    this.provider = provider;
    this.userName = provider.name() + "_" + providerId;
  }

  public Member update(String userName){
    this.userName = userName;
    return this;
  }
}
