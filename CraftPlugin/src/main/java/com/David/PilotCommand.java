package com.David;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PilotCommand implements CommandExecutor{
    private final JavaPlugin plugin;
    private CraftInstance craft; // Single test craft for now

    public PilotCommand(JavaPlugin plugin, CraftInstance craft) {
        this.plugin = plugin;
        this.craft = craft;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (craft == null) {
            player.sendMessage("No craft to pilot!");
            return true;
        }

        player.sendMessage("You are now piloting this craft!");

        // Schedule updates
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Temporary keybind logic (WASD simulated with sneak/jump for now)
                if (player.isSneaking()) {
                    craft.addVelocity(new Vector(0, 0, 0.1)); // forward thrust
                }
                if (player.isJumping()) {
                    craft.rotate(2f); // rotate slowly
                }

                craft.tick();
            }
        }.runTaskTimer(plugin, 1, 1);

        return true;
    }
}
