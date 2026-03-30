package com.turtle.turtlegopy.api.storage;

import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportChatMessage;

public interface SupportChatStorageProvider {

    void init();

    void shutdown();

    void saveMessage(SupportChatMessage message);

    List<SupportChatMessage> getMessages(UUID ticketId);

    void deleteByTicket(UUID ticketId);
}
