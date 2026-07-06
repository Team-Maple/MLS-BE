package com.maple.api.item.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.ItemBookmarkSearchRequestDto;
import com.maple.api.bookmark.repository.BookmarkQueryDslRepository;
import com.maple.api.bookmark.repository.BookmarkQueryDslRepositoryImpl;
import com.maple.api.common.presentation.config.QueryDslConfig;
import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.domain.Item;
import com.maple.api.job.domain.Job;
import com.maple.api.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@ActiveProfiles("test")
@Import({QueryDslConfig.class, ItemQueryDslRepositoryImpl.class, BookmarkQueryDslRepositoryImpl.class})
class ItemJobRepositoryTransitionTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ItemQueryDslRepository itemQueryDslRepository;

    @Autowired
    private BookmarkQueryDslRepository bookmarkQueryDslRepository;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        insertJob(100, "전사");
        insertJob(200, "법사");

        insertEquipmentItem(7000, "전사 아이템");
        insertEquipmentItem(7001, "법사 아이템");
        insertEquipmentItem(7002, "전체착용 아이템");

        insertItemJob(7000, 100);
        insertItemJob(7001, 200);

        jdbcTemplate.update("""
                INSERT INTO member (
                    id, provider, nickname, fcm_token,
                    marketing_agreement, notice_agreement, patch_note_agreement, event_agreement,
                    level, job_id, profile_image_url, created_at, updated_at
                )
                VALUES (?, 'KAKAO', 'tester', '', false, false, false, false, null, null, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, "member-1");

        insertItemBookmark(7000);
        insertItemBookmark(7001);
        insertItemBookmark(7002);
    }

    @Nested
    @DisplayName("아이템 검색 직업 필터")
    class ItemSearchJobFilter {

        @Test
        @DisplayName("전사 계열과 전체착용 아이템을 반환한다")
        void searchWarriorCategoryAndCommonItems() {
            Page<Item> result = itemQueryDslRepository.searchItems(
                    new ItemSearchRequestDto(null, List.of(100), null, null, null),
                    PAGEABLE
            );

            assertThat(itemIds(result)).containsExactly(7000, 7002);
        }

        @Test
        @DisplayName("법사 계열과 전체착용 아이템을 반환한다")
        void searchMageCategoryAndCommonItems() {
            Page<Item> result = itemQueryDslRepository.searchItems(
                    new ItemSearchRequestDto(null, List.of(200), null, null, null),
                    PAGEABLE
            );

            assertThat(itemIds(result)).containsExactly(7001, 7002);
        }

        @Test
        @DisplayName("직업 필터가 비어 있으면 전체 아이템을 반환한다")
        void skipJobFilterWhenJobIdsAreEmpty() {
            Page<Item> result = itemQueryDslRepository.searchItems(
                    new ItemSearchRequestDto(null, List.of(), null, null, null),
                    PAGEABLE
            );

            assertThat(itemIds(result)).containsExactly(7000, 7001, 7002);
        }
    }

    @Nested
    @DisplayName("아이템 북마크 직업 필터")
    class ItemBookmarkJobFilter {

        @Test
        @DisplayName("전사 계열과 전체착용 아이템 북마크를 반환한다")
        void searchWarriorCategoryAndCommonBookmarks() {
            Page<BookmarkSummaryDto> result = bookmarkQueryDslRepository.searchItemBookmarks(
                    "member-1",
                    new ItemBookmarkSearchRequestDto(List.of(100), null, null, null),
                    PAGEABLE
            );

            assertThat(bookmarkOriginalIds(result)).containsExactlyInAnyOrder(7000, 7002);
        }

        @Test
        @DisplayName("법사 계열과 전체착용 아이템 북마크를 반환한다")
        void searchMageCategoryAndCommonBookmarks() {
            Page<BookmarkSummaryDto> result = bookmarkQueryDslRepository.searchItemBookmarks(
                    "member-1",
                    new ItemBookmarkSearchRequestDto(List.of(200), null, null, null),
                    PAGEABLE
            );

            assertThat(bookmarkOriginalIds(result)).containsExactlyInAnyOrder(7001, 7002);
        }

        @Test
        @DisplayName("직업 필터가 비어 있으면 전체 아이템 북마크를 반환한다")
        void skipJobFilterWhenJobIdsAreEmpty() {
            Page<BookmarkSummaryDto> result = bookmarkQueryDslRepository.searchItemBookmarks(
                    "member-1",
                    new ItemBookmarkSearchRequestDto(List.of(), null, null, null),
                    PAGEABLE
            );

            assertThat(bookmarkOriginalIds(result)).containsExactlyInAnyOrder(7000, 7001, 7002);
        }
    }

    @Nested
    @DisplayName("아이템 상세 직업 목록")
    class ItemDetailAvailableJobs {

        @Test
        @DisplayName("계열 아이템은 base 계열 job을 반환한다")
        void findCategoryJobByItemId() {
            List<Job> result = jobRepository.findByItemId(7000);

            assertThat(result)
                    .extracting(Job::getJobId, Job::getJobName)
                    .containsExactly(tuple(100, "전사"));
        }

        @Test
        @DisplayName("전체착용 아이템은 빈 직업 목록을 반환한다")
        void findNoJobsForCommonItem() {
            List<Job> result = jobRepository.findByItemId(7002);

            assertThat(result).isEmpty();
        }
    }

    private void insertJob(int jobId, String jobName) {
        jdbcTemplate.update("""
                INSERT INTO jobs (job_id, job_name, job_level, disabled, created_at, updated_at)
                VALUES (?, ?, 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, jobId, jobName);
    }

    private void insertEquipmentItem(int itemId, String nameKr) {
        jdbcTemplate.update("""
                INSERT INTO items (
                    item_id, item_type, name_kr, name_en, item_image_url,
                    npc_price, category_id, required_level, created_at, updated_at
                )
                VALUES (?, 'EQUIPMENT', ?, ?, '', 0, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, itemId, nameKr, nameKr);
    }

    private void insertItemJob(int itemId, int jobCategoryId) {
        jdbcTemplate.update("""
                INSERT INTO item_jobs (item_id, job_category_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, itemId, jobCategoryId);
    }

    private void insertItemBookmark(int itemId) {
        jdbcTemplate.update("""
                INSERT INTO bookmarks (member_id, bookmark_type, resource_id, created_at, updated_at)
                VALUES ('member-1', 'ITEM', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, itemId);
    }

    private List<Integer> itemIds(Page<Item> page) {
        return page.getContent().stream()
                .map(Item::getItemId)
                .toList();
    }

    private List<Integer> bookmarkOriginalIds(Page<BookmarkSummaryDto> page) {
        return page.getContent().stream()
                .map(BookmarkSummaryDto::originalId)
                .toList();
    }
}
