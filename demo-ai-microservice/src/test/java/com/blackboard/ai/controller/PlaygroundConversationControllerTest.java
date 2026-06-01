package com.blackboard.ai.controller;

import com.blackboard.ai.dto.ConversationDTO;
import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.entity.enums.ContextType;
import com.blackboard.ai.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PlaygroundConversationController.
 * 
 * <p><b>Test Strategy:</b> Using @WebMvcTest for fast, focused controller tests.
 * Service layer is mocked to isolate controller behavior.
 * 
 * <p><b>Authentication:</b> Tests use Spring Security's JWT test support
 * to simulate authenticated requests.
 */
@WebMvcTest(PlaygroundConversationController.class)
@DisplayName("PlaygroundConversationController Tests")
class PlaygroundConversationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ConversationService conversationService;
    
    private static final String BASE_URL = "/v1/ai-playground/conversations";
    private static final String CONVERSATION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long USER_ID = 12345L;
    
    private ConversationDTO.Response sampleResponse;
    private ConversationDTO.CreateRequest sampleCreateRequest;
    
    @BeforeEach
    void setUp() {
        // Sample response for mocking
        sampleResponse = new ConversationDTO.Response(
                CONVERSATION_ID,
                "Test Conversation",
                ContextType.GENERAL,
                AiModel.AMAZON_NOVA_MICRO,
                Instant.now(),
                Instant.now()
        );
        
        // Sample create request
        sampleCreateRequest = new ConversationDTO.CreateRequest(
                "New Conversation",
                ContextType.COURSE,
                AiModel.AMAZON_NOVA_LITE
        );
    }
    
    @Nested
    @DisplayName("POST /conversations - Create Conversation")
    class CreateConversation {
        
        @Test
        @DisplayName("Should create conversation successfully with valid request")
        void shouldCreateConversation() throws Exception {
            // Given
            when(conversationService.createConversation(any(), any()))
                    .thenReturn(sampleResponse);
            
            // When/Then
            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(CONVERSATION_ID))
                    .andExpect(jsonPath("$.title").value("Test Conversation"))
                    .andExpect(jsonPath("$.contextType").value("GENERAL"))
                    .andExpect(jsonPath("$.modelName").value("AMAZON_NOVA_MICRO"));
            
            verify(conversationService).createConversation(eq(USER_ID), any());
        }
        
        @Test
        @DisplayName("Should return 400 when title is missing")
        void shouldRejectMissingTitle() throws Exception {
            // Given
            var invalidRequest = new ConversationDTO.CreateRequest(
                    "",  // Empty title
                    ContextType.GENERAL,
                    AiModel.AMAZON_NOVA_MICRO
            );
            
            // When/Then
            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(builder -> builder.subject(USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
            
            verify(conversationService, never()).createConversation(any(), any());
        }
        
        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldRequireAuthentication() throws Exception {
            // When/Then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
    
    @Nested
    @DisplayName("GET /conversations - List Conversations")
    class ListConversations {
        
        @Test
        @DisplayName("Should return paginated conversations")
        void shouldReturnPaginatedList() throws Exception {
            // Given
            var listResponse = new ConversationDTO.ListResponse(
                    List.of(sampleResponse),
                    0, 20, 1, 1, true, true
            );
            
            when(conversationService.listConversations(eq(USER_ID), any()))
                    .thenReturn(listResponse);
            
            // When/Then
            mockMvc.perform(get(BASE_URL)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID)))
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
        
        @Test
        @DisplayName("Should enforce max page size")
        void shouldEnforceMaxPageSize() throws Exception {
            // Given
            var listResponse = new ConversationDTO.ListResponse(
                    List.of(), 0, 100, 0, 0, true, true
            );
            
            when(conversationService.listConversations(eq(USER_ID), any()))
                    .thenReturn(listResponse);
            
            // When/Then
            mockMvc.perform(get(BASE_URL)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID)))
                            .param("size", "500"))  // Exceeds max
                    .andExpect(status().isOk());
            
            // Verify size was capped to 100
            verify(conversationService).listConversations(eq(USER_ID), 
                    argThat(pageable -> pageable.getPageSize() <= 100));
        }
    }
    
    @Nested
    @DisplayName("GET /conversations/{id} - Get Conversation")
    class GetConversation {
        
        @Test
        @DisplayName("Should return conversation by ID")
        void shouldReturnConversationById() throws Exception {
            // Given
            when(conversationService.getConversation(USER_ID, CONVERSATION_ID))
                    .thenReturn(sampleResponse);
            
            // When/Then
            mockMvc.perform(get(BASE_URL + "/" + CONVERSATION_ID)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CONVERSATION_ID))
                    .andExpect(jsonPath("$.title").value("Test Conversation"));
        }
    }
    
    @Nested
    @DisplayName("PATCH /conversations/{id} - Update Conversation")
    class UpdateConversation {
        
        @Test
        @DisplayName("Should update conversation title")
        void shouldUpdateConversationTitle() throws Exception {
            // Given
            var updateRequest = new ConversationDTO.UpdateRequest(
                    "Updated Title",
                    null,  // Don't change context type
                    null   // Don't change model
            );
            
            var updatedResponse = new ConversationDTO.Response(
                    CONVERSATION_ID,
                    "Updated Title",
                    ContextType.GENERAL,
                    AiModel.AMAZON_NOVA_MICRO,
                    Instant.now(),
                    Instant.now()
            );
            
            when(conversationService.updateConversation(eq(USER_ID), eq(CONVERSATION_ID), any()))
                    .thenReturn(updatedResponse);
            
            // When/Then
            mockMvc.perform(patch(BASE_URL + "/" + CONVERSATION_ID)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }
    }
    
    @Nested
    @DisplayName("DELETE /conversations/{id} - Delete Conversation")
    class DeleteConversation {
        
        @Test
        @DisplayName("Should delete conversation successfully")
        void shouldDeleteConversation() throws Exception {
            // Given
            doNothing().when(conversationService).deleteConversation(USER_ID, CONVERSATION_ID);
            
            // When/Then
            mockMvc.perform(delete(BASE_URL + "/" + CONVERSATION_ID)
                            .with(jwt().jwt(builder -> builder
                                    .subject(USER_ID.toString())
                                    .claim("learn_user_id", USER_ID))))
                    .andExpect(status().isNoContent());
            
            verify(conversationService).deleteConversation(USER_ID, CONVERSATION_ID);
        }
    }
}
