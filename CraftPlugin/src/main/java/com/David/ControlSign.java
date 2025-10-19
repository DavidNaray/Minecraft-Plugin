package com.David;

//import java.util.*;

import org.bukkit.util.Vector;
public class ControlSign {
    private Vector relativePos;
    private String command; // e.g., "LEFT", "RIGHT", "FORWARD"

    public ControlSign(Vector relativePos, String command) {
        this.relativePos = relativePos;
        this.command = command.toUpperCase();
    }

    public Vector getRelativePos() {
        return relativePos;
    }

    public String getCommand() {
        return command;
    }
}
