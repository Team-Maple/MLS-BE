package com.maple.api.alrim.repository;

import com.maple.api.alrim.domain.AlrimRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlrimReadRepository extends JpaRepository<AlrimRead, Long> {
  List<AlrimRead> findAllByMemberIdAndAlrimLinkIn(String memberId, List<String> alrimLinks);
}
