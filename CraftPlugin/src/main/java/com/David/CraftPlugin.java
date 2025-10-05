package com.David;

import org.bukkit.plugin.java.JavaPlugin;

public class CraftPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CraftPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CraftPlugin disabled!");
    }
}
