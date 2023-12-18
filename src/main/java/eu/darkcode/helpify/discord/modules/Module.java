package eu.darkcode.helpify.discord.modules;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public enum Module {
    UNKNOWN(-1, "Unknown"),
    VOTING(0, "Voting");

    public static @NotNull Module fromId(int id){
        return fromId.getOrDefault(id, UNKNOWN);
    }

    private static final HashMap<Integer, Module> fromId = new HashMap<>();

    static{
        for (Module value : values())
            fromId.put(value.id, value);
    }

    private final int id;
    private final String message;

    Module(int id, String message) {
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