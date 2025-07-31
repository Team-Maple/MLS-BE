package com.maple.api.alrim.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author jin
 *
 * 유저별 읽음 여부를 저장하는 Many To Many Entity
 */
@Entity
@Table(name = "alrim_read", indexes = {
  @Index(name = "idx_alrim_read_member_id_alrim_link", columnList = "memberId, alrimLink"),
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AlrimRead {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String alrimLink;
  private String memberId;

  public AlrimRead(String alrimLink, String memberId) {
    this.alrimLink = alrimLink;
    this.memberId = memberId;
  }
}
