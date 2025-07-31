package com.maple.api.alrim.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author jin
 *
 * 공지사항, 패치노트, 이벤트 알림을 저장하는 엔티티
 */
@Entity
@Table(name = "alrim")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Alrim extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "varchar(31)")
  @Enumerated(EnumType.STRING)
  private AlrimType type;

  private String title;
  private LocalDateTime date;
  private String link;

  @Setter
  private Boolean outdated = false;

  public static Alrim createNotice(
    String title,
    LocalDateTime date,
    String link
  ) {
    return new Alrim(
      null, AlrimType.NOTICE, title, date, link, false
    );
  }

  public static Alrim createPatchNote(
    String title,
    LocalDateTime date,
    String link
  ) {
    return new Alrim(
      null, AlrimType.PATCH_NOTE, title, date, link, false
    );
  }

  public static Alrim createEvents(
    String title,
    LocalDateTime date,
    String link
  ) {
    return new Alrim(
      null, AlrimType.EVENT, title, date, link, false
    );
  }
}