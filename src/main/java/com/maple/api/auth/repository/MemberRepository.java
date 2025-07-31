package com.maple.api.auth.repository;

import com.maple.api.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 우선 JpaRepository 만 사용
@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
  List<Member> findAllByFcmTokenIsNotNull();
}
