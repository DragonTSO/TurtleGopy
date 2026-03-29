package com.turtle.turtlegopy.api.storage;

import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.SupportTicket;

public interface SupportTicketStorageProvider {

    void init();

    void shutdown();

    void save(SupportTicket ticket);

    void update(SupportTicket ticket);

    void delete(UUID ticketId);

    SupportTicket getById(UUID ticketId);

    List<SupportTicket> getByPlayer(UUID playerUUID);

    List<SupportTicket> getAll();
}
