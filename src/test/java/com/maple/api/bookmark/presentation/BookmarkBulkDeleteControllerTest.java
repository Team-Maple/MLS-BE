package com.maple.api.bookmark.presentation;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.BookmarkService;
import com.maple.api.bookmark.application.dto.BookmarkBulkDeleteRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkBulkDeleteResponseDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class BookmarkBulkDeleteControllerTest {

    private static final String PATH = "/api/v1/bookmarks";
    private static final String BODY = "{\"bookmarkIds\":[101,102]}";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private BookmarkService service;

    @Test
    void acceptsDeleteJsonBodyAndReturnsActualBookmarkCount() throws Exception {
        when(service.deleteBookmarks(eq("owner"), any())).thenReturn(new BookmarkBulkDeleteResponseDto(2));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(2));
        verify(service).deleteBookmarks("owner", new BookmarkBulkDeleteRequestDto(List.of(101, 102)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "null", "{", "{\"bookmarkIds\":null}", "{\"bookmarkIds\":[]}",
            "{\"bookmarkIds\":[101,101]}", "{\"bookmarkIds\":[0]}",
            "{\"bookmarkIds\":[-1]}", "{\"bookmarkIds\":[null]}",
            "{\"bookmarkIds\":[\"invalid\"]}"
    })
    void rejectsInvalidBodyBeforeCallingService(String body) throws Exception {
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingBodyEvenIfQueryParametersArePresent() throws Exception {
        mockMvc.perform(delete(PATH).param("bookmarkIds", "101,102")
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
    void returnsForbiddenForUnownedBookmarks() throws Exception {
        when(service.deleteBookmarks(eq("owner"), any()))
                .thenThrow(new ApiException(BookmarkException.ACCESS_DENIED));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsNotFoundForMissingBookmarks() throws Exception {
        when(service.deleteBookmarks(eq("owner"), any()))
                .thenThrow(new ApiException(BookmarkException.BOOKMARK_NOT_FOUND));
        mockMvc.perform(delete(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsExistingSingleDeleteRouteAndResponse() throws Exception {
        mockMvc.perform(delete(PATH + "/101").with(user(new PrincipalDetails("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
        verify(service).deleteBookmark("owner", 101);
        verify(service, never()).deleteBookmarks(anyString(), any());
    }
}
