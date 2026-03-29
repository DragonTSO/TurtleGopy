package com.turtle.turtlegopy;

import com.turtle.turtlegopy.core.TurtleGopyCore;

import org.bukkit.plugin.java.JavaPlugin;

public class TurtleGopyPlugin extends JavaPlugin {

    private TurtleGopyCore core;

    @Override
    public void onEnable() {
        core = new TurtleGopyCore(this);
        core.onEnable();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.onDisable();
        }
    }
}
