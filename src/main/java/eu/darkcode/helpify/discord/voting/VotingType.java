package eu.darkcode.helpify.discord.voting;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public enum VotingType {
    UNKNOWN(-1, "Unknown"),
    ONE_ANSWER(0, "Select one"),
    MULTIPLE_ANSWERS(1, "Select multiple"),
    OPEN_ANSWER(2, "Free answer");

    public static @NotNull VotingType fromId(int id){
        return fromId.getOrDefault(id, UNKNOWN);
    }

    private static final HashMap<Integer, VotingType> fromId = new HashMap<>();

    static{
        for (VotingType value : values())
            fromId.put(value.id, value);
    }

    public static Collection<VotingType> useable() {
        return Arrays.stream(values()).filter(a -> a != UNKNOWN).collect(Collectors.toList());
    }

    private final int id;
    private final String message;

    VotingType(int id, String message) {
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