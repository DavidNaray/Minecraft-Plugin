package com.David;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CraftInstance {
    private final World world;
    private final List<BlockDisplay> parts = new ArrayList<>();
    private List<ControlSign> controlSigns = new ArrayList<>();
    private Location pivot;   // geometric center
    private float yaw = 0;    // rotation in degrees
    private Vector velocity = new Vector(0, 0, 0);

    public CraftInstance(World world, Location pivot) {
        this.world = world;
        this.pivot = pivot.clone();
    }

    public void addPart(BlockDisplay bd) {
        parts.add(bd);
    }

    public Location getPivot() {
        return pivot.clone();
    }

    public void setVelocity(Vector v) {
        this.velocity = v;
    }

    public void addVelocity(Vector v) {
        this.velocity.add(v);
    }

    public void addControlSign(ControlSign sign) {
        controlSigns.add(sign);
    }

    public void rotate(float deltaYaw) {
        this.yaw += deltaYaw;
    }

    public void tick() {
        // Move pivot by velocity
        pivot.add(velocity);

        // Apply transform to each part
        for (BlockDisplay bd : parts) {
            Location rel = bd.getLocation().clone().subtract(pivot); // offset from pivot
            rel = rotateAroundY(rel, yaw); // apply yaw
            bd.teleport(pivot.clone().add(rel));
        }
    }

    // Rotate vector around Y axis
    private Location rotateAroundY(Location loc, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double x = loc.getX();
        double z = loc.getZ();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double nx = cos * x - sin * z;
        double nz = sin * x + cos * z;
        return new Location(loc.getWorld(), nx, loc.getY(), nz, loc.getYaw(), loc.getPitch());
    }
}
