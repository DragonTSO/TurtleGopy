package com.turtle.turtlegopy.core;

import com.turtle.turtlegopy.api.storage.BugReportStorageProvider;
import com.turtle.turtlegopy.api.storage.StorageProvider;
import com.turtle.turtlegopy.command.GopyCommand;
import com.turtle.turtlegopy.gui.GUIListener;
import com.turtle.turtlegopy.listener.ChatInputListener;
import com.turtle.turtlegopy.manager.BroadcastManager;
import com.turtle.turtlegopy.manager.BugReportManager;
import com.turtle.turtlegopy.manager.FeedbackManager;
import com.turtle.turtlegopy.storage.DatabaseBugReportStorageProvider;
import com.turtle.turtlegopy.storage.DatabaseStorageProvider;
import com.turtle.turtlegopy.storage.YamlBugReportStorageProvider;
import com.turtle.turtlegopy.storage.YamlStorageProvider;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Getter
public class TurtleGopyCore {

    private final JavaPlugin plugin;
    private StorageProvider storageProvider;
    private BugReportStorageProvider bugReportStorageProvider;
    private FeedbackManager feedbackManager;
    private BugReportManager bugReportManager;
    private BroadcastManager broadcastManager;
    private ChatInputListener chatInputListener;
    private FileConfiguration messagesConfig;

    public TurtleGopyCore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        plugin.saveDefaultConfig();
        saveDefaultMessages();
        loadMessagesConfig();

        initStorage();

        feedbackManager = new FeedbackManager(this);
        bugReportManager = new BugReportManager(this);
        broadcastManager = new BroadcastManager(this);
        chatInputListener = new ChatInputListener(this);

        plugin.getServer().getPluginManager().registerEvents(chatInputListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new GUIListener(this), plugin);

        // Register /gopy and /baoloi commands (same executor, detects label)
        GopyCommand gopyCommand = new GopyCommand(this);

        PluginCommand gopyCmd = plugin.getCommand("gopy");
        if (gopyCmd != null) {
            gopyCmd.setExecutor(gopyCommand);
            gopyCmd.setTabCompleter(gopyCommand);
        }

        PluginCommand baoloiCmd = plugin.getCommand("baoloi");
        if (baoloiCmd != null) {
            baoloiCmd.setExecutor(gopyCommand);
            baoloiCmd.setTabCompleter(gopyCommand);
        }

        // Start broadcast manager
        broadcastManager.start();

        plugin.getLogger().info("TurtleGopy đã được kích hoạt!");
    }

    public void onDisable() {
        if (broadcastManager != null) {
            broadcastManager.stop();
        }
        if (chatInputListener != null) {
            chatInputListener.clearAll();
        }
        if (storageProvider != null) {
            storageProvider.shutdown();
        }
        if (bugReportStorageProvider != null) {
            bugReportStorageProvider.shutdown();
        }
        plugin.getLogger().info("TurtleGopy đã bị vô hiệu hóa!");
    }

    public void reload() {
        plugin.reloadConfig();
        loadMessagesConfig();

        if (storageProvider != null) {
            storageProvider.shutdown();
        }
        if (bugReportStorageProvider != null) {
            bugReportStorageProvider.shutdown();
        }
        initStorage();

        feedbackManager = new FeedbackManager(this);
        bugReportManager = new BugReportManager(this);
        broadcastManager.reload();
    }

    private void initStorage() {
        String storageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();

        if ("DATABASE".equals(storageType)) {
            DatabaseStorageProvider dbProvider = new DatabaseStorageProvider(this);
            storageProvider = dbProvider;
            storageProvider.init();

            bugReportStorageProvider = new DatabaseBugReportStorageProvider(this, dbProvider.getDataSource());
            bugReportStorageProvider.init();
        } else {
            storageProvider = new YamlStorageProvider(this);
            storageProvider.init();

            bugReportStorageProvider = new YamlBugReportStorageProvider(this);
            bugReportStorageProvider.init();
        }

        plugin.getLogger().info("Đã khởi tạo storage: " + storageType);
    }

    private void saveDefaultMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "§8[§6TurtleGopy§8] ");
        String message = messagesConfig.getString(key, "§cMissing message: " + key);
        return colorize(prefix + message);
    }

    public String getMessageNoPrefix(String key) {
        String message = messagesConfig.getString(key, "§cMissing message: " + key);
        return colorize(message);
    }

    public String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
