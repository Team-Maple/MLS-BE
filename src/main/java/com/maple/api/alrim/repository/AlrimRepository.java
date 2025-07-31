package com.maple.api.alrim.repository;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlrimRepository extends JpaRepository<Alrim, Long> {
  // 가장 최근 알림 보기
  Optional<Alrim> findTopByTypeOrderByDateDesc(AlrimType type);

  // 모든 타입의 알림 보기
  List<Alrim> findAllByDateAfterOrderByDateDesc(LocalDateTime dateTime);

  // 이벤트 타입중 현재 진행중인 이벤트만 보기
  List<Alrim> findAllByOutdatedIsFalseAndTypeOrderByDateDesc(AlrimType type);
  // 이벤트 타입중 현재 진행중인 이벤트만 보기
  List<Alrim> findAllByOutdatedIsTrueAndTypeAndDateAfterOrderByDateDesc(AlrimType type, LocalDateTime dateTime);

  // 타입별 알림 보기
  List<Alrim> findAllByTypeAndDateAfterOrderByDateDesc(AlrimType type, LocalDateTime dateTime);
}
