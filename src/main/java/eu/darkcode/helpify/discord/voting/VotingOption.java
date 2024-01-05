package eu.darkcode.helpify.discord.voting;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VotingOption {

    private final long id;
    private @NotNull String message;
    private @NotNull List<Vote> voters;

    public VotingOption(long id) {
        this(id, "", new ArrayList<>());
    }

    public VotingOption(long id, @NotNull String message) {
        this(id, message, new ArrayList<>());
    }

    public VotingOption(long id, @NotNull String message, @NotNull List<Vote> voters) {
        this.id = id;
        this.message = message;
        this.voters = voters;
    }

    public long getId() {
        return id;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    public void setMessage(@NotNull String message) {
        this.message = message;
    }

    @NotNull
    public List<Vote> getVoters() {
        return voters;
    }

    public void setVoters(@NotNull List<Vote> voters) {
        this.voters = voters;
    }

    public String formatVoters() {
        if(getVoters().isEmpty()){
            return "> ---";
        }
        return getVoters().stream().sorted(Comparator.comparingLong(v -> v.time().getTime())).map((v) -> "> " + v.user().effName()).collect(Collectors.joining("\n"));
    }

    public void addVote(Vote vote) {
        voters.add(vote);
    }
}