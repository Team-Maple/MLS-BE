package com.maple.api.map.application.command;

import com.maple.api.common.logging.SafeExceptionLog;
import com.maple.api.map.repository.AuraMapRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AuraMapRecommendationRepository.class)
@ConditionalOnProperty(name = "batch.auradb-keep-alive.enabled", havingValue = "true")
public class AuraDbKeepAliveBatch {

    private final AuraMapRecommendationRepository auraMapRecommendationRepository;

    // AuraDB Free 티어 미사용 일시중지 방지용 keep-alive
    // 매일 04:00, 16:00 (KST)
    @Scheduled(cron = "0 0 4,16 * * *", zone = "Asia/Seoul")
    public void keepAlive() {
        try {
            auraMapRecommendationRepository.ping();
            log.atInfo()
                .addKeyValue("event.action", "external.keep-alive")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("mapleland.batch.type", "auradb-keep-alive")
                .log("AuraDB keep-alive completed");
        } catch (Exception e) {
            SafeExceptionLog.addException(log.atError(), e)
                .addKeyValue("event.action", "external.keep-alive")
                .addKeyValue("event.outcome", "failure")
                .addKeyValue("mapleland.batch.type", "auradb-keep-alive")
                .log("AuraDB keep-alive failed");
        }
    }
}
