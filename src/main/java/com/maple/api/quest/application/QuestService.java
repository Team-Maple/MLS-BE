package com.maple.api.quest.application;

import com.maple.api.item.domain.Item;
import com.maple.api.item.repository.ItemRepository;
import com.maple.api.job.domain.Job;
import com.maple.api.job.repository.JobRepository;
import com.maple.api.monster.domain.Monster;
import com.maple.api.monster.repository.MonsterRepository;
import com.maple.api.quest.application.dto.*;
import com.maple.api.quest.domain.*;
import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.quest.exception.QuestException;
import com.maple.api.quest.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestQueryDslRepository questQueryDslRepository;
    private final QuestRepository questRepository;
    private final QuestRewardRepository questRewardRepository;
    private final QuestRewardItemRepository questRewardItemRepository;
    private final QuestRequirementRepository questRequirementRepository;
    private final QuestAllowedJobRepository questAllowedJobRepository;
    private final QuestChainMemberRepository questChainMemberRepository;
    private final ItemRepository itemRepository;
    private final MonsterRepository monsterRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public Page<QuestSummaryDto> searchQuests(QuestSearchRequestDto request, Pageable pageable) {
        Page<Quest> questPage = questQueryDslRepository.searchQuests(request, pageable);
        return questPage.map(QuestSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public QuestDetailDto getQuestDetail(Integer questId) {
        Quest quest = findQuest(questId);
        
        QuestReward reward = findQuestReward(questId);
        List<QuestRewardItem> rewardItems = findQuestRewardItems(questId);
        List<QuestRequirement> requirements = findQuestRequirements(questId);
        List<QuestAllowedJob> allowedJobs = findQuestAllowedJobs(questId);

        Map<Integer, String> itemNameMap = fetchItemNames(rewardItems, requirements);
        Map<Integer, String> monsterNameMap = fetchMonsterNames(requirements);
        Map<Integer, String> jobNameMap = fetchJobNames(allowedJobs);

        return buildQuestDetailDto(quest, reward, rewardItems, requirements, allowedJobs, 
                                 itemNameMap, monsterNameMap, jobNameMap);
    }

    private Quest findQuest(Integer questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> ApiException.of(QuestException.QUEST_NOT_FOUND));
    }

    private QuestReward findQuestReward(Integer questId) {
        return questRewardRepository.findByQuestId(questId).orElse(null);
    }

    private List<QuestRewardItem> findQuestRewardItems(Integer questId) {
        return questRewardItemRepository.findByQuestId(questId);
    }

    private List<QuestRequirement> findQuestRequirements(Integer questId) {
        return questRequirementRepository.findByQuestId(questId);
    }

    private List<QuestAllowedJob> findQuestAllowedJobs(Integer questId) {
        return questAllowedJobRepository.findByQuestId(questId);
    }

    private Map<Integer, String> fetchItemNames(List<QuestRewardItem> rewardItems, List<QuestRequirement> requirements) {
        List<Integer> itemIds = rewardItems.stream()
                .map(QuestRewardItem::getItemId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Integer> requirementItemIds = requirements.stream()
                .map(QuestRequirement::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        itemIds.addAll(requirementItemIds);
        
        return itemRepository.findByItemIdIn(itemIds.stream().distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Item::getNameKr));
    }

    private Map<Integer, String> fetchMonsterNames(List<QuestRequirement> requirements) {
        List<Integer> monsterIds = requirements.stream()
                .map(QuestRequirement::getMonsterId)
                .filter(monsterId -> monsterId != null)
                .distinct()
                .collect(Collectors.toList());
        
        return monsterRepository.findAllById(monsterIds)
                .stream()
                .collect(Collectors.toMap(Monster::getMonsterId, Monster::getNameKr));
    }

    private Map<Integer, String> fetchJobNames(List<QuestAllowedJob> allowedJobs) {
        List<Integer> jobIds = allowedJobs.stream()
                .map(QuestAllowedJob::getJobId)
                .distinct()
                .collect(Collectors.toList());
        
        return jobRepository.findAllById(jobIds)
                .stream()
                .collect(Collectors.toMap(Job::getJobId, Job::getJobName));
    }

    private QuestDetailDto buildQuestDetailDto(Quest quest, QuestReward reward, 
                                             List<QuestRewardItem> rewardItems, List<QuestRequirement> requirements,
                                             List<QuestAllowedJob> allowedJobs, Map<Integer, String> itemNameMap,
                                             Map<Integer, String> monsterNameMap, Map<Integer, String> jobNameMap) {
        QuestRewardDto rewardDto = reward != null ? QuestRewardDto.toDto(reward) : null;

        List<QuestRewardItemDto> rewardItemDtos = rewardItems.stream()
                .map(item -> QuestRewardItemDto.toDto(item, itemNameMap.get(item.getItemId())))
                .collect(Collectors.toList());

        List<QuestRequirementDto> requirementDtos = requirements.stream()
                .map(req -> QuestRequirementDto.toDto(req, 
                        req.getItemId() != null ? itemNameMap.get(req.getItemId()) : null,
                        req.getMonsterId() != null ? monsterNameMap.get(req.getMonsterId()) : null))
                .collect(Collectors.toList());

        List<QuestJobDto> allowedJobDtos = allowedJobs.stream()
                .map(job -> new QuestJobDto(job.getJobId(), jobNameMap.get(job.getJobId())))
                .collect(Collectors.toList());

        return QuestDetailDto.toDto(quest, rewardDto, rewardItemDtos, requirementDtos, allowedJobDtos);
    }

    @Transactional(readOnly = true)
    public QuestChainResponseDto getQuestChain(Integer questId) {
        findQuest(questId);
        
        QuestChainMember currentChainMember = findQuestChainMember(questId);
        if (currentChainMember == null) {
            return QuestChainResponseDto.of(List.of(), List.of());
        }
        
        List<QuestChainMember> allChainMembers = findAllChainMembers(currentChainMember.getChainId());
        Integer currentSequenceOrder = currentChainMember.getSequenceOrder();
        
        List<QuestChainDto> previousQuests = buildPreviousQuests(allChainMembers, currentSequenceOrder);
        List<QuestChainDto> nextQuests = buildNextQuests(allChainMembers, currentSequenceOrder);
        
        return QuestChainResponseDto.of(previousQuests, nextQuests);
    }

    private QuestChainMember findQuestChainMember(Integer questId) {
        return questChainMemberRepository.findByQuestId(questId).orElse(null);
    }

    private List<QuestChainMember> findAllChainMembers(Integer chainId) {
        return questChainMemberRepository.findByChainId(chainId);
    }

    private List<QuestChainDto> buildPreviousQuests(List<QuestChainMember> allChainMembers, Integer currentSequenceOrder) {
        List<Integer> previousQuestIds = allChainMembers.stream()
                .filter(member -> member.getSequenceOrder() < currentSequenceOrder)
                .sorted((a, b) -> Integer.compare(b.getSequenceOrder(), a.getSequenceOrder()))
                .map(QuestChainMember::getQuestId)
                .toList();
        
        return buildQuestChainDtos(previousQuestIds);
    }

    private List<QuestChainDto> buildNextQuests(List<QuestChainMember> allChainMembers, Integer currentSequenceOrder) {
        List<Integer> nextQuestIds = allChainMembers.stream()
                .filter(member -> member.getSequenceOrder() > currentSequenceOrder)
                .sorted(Comparator.comparing(QuestChainMember::getSequenceOrder))
                .map(QuestChainMember::getQuestId)
                .toList();
        
        return buildQuestChainDtos(nextQuestIds);
    }

    private List<QuestChainDto> buildQuestChainDtos(List<Integer> questIds) {
        if (questIds.isEmpty()) {
            return List.of();
        }
        
        Map<Integer, Quest> questMap = questRepository.findAllById(questIds).stream()
                .collect(Collectors.toMap(Quest::getQuestId, java.util.function.Function.identity()));

        return questIds.stream()
                .map(questMap::get)
                .filter(Objects::nonNull)
                .map(QuestChainDto::toDto)
                .collect(Collectors.toList());
    }
}