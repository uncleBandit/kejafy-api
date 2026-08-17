package com.pitahmzalendo.kejafy.chat.dto.response;

import com.pitahmzalendo.kejafy.chat.entity.Message;
import com.pitahmzalendo.kejafy.user.entity.User;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        boolean mine,
        String body,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static MessageResponse from(Message message, User viewer) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getFirstName() + " " + message.getSender().getLastName(),
                message.isSentBy(viewer),
                message.getBody(),
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
