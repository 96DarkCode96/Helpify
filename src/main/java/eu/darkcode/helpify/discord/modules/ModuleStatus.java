package eu.darkcode.helpify.discord.modules;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public enum ModuleStatus {
    UNKNOWN(-1, "Unknown"),
    WAITING(0, "Waiting for request :hourglass:"),
    ENABLED(1, "Enabled :white_check_mark:"),
    DISABLED(2, "Disabled :x:"),
    REQUESTED(3, "Requested :thought_balloon:"),
    BLOCKED(4, "Blocked :no_entry:");

    public static @NotNull ModuleStatus fromId(int id){
        return fromId.getOrDefault(id, UNKNOWN);
    }

    private static final HashMap<Integer, ModuleStatus> fromId = new HashMap<>();

    static{
        for (ModuleStatus value : values())
            fromId.put(value.id, value);
    }

    private final int id;
    private final String message;

    ModuleStatus(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}