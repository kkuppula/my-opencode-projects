package com.blackboard.ai.service;

import com.blackboard.ai.dto.ConversationDTO;
import com.blackboard.ai.entity.PlaygroundConversation;
import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.entity.enums.ContextType;
import com.blackboard.ai.exception.AiServiceException;
import com.blackboard.ai.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService.
 * 
 * <p><b>Test Strategy:</b> Using Mockito to mock repository layer.
 * Tests focus on business logic validation and proper data transformation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
class ConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @InjectMocks
    private ConversationService conversationService;
    
    private static final Long USER_ID = 12345L;
    private static final String CONVERSATION_ID = UUID.randomUUID().toString();
    
    private PlaygroundConversation sampleEntity;
    
    @BeforeEach
    void setUp() {
        sampleEntity = new PlaygroundConversation(
                USER_ID,
                "Test Conversation",
                ContextType.GENERAL,
                AiModel.AMAZON_NOVA_MICRO
        );
        sampleEntity.setId(1L);
        sampleEntity.setConversationUid(CONVERSATION_ID);
        sampleEntity.setCreatedAt(Instant.now());
        sampleEntity.setLastModifiedAt(Instant.now());
    }
    
    @Nested
    @DisplayName("createConversation")
    class CreateConversation {
        
        @Test
        @DisplayName("Should create and return new conversation")
        void shouldCreateConversation() {
            // Given
            var request = new ConversationDTO.CreateRequest(
                    "New Conversation",
                    ContextType.COURSE,
                    AiModel.AMAZON_NOVA_LITE
            );
            
            when(conversationRepository.save(any(PlaygroundConversation.class)))
                    .thenAnswer(invocation -> {
                        PlaygroundConversation saved = invocation.getArgument(0);
                        saved.setId(1L);
                        saved.setConversationUid(UUID.randomUUID().toString());
                        saved.setCreatedAt(Instant.now());
                        saved.setLastModifiedAt(Instant.now());
                        return saved;
                    });
            
            // When
            ConversationDTO.Response result = conversationService.createConversation(USER_ID, request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("New Conversation");
            assertThat(result.contextType()).isEqualTo(ContextType.COURSE);
            assertThat(result.modelName()).isEqualTo(AiModel.AMAZON_NOVA_LITE);
            assertThat(result.id()).isNotNull();
            
            verify(conversationRepository).save(any(PlaygroundConversation.class));
        }
        
        @Test
        @DisplayName("Should set user ID from argument")
        void shouldSetUserIdFromArgument() {
            // Given
            var request = new ConversationDTO.CreateRequest(
                    "Test",
                    ContextType.GENERAL,
                    AiModel.AMAZON_NOVA_MICRO
            );
            
            when(conversationRepository.save(any(PlaygroundConversation.class)))
                    .thenAnswer(invocation -> {
                        PlaygroundConversation saved = invocation.getArgument(0);
                        assertThat(saved.getUsersPk1()).isEqualTo(USER_ID);
                        saved.setId(1L);
                        saved.setConversationUid(UUID.randomUUID().toString());
                        saved.setCreatedAt(Instant.now());
                        saved.setLastModifiedAt(Instant.now());
                        return saved;
                    });
            
            // When
            conversationService.createConversation(USER_ID, request);
            
            // Then
            verify(conversationRepository).save(argThat(entity -> 
                    entity.getUsersPk1().equals(USER_ID)));
        }
    }
    
    @Nested
    @DisplayName("getConversation")
    class GetConversation {
        
        @Test
        @DisplayName("Should return conversation when found")
        void shouldReturnConversationWhenFound() {
            // Given
            when(conversationRepository.findByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleEntity));
            
            // When
            ConversationDTO.Response result = conversationService.getConversation(USER_ID, CONVERSATION_ID);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CONVERSATION_ID);
            assertThat(result.title()).isEqualTo("Test Conversation");
        }
        
        @Test
        @DisplayName("Should throw exception when not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(conversationRepository.findByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.empty());
            
            // When/Then
            assertThatThrownBy(() -> conversationService.getConversation(USER_ID, CONVERSATION_ID))
                    .isInstanceOf(AiServiceException.class)
                    .satisfies(ex -> {
                        AiServiceException aiEx = (AiServiceException) ex;
                        assertThat(aiEx.getErrorCode()).isEqualTo("CONVERSATION_NOT_FOUND");
                        assertThat(aiEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
        
        @Test
        @DisplayName("Should not return other user's conversation")
        void shouldNotReturnOtherUsersConversation() {
            // Given
            Long otherUserId = 99999L;
            when(conversationRepository.findByConversationUidAndUsersPk1(CONVERSATION_ID, otherUserId))
                    .thenReturn(Optional.empty());
            
            // When/Then
            assertThatThrownBy(() -> conversationService.getConversation(otherUserId, CONVERSATION_ID))
                    .isInstanceOf(AiServiceException.class);
        }
    }
    
    @Nested
    @DisplayName("listConversations")
    class ListConversations {
        
        @Test
        @DisplayName("Should return paginated conversations")
        void shouldReturnPaginatedConversations() {
            // Given
            Page<PlaygroundConversation> page = new PageImpl<>(
                    List.of(sampleEntity),
                    PageRequest.of(0, 20),
                    1
            );
            
            when(conversationRepository.findByUsersPk1(eq(USER_ID), any()))
                    .thenReturn(page);
            
            // When
            ConversationDTO.ListResponse result = conversationService.listConversations(
                    USER_ID, PageRequest.of(0, 20));
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(20);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.first()).isTrue();
            assertThat(result.last()).isTrue();
        }
        
        @Test
        @DisplayName("Should return empty list when no conversations")
        void shouldReturnEmptyListWhenNoConversations() {
            // Given
            Page<PlaygroundConversation> emptyPage = Page.empty(PageRequest.of(0, 20));
            
            when(conversationRepository.findByUsersPk1(eq(USER_ID), any()))
                    .thenReturn(emptyPage);
            
            // When
            ConversationDTO.ListResponse result = conversationService.listConversations(
                    USER_ID, PageRequest.of(0, 20));
            
            // Then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("updateConversation")
    class UpdateConversation {
        
        @Test
        @DisplayName("Should update title when provided")
        void shouldUpdateTitleWhenProvided() {
            // Given
            var updateRequest = new ConversationDTO.UpdateRequest(
                    "Updated Title",
                    null,
                    null
            );
            
            when(conversationRepository.findByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleEntity));
            when(conversationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            
            // When
            ConversationDTO.Response result = conversationService.updateConversation(
                    USER_ID, CONVERSATION_ID, updateRequest);
            
            // Then
            assertThat(result.title()).isEqualTo("Updated Title");
            assertThat(result.contextType()).isEqualTo(ContextType.GENERAL); // Unchanged
            assertThat(result.modelName()).isEqualTo(AiModel.AMAZON_NOVA_MICRO); // Unchanged
        }
        
        @Test
        @DisplayName("Should update multiple fields")
        void shouldUpdateMultipleFields() {
            // Given
            var updateRequest = new ConversationDTO.UpdateRequest(
                    "New Title",
                    ContextType.ASSIGNMENT,
                    AiModel.OPEN_AI_GPT_OSS_20B
            );
            
            when(conversationRepository.findByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(sampleEntity));
            when(conversationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            
            // When
            ConversationDTO.Response result = conversationService.updateConversation(
                    USER_ID, CONVERSATION_ID, updateRequest);
            
            // Then
            assertThat(result.title()).isEqualTo("New Title");
            assertThat(result.contextType()).isEqualTo(ContextType.ASSIGNMENT);
            assertThat(result.modelName()).isEqualTo(AiModel.OPEN_AI_GPT_OSS_20B);
        }
    }
    
    @Nested
    @DisplayName("deleteConversation")
    class DeleteConversation {
        
        @Test
        @DisplayName("Should delete conversation when exists")
        void shouldDeleteWhenExists() {
            // Given
            when(conversationRepository.existsByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(true);
            when(conversationRepository.deleteByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(1);
            
            // When
            conversationService.deleteConversation(USER_ID, CONVERSATION_ID);
            
            // Then
            verify(conversationRepository).deleteByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID);
        }
        
        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(conversationRepository.existsByConversationUidAndUsersPk1(CONVERSATION_ID, USER_ID))
                    .thenReturn(false);
            
            // When/Then
            assertThatThrownBy(() -> conversationService.deleteConversation(USER_ID, CONVERSATION_ID))
                    .isInstanceOf(AiServiceException.class)
                    .satisfies(ex -> {
                        AiServiceException aiEx = (AiServiceException) ex;
                        assertThat(aiEx.getErrorCode()).isEqualTo("CONVERSATION_NOT_FOUND");
                        assertThat(aiEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
            
            verify(conversationRepository, never()).deleteByConversationUidAndUsersPk1(any(), any());
        }
    }
}
