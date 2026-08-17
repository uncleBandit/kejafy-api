package com.pitahmzalendo.kejafy.chat.controller;

import com.pitahmzalendo.kejafy.chat.dto.request.SendMessageRequest;
import com.pitahmzalendo.kejafy.chat.dto.request.StartConversationRequest;
import com.pitahmzalendo.kejafy.chat.dto.response.ConversationResponse;
import com.pitahmzalendo.kejafy.chat.dto.response.MessageResponse;
import com.pitahmzalendo.kejafy.chat.entity.Conversation;
import com.pitahmzalendo.kejafy.chat.service.ChatService;
import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Chat", description = "Subscriber-to-agent messaging")
@SecurityRequirement(name = "sessionCookie")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/v1/listings/{listingId}/chat")
    @Operation(
            summary = "Start (or reuse) a chat with a listing's agent",
            description = "Requires an active subscription. Reuses the existing thread with " +
                    "that agent if one already exists, rather than creating a new one per listing."
    )
    @ApiResponse(responseCode = "200", description = "The (possibly pre-existing) conversation")
    @ApiResponse(responseCode = "403", description = "No active subscription")
    public ResponseEntity<ConversationResponse> startConversation(
            @PathVariable Long listingId,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody(required = false) StartConversationRequest request
    ) {
        User viewer = principal.getDomainUser();
        Conversation conversation = chatService.startConversation(listingId, viewer, request);
        long unread = chatService.getUnreadCount(conversation, viewer);
        return ResponseEntity.ok(ConversationResponse.from(conversation, viewer, unread));
    }

    @GetMapping("/api/v1/conversations")
    @Operation(summary = "List the caller's conversations, most recently active first")
    public ResponseEntity<Page<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User viewer = principal.getDomainUser();
        Page<ConversationResponse> page = chatService.getMyConversations(viewer, pageable)
                .map(conversation -> ConversationResponse.from(
                        conversation, viewer, chatService.getUnreadCount(conversation, viewer)));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/api/v1/conversations/{id}/messages")
    @Operation(
            summary = "Read a conversation's messages, oldest first",
            description = "Also marks the other participant's messages as read, same as opening a chat app."
    )
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        User viewer = principal.getDomainUser();
        Page<MessageResponse> page = chatService.getMessages(id, viewer, pageable)
                .map(message -> MessageResponse.from(message, viewer));
        return ResponseEntity.ok(page);
    }

    @PostMapping("/api/v1/conversations/{id}/messages")
    @Operation(
            summary = "Send a message",
            description = "Agents can always reply. Customers need an active subscription, " +
                    "re-checked on every send — not just when the thread was opened."
    )
    @ApiResponse(responseCode = "201", description = "Message sent")
    @ApiResponse(responseCode = "403", description = "Not a participant, or subscription has lapsed")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody SendMessageRequest request
    ) {
        User sender = principal.getDomainUser();
        var message = chatService.sendMessage(id, sender, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message, sender));
    }
}
