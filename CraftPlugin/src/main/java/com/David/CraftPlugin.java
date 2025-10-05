package com.David;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CraftPlugin extends JavaPlugin {

    // Example config — in practice this will come from crafts.yml
    private final Set<Material> allowedBlocks = Set.of(
            Material.OAK_PLANKS,
            Material.WHITE_WOOL,
            Material.SPRUCE_PLANKS
    );
    private final Material separatorBlock = Material.COBBLESTONE;
    private final int maxSize = 5000;
    private final int minSize = 10;

    @Override
    public void onEnable() {
        getLogger().info("CraftPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CraftPlugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("detectcraft")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can run this command.");
                return true;
            }

            Player player = (Player) sender;
            Block seed = player.getTargetBlockExact(10); // look at block within 10 blocks
            if (seed == null) {
                player.sendMessage("Look at a block within 10 blocks to detect a craft.");
                return true;
            }

            Set<Block> craftBlocks = floodFill(seed);

            if (craftBlocks.isEmpty()) {
                player.sendMessage("No valid craft found.");
            } else if (craftBlocks.size() < minSize) {
                player.sendMessage("Too small (" + craftBlocks.size() + " blocks). Minimum is " + minSize + ".");
            } else {
                Map<Material, Integer> counts = countBlocks(craftBlocks);

                player.sendMessage("Craft detected with " + craftBlocks.size() + " blocks total:");
                for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
                    player.sendMessage("- " + entry.getValue() + " × " + entry.getKey().name());
                }
            }

            return true;
        }
        return false;
    }

    private Set<Block> floodFill(Block start) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> toVisit = new ArrayDeque<>();

        toVisit.add(start);

        while (!toVisit.isEmpty()) {
            Block current = toVisit.poll();

            if (!allowedBlocks.contains(current.getType())) continue; // only allowed blocks
            if (visited.contains(current)) continue; // already done
            if (current.getType() == separatorBlock) continue; // separators stop the fill

            visited.add(current);

            if (visited.size() > maxSize) {
                getLogger().warning("Craft too large! Aborting.");
                return Collections.emptySet();
            }

            // Add 6 neighbors (up, down, north, south, east, west)
            for (Block neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }

        return visited;
    }

    private List<Block> getNeighbors(Block block) {
        return List.of(
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 1, 0),
                block.getRelative(0, -1, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1)
        );
    }

    private Map<Material, Integer> countBlocks(Set<Block> blocks) {
        Map<Material, Integer> counts = new HashMap<>();
        for (Block b : blocks) {
            counts.merge(b.getType(), 1, Integer::sum);
        }
        return counts;
    }
}
