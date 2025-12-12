package com.maple.api.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {
  private static final String[] MONSTERS = {
    "주황버섯", "슬라임", "골렘", "발록", "자쿰", "와일드카고", "와일드보어", "머쉬맘",
  };

  private static final String[] EMOTIONS = {
    "용감한", "기쁜", "지친", "신난", "냉정한", "따뜻한", "차가운", "의욕적인"
  };

  @Id
  private String id; // = Social ID

  @Enumerated(EnumType.STRING)
  private Provider provider;

  @Setter
  @Column(unique = true)
  private String nickname;

  @Setter
  private String fcmToken;

  @Setter
  @Builder.Default
  private Boolean marketingAgreement = false;

  @Builder.Default
  private Boolean noticeAgreement = false;

  @Builder.Default
  private Boolean patchNoteAgreement = false;

  @Builder.Default
  private Boolean eventAgreement = false;

  @Setter
  private Integer level;

  @Setter
  private Integer jobId;

  private String profileImageUrl = "https://maple-db-team-s3.s3.ap-northeast-2.amazonaws.com/profile-images/profile_1.jpg";

  // private String role;
  public String getRole() {
    return "ROLE_MEMBER";
  }

  @CreatedDate
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @LastModifiedDate
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Member(
    String providerId,
    Provider provider,
    @Nullable String nickname,
    String fcmToken,
    Boolean marketingAgreement
  ) {
    this.id = providerId;
    this.provider = provider;
    this.nickname = nickname != null ? nickname : createRandomName();
    this.fcmToken = fcmToken;
    this.marketingAgreement = marketingAgreement;
  }

  public Member setAlertAgreements(
    Boolean noticeAgreement,
    Boolean patchNoteAgreement,
    Boolean eventAgreement
  ) {
    this.noticeAgreement = noticeAgreement;
    this.patchNoteAgreement = patchNoteAgreement;
    this.eventAgreement = eventAgreement;
    return this;
  }

  public Member setProfileImageUrl(
    String profileImageUrl
  ) {
    this.profileImageUrl = profileImageUrl;
    return this;
  }

  private String createRandomName() {
    Random random = new Random();

    String emotion = EMOTIONS[random.nextInt(EMOTIONS.length)];
    String monster = MONSTERS[random.nextInt(MONSTERS.length)];
    int number = 100 + random.nextInt(900); // 100 ~ 999

    return emotion + " " + monster + "-" + number;
  }
}
