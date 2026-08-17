package com.pitahmzalendo.kejafy.chat.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Posted to /api/v1/listings/{listingId}/chat to open (or reuse) the
 * thread with that listing's agent. openingMessage is optional — the
 * client can just create/open the thread first and let the person type,
 * rather than forcing a message on creation.
 */
public record StartConversationRequest(
        @Size(max = 2000, message = "openingMessage is too long")
        String openingMessage
) {
}
