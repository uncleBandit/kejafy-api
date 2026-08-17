package com.pitahmzalendo.kejafy.chat.repository;

import com.pitahmzalendo.kejafy.chat.entity.Conversation;
import com.pitahmzalendo.kejafy.chat.entity.Message;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation, Pageable pageable);

    // Unread = sent by the other participant and not yet read. Used to
    // mark a thread read when its recipient opens it (see ChatService).
    List<Message> findByConversationAndSenderNotAndReadAtIsNull(Conversation conversation, User notSender);

    long countByConversationAndSenderNotAndReadAtIsNull(Conversation conversation, User notSender);
}