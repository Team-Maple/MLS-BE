package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.Quest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "퀘스트 상세 정보 DTO")
public record QuestDetailDto(
        @Schema(description = "퀘스트 ID", example = "6002")
        Integer questId,
        
        @Schema(description = "퀘스트 타이틀 접두사", example = "[체험]")
        String titlePrefix,
        
        @Schema(description = "퀘스트명 (한국어)", example = "몬스터 라이딩")
        String nameKr,
        
        @Schema(description = "퀘스트명 (영어)", example = "Monster Riding")
        String nameEn,
        
        @Schema(description = "퀘스트 아이콘 URL", example = "https://maplestory.io/api/gms/62/quest/6002/icon?resize=2")
        String iconUrl,
        
        @Schema(description = "퀘스트 타입", example = "1회성")
        String questType,
        
        @Schema(description = "최소 레벨", example = "70")
        Integer minLevel,
        
        @Schema(description = "최대 레벨", example = "null")
        Integer maxLevel,
        
        @Schema(description = "시작 시 필요한 메소", example = "0")
        Integer requiredMesoStart,
        
        @Schema(description = "시작 NPC ID", example = "2060005")
        Long startNpcId,
        
        @Schema(description = "완료 NPC ID", example = "2060005")
        Long endNpcId,
        
        @Schema(description = "퀘스트 보상 정보")
        QuestRewardDto reward,
        
        @Schema(description = "퀘스트 보상 아이템 목록")
        List<QuestRewardItemDto> rewardItems,
        
        @Schema(description = "퀘스트 완료 조건 목록")
        List<QuestRequirementDto> requirements,
        
        @Schema(description = "허용 직업 목록")
        List<QuestJobDto> allowedJobs
) {
    public static QuestDetailDto toDto(Quest quest, QuestRewardDto reward, List<QuestRewardItemDto> rewardItems, 
                                      List<QuestRequirementDto> requirements, List<QuestJobDto> allowedJobs) {
        return new QuestDetailDto(
                quest.getQuestId(),
                quest.getTitlePrefix(),
                quest.getNameKr(),
                quest.getNameEn(),
                quest.getIconUrl(),
                quest.getQuestType(),
                quest.getMinLevel(),
                quest.getMaxLevel(),
                quest.getRequiredMesoStart(),
                quest.getStartNpcId(),
                quest.getEndNpcId(),
                reward,
                rewardItems,
                requirements,
                allowedJobs
        );
    }
}