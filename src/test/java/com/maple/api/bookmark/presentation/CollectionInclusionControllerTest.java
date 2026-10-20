package com.maple.api.bookmark.presentation;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.CollectionQueryService;
import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.common.presentation.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class CollectionInclusionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CollectionQueryService service;

    private final CollectionWithBookmarksDto collection =
            CollectionWithBookmarksDto.of(new Collection("owner", "collection"), List.of());

    @Test
    void acceptsCommaSeparatedIdsAndReturnsInclusionFields() throws Exception {
        when(service.getCollectionsWithRecentBookmarks(eq("owner"), any(Pageable.class), eq(List.of(101, 102))))
                .thenReturn(new PageImpl<>(List.of(collection.withInclusion(1, 2))));

        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", "101,102")
                        .param("sort", "name,asc").with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].includedCount").value(1))
                .andExpect(jsonPath("$.data[0].allIncluded").value(false))
                .andExpect(jsonPath("$.data[0].recentBookmarks").isArray());
        verify(service).getCollectionsWithRecentBookmarks(eq("owner"),
                argThat(pageable -> pageable.getSort().equals(Sort.by("name"))), eq(List.of(101, 102)));
    }

    @Test
    void omittingIdsPreservesExistingResponseKeys() throws Exception {
        when(service.getCollectionsWithRecentBookmarks(eq("owner"), any(Pageable.class), isNull()))
                .thenReturn(new PageImpl<>(List.of(collection)));
        mockMvc.perform(get("/api/v1/collections").with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("collection"))
                .andExpect(jsonPath("$.data[0].recentBookmarks").isArray())
                .andExpect(jsonPath("$.data[0].includedCount").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data[0].allIncluded").doesNotHaveJsonPath());
    }

    @Test
    void acceptsRepeatedQueryParameters() throws Exception {
        when(service.getCollectionsWithRecentBookmarks(eq("owner"), any(Pageable.class), eq(List.of(101, 102))))
                .thenReturn(new PageImpl<>(List.of(collection.withInclusion(2, 2))));
        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", "101", "102")
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].allIncluded").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "101,101", "0", "-1", "abc", "101,,102"})
    void rejectsInvalidIdsBeforeCallingService(String ids) throws Exception {
        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", ids)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", "101"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void returnsForbiddenForForeignBookmark() throws Exception {
        when(service.getCollectionsWithRecentBookmarks(eq("owner"), any(Pageable.class), anyList()))
                .thenThrow(new ApiException(BookmarkException.ACCESS_DENIED));
        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", "101")
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsNotFoundForMissingBookmark() throws Exception {
        when(service.getCollectionsWithRecentBookmarks(eq("owner"), any(Pageable.class), anyList()))
                .thenThrow(new ApiException(BookmarkException.BOOKMARK_NOT_FOUND));
        mockMvc.perform(get("/api/v1/collections").param("bookmarkIds", "101")
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isNotFound());
    }
}
