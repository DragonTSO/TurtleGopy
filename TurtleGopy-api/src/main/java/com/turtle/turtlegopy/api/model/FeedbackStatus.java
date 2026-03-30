package com.turtle.turtlegopy.api.model;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public enum FeedbackStatus {

    PENDING("§e⏳ Chờ xử lý", Material.CLOCK, "§e"),
    READ("§b👁 Đã đọc", Material.ENDER_EYE, "§b"),
    ACCEPTED("§a✔ Đã chấp nhận", Material.EMERALD, "§a"),
    DEPLOYING("§6🚀 Đang triển khai", Material.CRAFTING_TABLE, "§6"),
    REJECTED("§c✘ Từ chối", Material.BARRIER, "§c"),
    IMPLEMENTED("§d⚡ Đã triển khai", Material.NETHER_STAR, "§d");

    private final String displayName;
    private final Material icon;
    private final String color;

    FeedbackStatus(String displayName, Material icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}
