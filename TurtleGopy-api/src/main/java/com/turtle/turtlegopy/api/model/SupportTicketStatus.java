package com.turtle.turtlegopy.api.model;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public enum SupportTicketStatus {

    PENDING("§e⏳ Chờ xử lý", Material.CLOCK, "§e"),
    READ("§b👁 Đã đọc", Material.ENDER_EYE, "§b"),
    PROCESSING("§6🔄 Đang xử lý", Material.ANVIL, "§6"),
    RESOLVED("§a✔ Đã giải quyết", Material.EMERALD, "§a"),
    REJECTED("§c✘ Từ chối", Material.BARRIER, "§c");

    private final String displayName;
    private final Material icon;
    private final String color;

    SupportTicketStatus(String displayName, Material icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}
