package com.pitahmzalendo.kejafy.chat.service;


import com.pitahmzalendo.kejafy.chat.entity.Conversation;
import com.pitahmzalendo.kejafy.chat.entity.Message;
import com.pitahmzalendo.kejafy.chat.exception.ChatAccessException;
import com.pitahmzalendo.kejafy.chat.exception.ChatException;
import com.pitahmzalendo.kejafy.chat.exception.ChatNotFoundException;
import com.pitahmzalendo.kejafy.chat.repository.ConversationRepository;
import com.pitahmzalendo.kejafy.chat.repository.MessageRepository;
import com.pitahmzalendo.kejafy.chat.dto.request.SendMessageRequest;
import com.pitahmzalendo.kejafy.chat.dto.request.StartConversationRequest;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.exception.ListingNotFoundException;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.subscription.service.SubscriptionService;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat is the other half of the subscriber value prop alongside exact
 * location (see ListingFeedItemResponse): a CUSTOMER needs an active
 * subscription to START a thread with an agent, AND to keep sending
 * once one exists — re-checked on every send, not just at creation, so
 * a lapsed subscription actually stops new outbound messages rather
 * than only blocking the first one. The AGENT side is never gated: once
 * a customer has legitimately opened a thread, the agent must always be
 * able to reply.
 */
@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final SubscriptionService subscriptionService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       ListingRepository listingRepository,
                       SubscriptionService subscriptionService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Opens (or reuses) the thread between the caller and a listing's
     * agent. Reused across every listing that agent and customer ever
     * discuss — see Conversation's javadoc — so calling this twice for
     * two different listings from the same agent returns the same
     * conversation both times.
     */
    public Conversation startConversation(Long listingId, User customer, StartConversationRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new ChatException("You can only chat about published listings.");
        }

        User agent = listing.getAgent();
        if (agent.getId().equals(customer.getId())) {
            throw new ChatException("You can't start a conversation with yourself.");
        }

        requireCanInitiate(customer);

        Conversation conversation = conversationRepository.findByCustomerAndAgent(customer, agent)
                .orElseGet(() -> {
                    Conversation created = conversationRepository.save(
                            new Conversation(customer, agent, listing));
                    log.info("Conversation started, conversationId={}, customerId={}, agentId={}, listingId={}",
                            created.getId(), customer.getId(), agent.getId(), listingId);
                    return created;
                });

        if (request != null && request.openingMessage() != null && !request.openingMessage().isBlank()) {
            appendMessage(conversation, customer, request.openingMessage());
        }

        return conversation;
    }

    @Transactional(readOnly = true)
    public Page<Conversation> getMyConversations(User user, Pageable pageable) {
        return conversationRepository.findByParticipant(user, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Conversation conversation, User viewer) {
        return messageRepository.countByConversationAndSenderNotAndReadAtIsNull(conversation, viewer);
    }

    /** Opening a thread marks the other side's messages as read — same as opening a chat app. */
    public Page<Message> getMessages(Long conversationId, User requester, Pageable pageable) {
        Conversation conversation = requireParticipant(conversationId, requester);

        messageRepository.findByConversationAndSenderNotAndReadAtIsNull(conversation, requester)
                .forEach(Message::markRead);

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation, pageable);
    }

    public Message sendMessage(Long conversationId, User sender, SendMessageRequest request) {
        Conversation conversation = requireParticipant(conversationId, sender);

        boolean senderIsCustomer = conversation.getCustomer().getId().equals(sender.getId());
        if (senderIsCustomer) {
            requireCanInitiate(sender);
        }

        return appendMessage(conversation, sender, request.body());
    }

    private Message appendMessage(Conversation conversation, User sender, String body) {
        Message message = messageRepository.save(new Message(conversation, sender, body));
        conversation.touch();
        conversationRepository.save(conversation);
        return message;
    }

    /** ADMIN bypasses the subscription check (support/moderation access); every other customer needs one. */
    private void requireCanInitiate(User customer) {
        if (customer.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!subscriptionService.hasActiveSubscription(customer)) {
            throw new ChatAccessException(
                    "An active subscription is required to chat with agents.");
        }
    }

    private Conversation requireParticipant(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found."));

        if (!conversation.isParticipant(user) && user.getRole() != UserRole.ADMIN) {
            throw new ChatAccessException("You are not part of this conversation.");
        }
        return conversation;
    }
}
