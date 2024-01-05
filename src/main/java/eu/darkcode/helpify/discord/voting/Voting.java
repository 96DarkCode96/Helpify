package eu.darkcode.helpify.discord.voting;

import eu.darkcode.helpify.discord.wrappers.User;
import eu.darkcode.helpify.objects.SelfHealingURL;
import eu.darkcode.helpify.objects.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

public class Voting implements SelfHealingURL {

    public static final String STRING_REGEX = "[\\p{L} ?!_\\-\"'.]";

    public static final int MIN_CONTENT_LENGTH = 10;
    public static final int MAX_TITLE_LENGTH = 127;
    public static final int MAX_DESCRIPTION_LENGTH = 254;
    public static final int MIN_OPTION_COUNT = 2;
    public static final int MAX_OPTION_COUNT = 10;
    public static final int MIN_OPTION_LENGTH = 2;
    public static final int MAX_OPTION_LENGTH = 64;

    public static final int FLAG_DEFAULT = 0b0000;
    public static final int FLAG_CREATING = 0b0001;
    public static final int FLAG_CLOSED = 0b0010;

    private final HashMap<Long, VotingOption> options = new HashMap<>();
    private final @NotNull VotingType type;
    private final @NotNull User owner;
    private final long votingId;
    private int flags;
    private Long channelMessageId, discordMessageId;
    private @NotNull String title, description;

    public Voting(@NotNull VotingType type, @NotNull User owner, long votingId) {
        this(type, owner, votingId, FLAG_DEFAULT, "", "");
    }

    public Voting(@NotNull VotingType type, @NotNull User owner, long votingId, int flags) {
        this(type, owner, votingId, flags, "", "");
    }

    public Voting(@NotNull VotingType type, @NotNull User owner, long votingId, @NotNull String title, @NotNull String description) {
        this(type, owner, votingId, FLAG_DEFAULT, title, description);
    }

    public Long getChannelMessageId() {
        return channelMessageId;
    }

    public void setChannelMessageId(Long channelMessageId) {
        this.channelMessageId = channelMessageId;
    }

    public Long getDiscordMessageId() {
        return discordMessageId;
    }

    public void setDiscordMessageId(Long discordMessageId) {
        this.discordMessageId = discordMessageId;
    }

    public Voting(@NotNull VotingType type, @NotNull User owner, long votingId, int flags, @NotNull String title, @NotNull String description) {
        this.type = type;
        this.owner = owner;
        this.votingId = votingId;
        this.flags = flags;
        this.title = title;
        this.description = description;
    }

    public void enableCreatingMode(){
        flags |= FLAG_CREATING;
    }

    public void disableCreatingMode(){
        flags &= ~FLAG_CREATING;
    }

    public boolean isInCreatingMode(){
        return (flags & FLAG_CREATING) == FLAG_CREATING;
    }

    public void close(){
        flags |= FLAG_CLOSED;
    }

    public void open(){
        flags &= ~FLAG_CLOSED;
    }

    public boolean isClosed(){
        return (flags & FLAG_CLOSED) == FLAG_CLOSED;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public long getVotingId() {
        return votingId;
    }

    @NotNull
    public VotingType getType() {
        return type;
    }

    @NotNull
    public User getOwner() {
        return owner;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    public String generateVotingUrl() {
        return "https://helpify.matejtomecek.eu/guild/" + owner.getGuildWrapper().generateSelfHealingURL() + "/voting/" + generateSelfHealingURL() + "/";
    }

    @Override
    public String generateSelfHealingURL() {
        return StringUtil.slug(getTitle()) + "-" + getVotingId();
    }

    public boolean putOption(VotingOption option){
        if(!getType().equals(VotingType.ONE_ANSWER) && !getType().equals(VotingType.MULTIPLE_ANSWERS))
            return false;
        return options.putIfAbsent(option.getId(), option) == null;
    }

    public boolean removeOption(long id){
        if(!getType().equals(VotingType.ONE_ANSWER) && !getType().equals(VotingType.MULTIPLE_ANSWERS))
            return false;
        options.remove(id);
        return true;
    }

    public Collection<VotingOption> getOptions() {
        return options.values();
    }

    public VotingOption getOption(long optionId) {
        return options.get(optionId);
    }
}