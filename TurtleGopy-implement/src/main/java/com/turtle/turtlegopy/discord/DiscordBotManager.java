package com.turtle.turtlegopy.discord;

import com.turtle.turtlegopy.api.model.*;
import com.turtle.turtlegopy.core.SchedulerUtil;
import com.turtle.turtlegopy.core.TurtleGopyCore;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the JDA Discord bot directly within the Minecraft plugin.
 * Improved with JDA best practices:
 * - createLight for minimal cache usage
 * - Non-blocking startup
 * - deferEdit for button interactions
 * - Persistent channelMap across restarts
 * - Reverse lookup map for O(1) message routing
 * - Bot activity status
 * - Proper error logging
 */
public class DiscordBotManager extends ListenerAdapter {

    private final TurtleGopyCore core;
    private JDA jda;

    // Maps ticketId -> channelId for tracking (persisted to file)
    private final Map<String, String> channelMap = new ConcurrentHashMap<>();
    // Reverse map: channelId -> ticketId for O(1) message lookup
    private final Map<String, String> reverseChannelMap = new ConcurrentHashMap<>();

    private static final String CHANNEL_MAP_FILE = "discord-channels.yml";

    // Status definitions
    private static final Map<String, StatusDef> FEEDBACK_STATUSES = Map.of(
            "PENDING", new StatusDef("⏳ Chờ xử lý", "⏳", 0xFFAA00),
            "READ", new StatusDef("👁 Đã đọc", "👁", 0x55FFFF),
            "ACCEPTED", new StatusDef("✔ Đã chấp nhận", "✅", 0x55FF55),
            "DEPLOYING", new StatusDef("🚀 Đang triển khai", "🚀", 0xFF6600),
            "REJECTED", new StatusDef("✘ Từ chối", "❌", 0xFF5555),
            "IMPLEMENTED", new StatusDef("⚡ Đã triển khai", "⚡", 0xAA00FF)
    );

    private static final Map<String, StatusDef> BUGREPORT_STATUSES = Map.of(
            "PENDING", new StatusDef("⏳ Chờ xử lý", "⏳", 0xFFAA00),
            "READ", new StatusDef("👁 Đã đọc", "👁", 0x55FFFF),
            "CHECKING", new StatusDef("🔍 Đang kiểm tra", "🔍", 0xFFFF55),
            "FIXING", new StatusDef("🔧 Đang sửa", "🔧", 0xFF6600),
            "FIXED", new StatusDef("✔ Đã sửa", "✅", 0x55FF55),
            "REJECTED", new StatusDef("✘ Không phải lỗi", "❌", 0xFF5555)
    );

    private static final Map<String, StatusDef> SUPPORT_STATUSES = Map.of(
            "PENDING", new StatusDef("⏳ Chờ xử lý", "⏳", 0xFFAA00),
            "READ", new StatusDef("👁 Đã đọc", "👁", 0x55FFFF),
            "PROCESSING", new StatusDef("🔄 Đang xử lý", "🔄", 0xFF6600),
            "RESOLVED", new StatusDef("✔ Đã giải quyết", "✅", 0x55FF55),
            "REJECTED", new StatusDef("✘ Từ chối", "❌", 0xFF5555)
    );

    public DiscordBotManager(TurtleGopyCore core) {
        this.core = core;
    }

    // ============================================
    // Lifecycle
    // ============================================

    public boolean isEnabled() {
        return core.getPlugin().getConfig().getBoolean("discord.enabled", false);
    }

    public void start() {
        if (!isEnabled()) return;

        String token = core.getPlugin().getConfig().getString("discord.token", "");
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN")) {
            core.getPlugin().getLogger().warning("[Discord] Thiếu token! Hãy cấu hình discord.token trong config.yml");
            return;
        }

        // Load persisted channel mappings
        loadChannelMap();

        try {
            // createLight: minimal cache, only what we need
            // No member cache, no voice state cache — just messages and guilds
            jda = JDABuilder.createLight(token, EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    ))
                    .disableCache(EnumSet.allOf(CacheFlag.class))
                    .addEventListeners(this)
                    .build();

            // Non-blocking: bot ready is handled in onReady event
            core.getPlugin().getLogger().info("[Discord] Đang kết nối bot...");

        } catch (Exception e) {
            core.getPlugin().getLogger().log(Level.SEVERE, "[Discord] Lỗi khởi tạo bot", e);
            jda = null;
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        core.getPlugin().getLogger().info("[Discord] Bot đã đăng nhập: " + jda.getSelfUser().getName());
        core.getPlugin().getLogger().info("[Discord] Đang trong " + jda.getGuilds().size() + " server(s)");
        core.getPlugin().getLogger().info("[Discord] Đã load " + channelMap.size() + " channel mapping(s) từ file");

        // Register slash commands
        String guildId = core.getPlugin().getConfig().getString("discord.guild-id", "");
        if (guildId != null && !guildId.isEmpty()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(
                        Commands.slash("delete", "Xóa channel góp ý / báo lỗi / hỗ trợ hiện tại")
                ).queue(
                        s -> core.getPlugin().getLogger().info("[Discord] Đã đăng ký slash commands"),
                        e -> core.getPlugin().getLogger().warning("[Discord] Lỗi đăng ký slash commands: " + e.getMessage())
                );
            }
        }

        // Update bot activity status
        updateBotActivity();
    }

    public void shutdown() {
        if (jda != null) {
            // Save channel map before shutting down
            saveChannelMap();
            jda.shutdown();
            core.getPlugin().getLogger().info("[Discord] Bot đã dừng.");
            jda = null;
        }
    }

    // ============================================
    // Bot Activity
    // ============================================

    public void updateBotActivity() {
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) return;

        String statusTemplate = core.getPlugin().getConfig().getString("discord.bot-status", "Đang hỗ trợ {tickets} phiếu");
        int ticketCount = channelMap.size();
        String statusText = statusTemplate.replace("{tickets}", String.valueOf(ticketCount));
        jda.getPresence().setActivity(Activity.watching(statusText));
    }

    // ============================================
    // Channel Map persistence
    // ============================================

    private File getChannelMapFile() {
        return new File(core.getPlugin().getDataFolder(), CHANNEL_MAP_FILE);
    }

    private void loadChannelMap() {
        File file = getChannelMapFile();
        if (!file.exists()) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            channelMap.clear();
            reverseChannelMap.clear();

            for (String key : config.getKeys(false)) {
                String channelId = config.getString(key);
                if (channelId != null && !channelId.isEmpty()) {
                    // Key is stored with underscores because YAML doesn't like hyphens in keys
                    String ticketId = key.replace('_', '-');
                    channelMap.put(ticketId, channelId);
                    reverseChannelMap.put(channelId, ticketId);
                }
            }
            core.getPlugin().getLogger().info("[Discord] Đã load " + channelMap.size() + " channel mapping(s)");
        } catch (Exception e) {
            core.getPlugin().getLogger().log(Level.WARNING, "[Discord] Lỗi load channel map", e);
        }
    }

    private void saveChannelMap() {
        SchedulerUtil.runAsync(core.getPlugin(), () -> {
            try {
                File file = getChannelMapFile();
                YamlConfiguration config = new YamlConfiguration();
                for (Map.Entry<String, String> entry : channelMap.entrySet()) {
                    // Replace hyphens with underscores for YAML key compatibility
                    String key = entry.getKey().replace('-', '_');
                    config.set(key, entry.getValue());
                }
                config.save(file);
            } catch (IOException e) {
                core.getPlugin().getLogger().log(Level.WARNING, "[Discord] Lỗi lưu channel map", e);
            }
        });
    }

    private void putChannelMapping(String ticketId, String channelId) {
        channelMap.put(ticketId, channelId);
        reverseChannelMap.put(channelId, ticketId);
        saveChannelMap();
    }

    private void removeChannelMapping(String ticketId) {
        String channelId = channelMap.remove(ticketId);
        if (channelId != null) {
            reverseChannelMap.remove(channelId);
        }
        saveChannelMap();
    }

    // ============================================
    // Event log channel
    // ============================================

    private void logToDiscord(String message) {
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) return;

        String logChannelId = core.getPlugin().getConfig().getString("discord.log-channel-id", "");
        if (logChannelId == null || logChannelId.isEmpty()) return;

        TextChannel logChannel = jda.getTextChannelById(logChannelId);
        if (logChannel == null) return;

        logChannel.sendMessage("📋 " + message).queue(
                null,
                e -> core.getPlugin().getLogger().warning("[Discord] Lỗi gửi log: " + e.getMessage())
        );
    }

    // ============================================
    // Channel creation
    // ============================================

    public void sendFeedbackCreated(String id, String playerName, String playerUUID, String content, String status, long createdAt) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.feedback.enabled", true)) return;

        SchedulerUtil.runAsync(core.getPlugin(), () -> {
            String categoryId = core.getPlugin().getConfig().getString("discord.feedback-category-id", "");
            createChannel("feedback", id, playerName, content, status, createdAt, categoryId, "gopy");
        });
    }

    public void sendBugReportCreated(String id, String playerName, String playerUUID, String content, String status, long createdAt) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.bugreport.enabled", true)) return;

        SchedulerUtil.runAsync(core.getPlugin(), () -> {
            String categoryId = core.getPlugin().getConfig().getString("discord.bugreport-category-id", "");
            createChannel("bugreport", id, playerName, content, status, createdAt, categoryId, "baoloi");
        });
    }

    public void sendSupportCreated(String id, String playerName, String playerUUID, String content, String status, long createdAt) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.support.enabled", true)) return;

        SchedulerUtil.runAsync(core.getPlugin(), () -> {
            String categoryId = core.getPlugin().getConfig().getString("discord.support-category-id", "");
            createChannel("support", id, playerName, content, status, createdAt, categoryId, "hotro");
        });
    }

    private void createChannel(String type, String id, String playerName, String content, String status,
                                long createdAt, String categoryId, String prefix) {
        if (jda.getStatus() != JDA.Status.CONNECTED) {
            core.getPlugin().getLogger().warning("[Discord] Bot chưa sẵn sàng, bỏ qua tạo channel cho " + type + "/" + id.substring(0, 8));
            return;
        }

        try {
            String guildId = core.getPlugin().getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                core.getPlugin().getLogger().warning("[Discord] Guild không tồn tại: " + guildId);
                return;
            }

            if (categoryId == null || categoryId.isEmpty()) {
                core.getPlugin().getLogger().warning("[Discord] Thiếu category ID cho " + type);
                return;
            }

            Category category = guild.getCategoryById(categoryId);
            if (category == null) {
                core.getPlugin().getLogger().warning("[Discord] Category không tồn tại: " + categoryId);
                return;
            }

            String shortId = id.substring(0, Math.min(8, id.length()));
            String channelName = buildChannelName(prefix, shortId, null);

            Map<String, StatusDef> statuses = getStatusMap(type);
            StatusDef statusDef = statuses.getOrDefault(status, statuses.get("PENDING"));

            // Build embed
            String title;
            switch (type) {
                case "feedback": title = "✉ Góp Ý — #" + shortId; break;
                case "bugreport": title = "🐛 Báo Lỗi — #" + shortId; break;
                case "support": title = "🎫 Hỗ Trợ — #" + shortId; break;
                default: title = "Ticket #" + shortId;
            }

            String formattedTime = formatTimestamp(createdAt);
            String footer = "support".equals(type)
                    ? "ID: " + id + " | Nhắn tin trong channel này để chat với người chơi"
                    : "ID: " + id;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setColor(new Color(statusDef.color))
                    .addField("👤 Người chơi", playerName, true)
                    .addField("📊 Trạng thái", statusDef.emoji + " " + statusDef.label, true)
                    .addField("📅 Thời gian", formattedTime, true)
                    .addField("📝 Nội dung", content != null ? content : "*Không có nội dung*", false)
                    .setFooter(footer)
                    .setTimestamp(Instant.now());

            // Build buttons
            List<ActionRow> buttons = buildButtons(type, id, status);

            // Topic
            String topicPrefix;
            switch (type) {
                case "feedback": topicPrefix = "Góp ý từ "; break;
                case "bugreport": topicPrefix = "Báo lỗi từ "; break;
                case "support": topicPrefix = "Hỗ trợ từ "; break;
                default: topicPrefix = "Ticket từ ";
            }

            category.createTextChannel(channelName)
                    .setTopic(topicPrefix + playerName + " | ID: " + id)
                    .queue(channel -> {
                        channel.sendMessageEmbeds(embed.build())
                                .setComponents(buttons)
                                .queue(msg -> {
                                    putChannelMapping(id, channel.getId());
                                    updateBotActivity();
                                    core.getPlugin().getLogger().info("[Discord] Đã tạo channel #" + channelName);
                                    logToDiscord("📌 Phiếu mới: **" + title + "** từ **" + playerName + "**");
                                }, e -> core.getPlugin().getLogger().warning("[Discord] Lỗi gửi embed: " + e.getMessage()));
                    }, error -> {
                        core.getPlugin().getLogger().warning("[Discord] Lỗi tạo channel: " + error.getMessage());
                    });
        } catch (Exception e) {
            core.getPlugin().getLogger().log(Level.WARNING, "[Discord] Lỗi tạo channel", e);
        }
    }

    // ============================================
    // Status updates
    // ============================================

    public void sendFeedbackStatusUpdate(String id, String newStatus) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.feedback.enabled", true)) return;
        SchedulerUtil.runAsync(core.getPlugin(), () -> updateChannelEmbed("feedback", id, newStatus));
    }

    public void sendBugReportStatusUpdate(String id, String newStatus) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.bugreport.enabled", true)) return;
        SchedulerUtil.runAsync(core.getPlugin(), () -> updateChannelEmbed("bugreport", id, newStatus));
    }

    public void sendSupportStatusUpdate(String id, String newStatus) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.support.enabled", true)) return;
        SchedulerUtil.runAsync(core.getPlugin(), () -> updateChannelEmbed("support", id, newStatus));
    }

    private void updateChannelEmbed(String type, String ticketId, String newStatus) {
        String channelId = channelMap.get(ticketId);
        if (channelId == null) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            // Channel was deleted, clean up mapping
            removeChannelMapping(ticketId);
            return;
        }

        Map<String, StatusDef> statuses = getStatusMap(type);
        StatusDef statusDef = statuses.getOrDefault(newStatus, statuses.get("PENDING"));

        // Rename channel
        String shortId = ticketId.substring(0, Math.min(8, ticketId.length()));
        String prefix = getPrefix(type);
        String newName = buildChannelName(prefix, shortId, newStatus.toLowerCase());
        channel.getManager().setName(newName).queue(
                null,
                e -> core.getPlugin().getLogger().warning("[Discord] Lỗi đổi tên channel: " + e.getMessage())
        );

        // Update the first message embed
        channel.getHistory().retrievePast(10).queue(messages -> {
            messages.stream()
                    .filter(m -> m.getAuthor().equals(jda.getSelfUser()) && !m.getEmbeds().isEmpty())
                    .findFirst()
                    .ifPresent(msg -> {
                        var oldEmbed = msg.getEmbeds().get(0);
                        EmbedBuilder newEmbed = new EmbedBuilder(oldEmbed)
                                .setColor(new Color(statusDef.color))
                                .clearFields()
                                .addField("👤 Người chơi",
                                        oldEmbed.getFields().size() > 0 ? oldEmbed.getFields().get(0).getValue() : "?", true)
                                .addField("📊 Trạng thái", statusDef.emoji + " " + statusDef.label, true)
                                .addField("📅 Thời gian",
                                        oldEmbed.getFields().size() > 2 ? oldEmbed.getFields().get(2).getValue() : "?", true)
                                .addField("📝 Nội dung",
                                        oldEmbed.getFields().size() > 3 ? oldEmbed.getFields().get(3).getValue() : "?", false)
                                .setTimestamp(Instant.now());

                        List<ActionRow> buttons = buildButtons(type, ticketId, newStatus);
                        msg.editMessageEmbeds(newEmbed.build())
                                .setComponents(buttons)
                                .queue(
                                        null,
                                        e -> core.getPlugin().getLogger().warning("[Discord] Lỗi cập nhật embed: " + e.getMessage())
                                );
                    });
        }, e -> core.getPlugin().getLogger().warning("[Discord] Lỗi lấy history: " + e.getMessage()));

        logToDiscord("🔄 Trạng thái đổi: **" + prefix + "-" + shortId + "** → " + statusDef.emoji + " " + statusDef.label);
    }

    // ============================================
    // Support chat: MC → Discord
    // ============================================

    public void sendSupportChatMessage(String ticketId, String senderName, String message, boolean isStaff) {
        if (!isEnabled() || jda == null) return;
        if (!core.getPlugin().getConfig().getBoolean("discord.support.enabled", true)) return;

        SchedulerUtil.runAsync(core.getPlugin(), () -> {
            String channelId = channelMap.get(ticketId);
            if (channelId == null) return;

            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) return;

            String roleTag = isStaff ? "🛡️ Staff" : "🎮 Player";
            int color = isStaff ? 0x00AAFF : 0x55FF55;

            EmbedBuilder chatEmbed = new EmbedBuilder()
                    .setDescription(message)
                    .setColor(new Color(color))
                    .setAuthor(roleTag + " | " + senderName)
                    .setTimestamp(Instant.now());

            channel.sendMessageEmbeds(chatEmbed.build()).queue(
                    null,
                    e -> core.getPlugin().getLogger().warning("[Discord] Lỗi gửi chat: " + e.getMessage())
            );
        });
    }

    // ============================================
    // Event handlers
    // ============================================

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String customId = event.getComponentId();
        String[] parts = customId.split("_");
        if (parts.length < 3) return;

        String type = parts[0];
        String newStatus = parts[1];
        String ticketId = String.join("_", Arrays.copyOfRange(parts, 2, parts.length));

        if (!type.equals("feedback") && !type.equals("bugreport") && !type.equals("support")) return;

        Map<String, StatusDef> statuses = getStatusMap(type);
        if (!statuses.containsKey(newStatus)) {
            event.reply("❌ Trạng thái không hợp lệ!").setEphemeral(true).queue();
            return;
        }

        // Defer the edit immediately to avoid 3-second timeout
        event.deferEdit().queue();

        StatusDef statusDef = statuses.get(newStatus);
        String discordUser = event.getUser().getName();

        // Update embed
        var oldEmbed = event.getMessage().getEmbeds().isEmpty() ? null : event.getMessage().getEmbeds().get(0);
        if (oldEmbed != null) {
            EmbedBuilder updated = new EmbedBuilder(oldEmbed)
                    .setColor(new Color(statusDef.color))
                    .clearFields()
                    .addField("👤 Người chơi",
                            oldEmbed.getFields().size() > 0 ? oldEmbed.getFields().get(0).getValue() : "?", true)
                    .addField("📊 Trạng thái", statusDef.emoji + " " + statusDef.label, true)
                    .addField("📅 Thời gian",
                            oldEmbed.getFields().size() > 2 ? oldEmbed.getFields().get(2).getValue() : "?", true)
                    .addField("📝 Nội dung",
                            oldEmbed.getFields().size() > 3 ? oldEmbed.getFields().get(3).getValue() : "?", false)
                    .setTimestamp(Instant.now());

            List<ActionRow> buttons = buildButtons(type, ticketId, newStatus);
            event.getHook().editOriginalEmbeds(updated.build())
                    .setComponents(buttons)
                    .queue(
                            null,
                            e -> core.getPlugin().getLogger().warning("[Discord] Lỗi cập nhật embed: " + e.getMessage())
                    );
        }

        // Rename channel
        try {
            String shortId = ticketId.substring(0, Math.min(8, ticketId.length()));
            String prefix = getPrefix(type);
            String channelName = buildChannelName(prefix, shortId, newStatus.toLowerCase());
            event.getChannel().asTextChannel().getManager()
                    .setName(channelName).queue(
                            null,
                            e -> core.getPlugin().getLogger().warning("[Discord] Lỗi đổi tên channel: " + e.getMessage())
                    );
        } catch (Exception e) {
            core.getPlugin().getLogger().warning("[Discord] Lỗi đổi tên channel: " + e.getMessage());
        }

        // Update plugin status on main thread
        SchedulerUtil.runGlobalTask(core.getPlugin(), () -> {
            try {
                UUID uuid = UUID.fromString(ticketId);
                switch (type) {
                    case "feedback":
                        FeedbackStatus fs = FeedbackStatus.valueOf(newStatus);
                        core.getFeedbackManager().updateStatus(uuid, fs);
                        break;
                    case "bugreport":
                        BugReportStatus bs = BugReportStatus.valueOf(newStatus);
                        core.getBugReportManager().updateStatus(uuid, bs);
                        break;
                    case "support":
                        SupportTicketStatus ss = SupportTicketStatus.valueOf(newStatus);
                        core.getSupportTicketManager().updateStatus(uuid, ss);
                        break;
                }
                core.getPlugin().getLogger().info("[Discord] " + discordUser + " đổi trạng thái " + type + "/" + ticketId.substring(0, 8) + " → " + newStatus);
            } catch (Exception e) {
                core.getPlugin().getLogger().warning("[Discord] Lỗi cập nhật status: " + e.getMessage());
            }
        });

        logToDiscord("🔘 **" + discordUser + "** đổi trạng thái **" + type + "/" + ticketId.substring(0, 8) + "** → " + statusDef.emoji + " " + statusDef.label);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"delete".equals(event.getName())) return;

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("❌ Bạn cần quyền **Manage Channels** để dùng lệnh này!").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        String channelName = channel.getName();

        // Check if this is a tracked channel
        boolean isTracked = channelName.startsWith("gopy-") || channelName.startsWith("baoloi-") || channelName.startsWith("hotro-");
        if (!isTracked) {
            event.reply("❌ Channel này không phải channel **Góp Ý**, **Báo Lỗi** hoặc **Hỗ Trợ**!").setEphemeral(true).queue();
            return;
        }

        String name = channel.getName();
        event.reply("🗑️ Đang xóa channel **#" + name + "**...").setEphemeral(true).queue();

        // Remove from channelMap using reverse lookup
        String ticketId = reverseChannelMap.get(channel.getId());
        if (ticketId != null) {
            removeChannelMapping(ticketId);
        }

        channel.delete().reason("Xóa bởi " + event.getUser().getName() + " qua /delete").queue(
                s -> {
                    core.getPlugin().getLogger().info("[Discord] " + event.getUser().getName() + " đã xóa channel #" + name);
                    updateBotActivity();
                    logToDiscord("🗑️ **" + event.getUser().getName() + "** đã xóa channel **#" + name + "**");
                },
                e -> core.getPlugin().getLogger().warning("[Discord] Lỗi xóa channel: " + e.getMessage())
        );
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        // O(1) lookup using reverse map instead of O(n) loop
        String channelId = event.getChannel().getId();
        String ticketId = reverseChannelMap.get(channelId);
        if (ticketId == null) return;

        // Forward to game on main thread
        SchedulerUtil.runGlobalTask(core.getPlugin(), () -> {
            try {
                UUID uuid = UUID.fromString(ticketId);
                SupportTicket ticket = core.getSupportTicketManager().getTicket(uuid);
                if (ticket == null) return;

                String message = event.getMessage().getContentRaw();
                String senderName = event.getAuthor().getName();

                // Save chat message
                SupportChatMessage chatMsg = SupportChatMessage.builder()
                        .id(UUID.randomUUID())
                        .ticketId(uuid)
                        .senderUUID(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .senderName("[Discord] " + senderName)
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .staff(true)
                        .build();

                SchedulerUtil.runAsync(core.getPlugin(), () ->
                        core.getSupportChatStorageProvider().saveMessage(chatMsg));

                // Deliver to in-game
                String roleTag = core.getMessageNoPrefix("support-chat-role-staff");
                String formatted = core.getMessageNoPrefix("support-chat-format")
                        .replace("{role}", roleTag)
                        .replace("{player}", "[Discord] " + senderName)
                        .replace("{message}", message)
                        .replace("{time}", "");

                var chatManager = core.getSupportChatManager();
                for (var entry : chatManager.getActiveSessions().entrySet()) {
                    if (entry.getValue().equals(uuid)) {
                        var participant = Bukkit.getPlayer(entry.getKey());
                        if (participant != null && participant.isOnline()) {
                            participant.sendMessage(formatted);
                        }
                    }
                }

                // Notify ticket owner if not in chat
                var ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
                if (ticketOwner != null && ticketOwner.isOnline()
                        && !chatManager.isInChat(ticketOwner.getUniqueId())) {
                    String notification = core.getMessage("support-chat-notify-new")
                            .replace("{player}", "[Discord] " + senderName)
                            .replace("{id}", ticketId.substring(0, 8));
                    ticketOwner.sendMessage(notification);
                }

                core.getPlugin().getLogger().info("[Chat] Discord → MC: " + senderName + ": " + message.substring(0, Math.min(50, message.length())));
            } catch (Exception e) {
                core.getPlugin().getLogger().log(Level.WARNING, "[Chat] Lỗi xử lý tin nhắn Discord", e);
            }
        });
    }

    // ============================================
    // Button builders
    // ============================================

    private List<ActionRow> buildButtons(String type, String ticketId, String currentStatus) {
        switch (type) {
            case "feedback":
                return buildFeedbackButtons(ticketId, currentStatus);
            case "bugreport":
                return buildBugReportButtons(ticketId, currentStatus);
            case "support":
                return buildSupportButtons(ticketId, currentStatus);
            default:
                return Collections.emptyList();
        }
    }

    private List<ActionRow> buildFeedbackButtons(String ticketId, String current) {
        return List.of(
                ActionRow.of(
                        makeButton("feedback", "READ", ticketId, "Đã đọc", "👁", current),
                        makeButton("feedback", "ACCEPTED", ticketId, "Chấp nhận", "✅", current),
                        makeButton("feedback", "DEPLOYING", ticketId, "Triển khai", "🚀", current)
                ),
                ActionRow.of(
                        makeButton("feedback", "IMPLEMENTED", ticketId, "Đã triển khai", "⚡", current),
                        makeButton("feedback", "REJECTED", ticketId, "Từ chối", "❌", current)
                )
        );
    }

    private List<ActionRow> buildBugReportButtons(String ticketId, String current) {
        return List.of(
                ActionRow.of(
                        makeButton("bugreport", "READ", ticketId, "Đã đọc", "👁", current),
                        makeButton("bugreport", "CHECKING", ticketId, "Đang kiểm tra", "🔍", current),
                        makeButton("bugreport", "FIXING", ticketId, "Đang sửa", "🔧", current)
                ),
                ActionRow.of(
                        makeButton("bugreport", "FIXED", ticketId, "Đã sửa", "✅", current),
                        makeButton("bugreport", "REJECTED", ticketId, "Không phải lỗi", "❌", current)
                )
        );
    }

    private List<ActionRow> buildSupportButtons(String ticketId, String current) {
        return List.of(
                ActionRow.of(
                        makeButton("support", "READ", ticketId, "Đã đọc", "👁", current),
                        makeButton("support", "PROCESSING", ticketId, "Đang xử lý", "🔄", current),
                        makeButton("support", "RESOLVED", ticketId, "Đã giải quyết", "✅", current),
                        makeButton("support", "REJECTED", ticketId, "Từ chối", "❌", current)
                )
        );
    }

    private Button makeButton(String type, String status, String ticketId, String label, String emoji, String currentStatus) {
        String id = type + "_" + status + "_" + ticketId;

        // Button custom ID has a 100 character limit
        if (id.length() > 100) {
            id = id.substring(0, 100);
            core.getPlugin().getLogger().warning("[Discord] Button ID cắt ngắn: " + type + "/" + status + " (vượt 100 ký tự)");
        }

        ButtonStyle style;
        if (status.equals(currentStatus)) {
            if ("REJECTED".equals(status)) style = ButtonStyle.DANGER;
            else if ("ACCEPTED".equals(status) || "FIXED".equals(status) || "IMPLEMENTED".equals(status) || "RESOLVED".equals(status))
                style = ButtonStyle.SUCCESS;
            else style = ButtonStyle.PRIMARY;
        } else {
            style = ButtonStyle.SECONDARY;
        }

        Button button = Button.of(style, id, label).withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji));
        return status.equals(currentStatus) ? button.asDisabled() : button;
    }

    // ============================================
    // Helpers
    // ============================================

    private Map<String, StatusDef> getStatusMap(String type) {
        switch (type) {
            case "feedback": return FEEDBACK_STATUSES;
            case "bugreport": return BUGREPORT_STATUSES;
            case "support": return SUPPORT_STATUSES;
            default: return FEEDBACK_STATUSES;
        }
    }

    private String getPrefix(String type) {
        switch (type) {
            case "feedback": return "gopy";
            case "bugreport": return "baoloi";
            case "support": return "hotro";
            default: return "ticket";
        }
    }

    /**
     * Build channel name with optional cluster prefix.
     * Format: prefix-cluster-id or prefix-id (if no cluster)
     * Optionally appends a status suffix.
     */
    private String buildChannelName(String prefix, String shortId, String statusSuffix) {
        String cluster = core.getPlugin().getConfig().getString("discord.channel-cluster", "");
        StringBuilder sb = new StringBuilder(prefix);
        if (cluster != null && !cluster.isEmpty()) {
            sb.append("-").append(cluster);
        }
        sb.append("-").append(shortId);
        if (statusSuffix != null && !statusSuffix.isEmpty()) {
            sb.append("-").append(statusSuffix);
        }
        return sb.toString();
    }

    private String formatTimestamp(long millis) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(millis));
    }

    private record StatusDef(String label, String emoji, int color) {}
}
