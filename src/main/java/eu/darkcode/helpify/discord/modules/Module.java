package eu.darkcode.helpify.discord.modules;

import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public enum Module {
    UNKNOWN(-1, "Unknown"),
    VOTING(0, "Voting"),
    REAL_NAMES(1, "Real names");

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

    public static Collection<Module> useable() {
        return Arrays.stream(values()).filter(a -> a != UNKNOWN).collect(Collectors.toList());
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}