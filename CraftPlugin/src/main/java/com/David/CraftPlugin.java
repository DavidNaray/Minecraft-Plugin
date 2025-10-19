package com.David;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import org.bukkit.block.data.BlockData;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
//import java.util.Vector;

public class CraftPlugin extends JavaPlugin {

    // Example config — in practice this will come from crafts.yml
    private final Set<Material> allowedBlocks = Set.of(
            Material.OAK_PLANKS,
            Material.WHITE_WOOL,
            Material.SPRUCE_PLANKS,

            Material.OAK_SIGN,
            Material.OAK_WALL_SIGN,
            Material.BIRCH_SIGN,
            Material.BIRCH_WALL_SIGN,
            Material.SPRUCE_SIGN,
            Material.SPRUCE_WALL_SIGN,
            Material.DARK_OAK_SIGN,
            Material.DARK_OAK_WALL_SIGN
    );
    private final Material separatorBlock = Material.COBBLESTONE;
    private final Map<String, CraftTemplate> templates = new HashMap<>();
    private final int maxSize = 5000;
    private final int minSize = 10;
    private final Map<UUID, CraftInstance> activeCrafts = new HashMap<>();

    @Override
    public void onEnable() {
        CraftInstance demoCraft = new CraftInstance(getServer().getWorlds().get(0), new Location(getServer().getWorlds().get(0), 0, 100, 0));
        this.getCommand("pilot").setExecutor(new PilotCommand(this, demoCraft));
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
            CraftTemplate template = new CraftTemplate(craftName);
            // Step 1: detect blocks with flood fill
            Set<Block> detected = floodFill(target);

            if (detected.isEmpty()) {
                player.sendMessage("No craft detected!");
                return true;
            }

            // Step 2: normalize into CraftTemplate

            int baseX = target.getX();
            int baseY = target.getY();
            int baseZ = target.getZ();

            for (Block b : detected) {
                int relX = b.getX() - baseX;
                int relY = b.getY() - baseY;
                int relZ = b.getZ() - baseZ;
                template.addBlock(relX, relY, relZ, b.getType());

                if (b.getState() instanceof Sign sign) {
                    for (String line : sign.getLines()) {
                        if (line.equalsIgnoreCase("[LEFT]") ||
                                line.equalsIgnoreCase("[RIGHT]") ||
                                line.equalsIgnoreCase("[FORWARD]") ||
                                line.equalsIgnoreCase("[BACK]")) {

                            // relative position from start
                            Vector rel=new Vector(relX,relY,relZ);
//                            rel.add(relX);
//                            rel.add(relY);
//                            rel.add(relZ);

                            String clean = line.replace("[", "").replace("]", "").toLowerCase();
                            template.addControlSign(new ControlSign(rel, clean));
//                            template.addControlSign(new ControlSign(rel, line.replace("[", "").replace("]", "")));
                            player.sendMessage("Registered control sign: " + clean + " at " + rel);
                        }
                    }
                }


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
        CraftInstance instance = new CraftInstance(world, base.clone());
        activeCrafts.put(player.getUniqueId(), instance);

        for (Map.Entry<String, Material> e : template.getBlocks().entrySet()) {
            Material mat = e.getValue();
            if (mat.name().endsWith("_SIGN")) continue;

            String[] parts = e.getKey().split(",");
            int rx = Integer.parseInt(parts[0]);
            int ry = Integer.parseInt(parts[1]);
            int rz = Integer.parseInt(parts[2]);

            Location loc = base.clone().add(rx, ry, rz);
            BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
            display.setBlock(e.getValue().createBlockData());
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);

            instance.addPart(display);


        }
        // Check if this block position matches a control sign
        for (ControlSign sign : template.getControlSigns()) {
            Location textLoc = base.clone().add(sign.getRelativePos())
                    .add(0.5, 0.5, 0.5);     // center in block
//                    .add(0, 0, 0.25);       // offset slightly outwards

            TextDisplay text = (TextDisplay) world.spawnEntity(textLoc, EntityType.TEXT_DISPLAY);
            text.text(Component.text("[" + sign.getCommand().toUpperCase() + "]")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            text.setBillboard(Display.Billboard.CENTER);  // double-sided
            text.setShadowed(true);
            text.setBrightness(new Display.Brightness(15, 15));
            text.setViewRange(16f);

            // ✅ Solid background (opaque)
            text.setBackgroundColor(Color.fromARGB(255, 240, 220, 180)); // wood-like sign color
            text.setLineWidth(80); // controls wrap and scale
            text.setAlignment(TextDisplay.TextAlignment.CENTER);

            text.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new Quaternionf()
            ));

            instance.addControlSign(sign);
        }
    }

}
