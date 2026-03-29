package com.turtle.turtlegopy.api.storage;

import java.util.List;
import java.util.UUID;

import com.turtle.turtlegopy.api.model.BugReport;

public interface BugReportStorageProvider {

    void init();

    void shutdown();

    void save(BugReport report);

    void update(BugReport report);

    void delete(UUID reportId);

    BugReport getById(UUID reportId);

    List<BugReport> getByPlayer(UUID playerUUID);

    List<BugReport> getAll();
}
