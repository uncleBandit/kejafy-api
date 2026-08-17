package com.pitahmzalendo.kejafy.chat.repository;

import com.pitahmzalendo.kejafy.chat.entity.Conversation;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByCustomerAndAgent(User customer, User agent);

    // A user's inbox: every thread where they're either side, newest activity first.
    @Query("select c from Conversation c " +
            "where c.customer = :user or c.agent = :user " +
            "order by c.lastMessageAt desc")
    Page<Conversation> findByParticipant(@Param("user") User user, Pageable pageable);
}
