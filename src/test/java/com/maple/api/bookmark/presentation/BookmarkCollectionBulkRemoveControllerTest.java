package com.maple.api.bookmark.presentation;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkAddResponseDto;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkRemoveRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkRemoveResponseDto;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.common.presentation.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class BookmarkCollectionBulkRemoveControllerTest {

    private static final String PATH = "/api/v1/bookmark-collections";
    private static final String BODY = """
            {"collectionIds":[1,2],"bookmarkIds":[101,102]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CollectionService service;

    @Test
    void acceptsDeleteJsonBodyAndReturnsActualMappingCount() throws Exception {
        when(service.removeBookmarksFromCollections(eq("owner"), any()))
                .thenReturn(new BookmarkCollectionBulkRemoveResponseDto(3));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.removedCount").value(3));
        verify(service).removeBookmarksFromCollections("owner",
                new BookmarkCollectionBulkRemoveRequestDto(List.of(1, 2), List.of(101, 102)));
    }

    @Test
    void returnsSuccessForNoMatchingMappings() throws Exception {
        when(service.removeBookmarksFromCollections(eq("owner"), any()))
                .thenReturn(new BookmarkCollectionBulkRemoveResponseDto(0));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.removedCount").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "null", "{",
            "{\"collectionIds\":[],\"bookmarkIds\":[101]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":[]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":null}",
            "{\"collectionIds\":[1,1],\"bookmarkIds\":[101]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":[101,101]}",
            "{\"collectionIds\":[0],\"bookmarkIds\":[101]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":[-1]}",
            "{\"collectionIds\":[null],\"bookmarkIds\":[101]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":[null]}",
            "{\"collectionIds\":[1],\"bookmarkIds\":[\"invalid\"]}"
    })
    void rejectsInvalidBodiesBeforeCallingService(String body) throws Exception {
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingBodyEvenIfQueryParametersArePresent() throws Exception {
        mockMvc.perform(delete(PATH).param("collectionIds", "1").param("bookmarkIds", "101")
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void returnsForbiddenForUnownedResources() throws Exception {
        when(service.removeBookmarksFromCollections(eq("owner"), any()))
                .thenThrow(new ApiException(BookmarkException.ACCESS_DENIED));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsNotFoundForMissingResources() throws Exception {
        when(service.removeBookmarksFromCollections(eq("owner"), any()))
                .thenThrow(new ApiException(BookmarkException.BOOKMARK_NOT_FOUND));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsExistingPostRouteAvailable() throws Exception {
        when(service.addBookmarksToCollections(eq("owner"), any()))
                .thenReturn(BookmarkCollectionBulkAddResponseDto.of(4));
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.addedCount").value(4));
        verify(service, never()).removeBookmarksFromCollections(anyString(), any());
    }
}
