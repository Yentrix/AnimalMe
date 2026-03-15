package com.iax.animalme.service;

import com.iax.animalme.model.Message;
import com.iax.animalme.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    private MessageRepository messageRepository;

    public Message sendMessage(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> getConversation(Long user1Id, Long user2ID) {
        return messageRepository.findChatHistory(user1Id, user2ID);
    }
}
