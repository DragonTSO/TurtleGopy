package com.turtle.turtlegopy.api.storage;

import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.Feedback;

public interface StorageProvider {

    void init();

    void shutdown();

    void save(Feedback feedback);

    void update(Feedback feedback);

    void delete(UUID feedbackId);

    Feedback getById(UUID feedbackId);

    List<Feedback> getByPlayer(UUID playerUUID);

    List<Feedback> getAll();
}
