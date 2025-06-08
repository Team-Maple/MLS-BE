package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.application.dto.UpdateMemberRequestDto;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
  private final MemberRepository memberRepository;

  public Optional<MemberDto> findMember(String memberId) {
    return memberRepository.findById(memberId)
      .map(MemberDto::toDto);
  }

  public MemberDto createMember(CreateMemberRequestDto createMemberRequestDto) {
    log.info("Craete member with memberId: {}", createMemberRequestDto.getProviderId());
    Member result = memberRepository.save(new Member(
      createMemberRequestDto.getProviderId(),
      createMemberRequestDto.getEmail(),
      createMemberRequestDto.getProvider()
    ));
    return MemberDto.toDto(result);
  }

  public Optional<MemberDto> updateMember(UpdateMemberRequestDto updateMemberRequestDto) {
    log.info("Update member with memberId: {}", updateMemberRequestDto.getProviderId());
    Optional<Member> member = memberRepository.findById(updateMemberRequestDto.getProviderId());

    return member.map(m -> {
        return m.update(updateMemberRequestDto.getName());
      })
      .map(MemberDto::toDto);
  }


  public void deleteMember(String memberId) {
    log.info("Deleting member with memberId: {}", memberId);
    memberRepository.deleteById(memberId);
  }
}
