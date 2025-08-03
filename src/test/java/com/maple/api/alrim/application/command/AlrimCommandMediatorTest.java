package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AlrimCommandMediatorTest {
  @Mock
  private AlrimRepository alrimRepository;

  @InjectMocks
  private AlrimCommandMediator commandBatchMediator;

  @Test()
  @DisplayName("이벤트 아웃 데이트 처리 테스트")
  void eventOutDates() throws Exception {
    // given
    LocalDateTime now = LocalDateTime.now();

    var alarm1 = Alrim.createEvents("이벤트", now, "링크 이벤트");
    var alarm2 = Alrim.createEvents("이벤트2", now, "링크 이벤트2");
    var alarm3 = Alrim.createEvents("이벤트2", now, "링크 이벤트3");

    // 이벤트
    given(alrimRepository.findAllByOutdatedIsFalseAndTypeOrderByDateDesc(AlrimType.EVENT)).willReturn(List.of(
      alarm1, alarm2, alarm3
    ));

    // when
    commandBatchMediator.changeEventOutDatedExclude(List.of(alarm3));

    // then
    verify(alrimRepository, times(2)).save(any());
  }
}
