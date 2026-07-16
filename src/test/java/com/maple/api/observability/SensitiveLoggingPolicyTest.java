package com.maple.api.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.maple.api.auth.application.AuthService;
import com.maple.api.auth.application.MemberService;
import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;
import com.maple.api.auth.repository.MemberRepository;
import com.maple.api.auth.repository.RefreshTokenRepository;
import com.maple.api.common.presentation.config.JwtTokenProvider;
import com.maple.api.common.presentation.config.JwtTokenValidator;
import com.maple.api.job.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveLoggingPolicyTest {

    private static final String MEMBER_ID = "member-id-that-must-not-leak";
    private static final String NICKNAME = "nickname-that-must-not-leak";
    private static final String NOTIFICATION_TOKEN = "notification-token-that-must-not-leak";

    private final Logger memberLogger = (Logger) LoggerFactory.getLogger(MemberService.class);
    private final Logger authLogger = (Logger) LoggerFactory.getLogger(AuthService.class);
    private final ListAppender<ILoggingEvent> memberEvents = new ListAppender<>();
    private final ListAppender<ILoggingEvent> authEvents = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        memberEvents.start();
        authEvents.start();
        memberLogger.addAppender(memberEvents);
        authLogger.addAppender(authEvents);
    }

    @AfterEach
    void stopCapturingLogs() {
        memberLogger.detachAppender(memberEvents);
        authLogger.detachAppender(authEvents);
        memberEvents.stop();
        authEvents.stop();
    }

    @Test
    void memberAndAuthenticationEventsNeverContainRawIdentifiersOrCredentials() {
        MemberRepository members = mock(MemberRepository.class);
        Member persisted = new Member(MEMBER_ID, Provider.KAKAO, NICKNAME, NOTIFICATION_TOKEN, true);
        when(members.save(any(Member.class))).thenReturn(persisted);
        when(members.findById(MEMBER_ID)).thenReturn(Optional.of(persisted));

        MemberService memberService = new MemberService(members, mock(JobRepository.class));
        memberService.createMember(new CreateMemberRequestDto(
            Provider.KAKAO, MEMBER_ID, NICKNAME, NOTIFICATION_TOKEN, true));
        memberService.updateNickname(MEMBER_ID, NICKNAME);
        memberService.updateFcmToken(MEMBER_ID, NOTIFICATION_TOKEN);
        memberService.deleteMember(MEMBER_ID);

        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        AuthService authService = new AuthService(
            refreshTokens, mock(JwtTokenProvider.class), mock(JwtTokenValidator.class));
        authService.logout(MEMBER_ID);

        String rendered = render(memberEvents) + render(authEvents);
        assertThat(rendered)
            .doesNotContain(MEMBER_ID)
            .doesNotContain(NICKNAME)
            .doesNotContain(NOTIFICATION_TOKEN)
            .doesNotContain("Authorization")
            .doesNotContain("Bearer ");
    }

    private String render(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
            .map(event -> event.getFormattedMessage() + " " + event.getKeyValuePairs().stream()
                .map(KeyValuePair::toString)
                .collect(Collectors.joining(" ")))
            .collect(Collectors.joining("\n"));
    }
}
