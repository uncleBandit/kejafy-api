package com.pitahmzalendo.kejafy.chat.dto.response;

import com.pitahmzalendo.kejafy.chat.entity.Conversation;
import com.pitahmzalendo.kejafy.user.entity.User;

import java.time.LocalDateTime;

/**
 * Built relative to the viewer: otherParticipant* is whichever side of
 * the conversation ISN'T the caller, so an inbox list can render "chat
 * with X" without the client having to know whether it's the customer
 * or agent looking at it.
 */
public record ConversationResponse(
        Long id,
        Long otherParticipantId,
        String otherParticipantName,
        String otherParticipantAvatarUrl,
        Long originListingId,
        String originListingTitle,
        long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt
) {
    public static ConversationResponse from(Conversation conversation, User viewer, long unreadCount) {
        User other = conversation.theOtherParticipant(viewer);
        return new ConversationResponse(
                conversation.getId(),
                other.getId(),
                other.getFirstName() + " " + other.getLastName(),
                other.getAvatarUrl(),
                conversation.getOriginListing() != null ? conversation.getOriginListing().getId() : null,
                conversation.getOriginListing() != null ? conversation.getOriginListing().getTitle() : null,
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getLastMessageAt()
        );
    }
}
