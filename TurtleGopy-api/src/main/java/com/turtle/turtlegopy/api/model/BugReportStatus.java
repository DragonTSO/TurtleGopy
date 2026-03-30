package com.turtle.turtlegopy.api.model;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public enum BugReportStatus {

    PENDING("§e⏳ Chờ xử lý", Material.CLOCK, "§e"),
    READ("§b👁 Đã đọc", Material.ENDER_EYE, "§b"),
    CHECKING("§e🔍 Đang kiểm tra", Material.SPYGLASS, "§e"),
    FIXING("§6🔧 Đang sửa", Material.ANVIL, "§6"),
    FIXED("§a✔ Đã sửa", Material.EMERALD, "§a"),
    REJECTED("§c✘ Không phải lỗi", Material.BARRIER, "§c");

    private final String displayName;
    private final Material icon;
    private final String color;

    BugReportStatus(String displayName, Material icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }
}
