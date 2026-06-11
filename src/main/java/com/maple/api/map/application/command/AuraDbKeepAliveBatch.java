package com.maple.api.map.application.command;

import com.maple.api.map.repository.MapRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.auradb-keep-alive.enabled", havingValue = "true")
public class AuraDbKeepAliveBatch {

    private final MapRecommendationRepository mapRecommendationRepository;

    // AuraDB Free 티어 미사용 일시중지 방지용 keep-alive
    // 매일 04:00, 16:00 (KST)
    @Scheduled(cron = "0 0 4,16 * * *", zone = "Asia/Seoul")
    public void keepAlive() {
        try {
            mapRecommendationRepository.ping();
            log.info("AuraDB keep-alive 성공");
        } catch (Exception e) {
            log.error("AuraDB keep-alive 실패", e);
        }
    }
}
