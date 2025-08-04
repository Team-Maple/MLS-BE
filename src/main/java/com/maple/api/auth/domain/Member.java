package com.maple.api.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member {
  private static final String[] MONSTERS = {
    "주황버섯", "슬라임", "골렘", "발록", "자쿰", "핑크빈", "혼테일", "반레온", "매그너스", "힐라",
    "블러디퀸", "반반", "벨룸", "루시드", "윌", "더스크", "검은마법사"
  };

  private static final String[] EMOTIONS = {
    "용감한", "슬픈", "화난", "기쁜", "지친", "신난", "겁먹은", "냉정한", "따뜻한", "차가운", "짜증난",
    "졸린", "수줍은", "광기어린", "의욕적인", "무기력한"
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

  private String createRandomName() {
    Random random = new Random();

    String emotion = EMOTIONS[random.nextInt(EMOTIONS.length)];
    String monster = MONSTERS[random.nextInt(MONSTERS.length)];
    int number = 100 + random.nextInt(900); // 100 ~ 999

    return emotion + " " + monster + "-" + number;
  }
}
