package com.iax.animalme.repository;

import com.iax.animalme.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :u1 AND m.recipient.id = :u2) " +
            "OR (m.sender.id = :u2 AND m.recipient.id = :u1) ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(Long u1, Long u2);
}
