package com.David;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;

import java.util.*;

public class CraftPlugin extends JavaPlugin {

    // Example config — in practice this will come from crafts.yml
    private final Set<Material> allowedBlocks = Set.of(
            Material.OAK_PLANKS,
            Material.WHITE_WOOL,
            Material.SPRUCE_PLANKS
    );
    private final Material separatorBlock = Material.COBBLESTONE;
    private final Map<String, CraftTemplate> templates = new HashMap<>();
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
        Player player = (Player) sender;
        if (label.equalsIgnoreCase("detectcraft")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can run this command.");
                return true;
            }

//            Player player = (Player) sender;
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
        if (label.equalsIgnoreCase("registercraft")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only!");
                return true;
            }
//            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage("Usage: /registercraft <name>");
                return true;
            }

            String craftName = args[0];
            Block target = player.getTargetBlockExact(5); // look at block within 5 blocks
            if (target == null) {
                player.sendMessage("No block in sight!");
                return true;
            }

            // Step 1: detect blocks with flood fill
            Set<Block> detected = floodFill(target);

            if (detected.isEmpty()) {
                player.sendMessage("No craft detected!");
                return true;
            }

            // Step 2: normalize into CraftTemplate
            CraftTemplate template = new CraftTemplate(craftName);
            int baseX = target.getX();
            int baseY = target.getY();
            int baseZ = target.getZ();

            for (Block b : detected) {
                int relX = b.getX() - baseX;
                int relY = b.getY() - baseY;
                int relZ = b.getZ() - baseZ;
                template.addBlock(relX, relY, relZ, b.getType());
            }

            // Step 3: store template
            templates.put(craftName.toLowerCase(), template);
            player.sendMessage("Craft '" + craftName + "' registered with " + detected.size() + " blocks!");

            return true;
        }
        if (label.equalsIgnoreCase("spawnclone")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only!");
                return true;
            }
//            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage("Usage: /spawnclone <name>");
                return true;
            }

            String craftName = args[0].toLowerCase();
            CraftTemplate template = templates.get(craftName);
            if (template == null) {
                player.sendMessage("No craft registered with that name.");
                return true;
            }

            Block target = player.getTargetBlockExact(10); // within 10 blocks
            if (target == null) {
                player.sendMessage("No block in sight!");
                return true;
            }

            int baseX = target.getX();
            int baseY = target.getY() + 1; // paste one above target block
            int baseZ = target.getZ();

            // Try to find a valid Y offset where it fits
            int maxAttempts = 20; // don’t loop forever
            boolean placed = false;
            for (int offsetY = 0; offsetY < maxAttempts; offsetY++) {
                if (canPlace(template, player.getWorld(), baseX, baseY + offsetY, baseZ)) {
                    pasteTemplate(template, player.getWorld(), baseX, baseY + offsetY, baseZ);
                    player.sendMessage("Craft '" + craftName + "' spawned!");
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                player.sendMessage("Not enough space to spawn craft!");
            }
            return true;
        }
        if (label.equalsIgnoreCase("spawnentityclone")) {

            if (args.length < 1) {
                player.sendMessage("Usage: /spawnentityclone <name>");
                return true;
            }

            String name = args[0].toLowerCase(Locale.ROOT);
            CraftTemplate template = templates.get(name);
            if (template == null) {
                player.sendMessage("No craft template named '" + name + "'.");
                return true;
            }

            Block target = player.getTargetBlockExact(10);
            if (target == null) {
                player.sendMessage("Look at a block within 10 blocks to place the entity clone.");
                return true;
            }

            // base location offset a little so player can see it
            Location base = target.getLocation().add(0.0, 1.0, 0.0);
            spawnEntityClone(player, template, base);
            player.sendMessage("Spawned entity clone '" + name + "'.");
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

    private boolean canPlace(CraftTemplate template, World world, int baseX, int baseY, int baseZ) {
        for (Map.Entry<String, Material> entry : template.getBlocks().entrySet()) {
            String[] parts = entry.getKey().split(",");
            int relX = Integer.parseInt(parts[0]);
            int relY = Integer.parseInt(parts[1]);
            int relZ = Integer.parseInt(parts[2]);

            int x = baseX + relX;
            int y = baseY + relY;
            int z = baseZ + relZ;

            Block block = world.getBlockAt(x, y, z);
            if (!block.isEmpty() && !block.isLiquid()) {
                return false; // blocked
            }
        }
        return true;
    }

    private void pasteTemplate(CraftTemplate template, World world, int baseX, int baseY, int baseZ) {
        for (Map.Entry<String, Material> entry : template.getBlocks().entrySet()) {
            String[] parts = entry.getKey().split(",");
            int relX = Integer.parseInt(parts[0]);
            int relY = Integer.parseInt(parts[1]);
            int relZ = Integer.parseInt(parts[2]);

            int x = baseX + relX;
            int y = baseY + relY;
            int z = baseZ + relZ;

            Block block = world.getBlockAt(x, y, z);
            block.setType(entry.getValue());
        }
    }

    private void spawnEntityClone(Player player, CraftTemplate template, Location base) {
        World world = player.getWorld();

        for (Map.Entry<String, Material> e : template.getBlocks().entrySet()) {
            String[] parts = e.getKey().split(",");
            int rx = Integer.parseInt(parts[0]);
            int ry = Integer.parseInt(parts[1]);
            int rz = Integer.parseInt(parts[2]);

            Location loc = base.clone().add(rx, ry, rz);
            // spawn BlockDisplay
            BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
            BlockData bd = e.getValue().createBlockData();
            display.setBlock(bd);

            // optional settings to make them stable / immediate
            try {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(1);
            } catch (Throwable ignored) {
                // older API versions might not have those setters; ignore safely
            }
        }
    }
}
