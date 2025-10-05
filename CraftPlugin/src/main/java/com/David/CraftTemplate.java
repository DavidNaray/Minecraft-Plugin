package com.David;

import org.bukkit.Material;
import org.bukkit.block.Block;
import java.util.*;

public class CraftTemplate {

    private final String name;
    private final Map<String, Material> blocks; // relative coords -> material

    public CraftTemplate(String name) {
        this.name = name;
        this.blocks = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Map<String, Material> getBlocks() {
        return blocks;
    }

    public void addBlock(int x, int y, int z, Material material) {
        String key = x + "," + y + "," + z;
        blocks.put(key, material);
    }
}
