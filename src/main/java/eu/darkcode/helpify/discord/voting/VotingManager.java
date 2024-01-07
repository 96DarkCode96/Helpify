package eu.darkcode.helpify.discord.voting;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.wrappers.GuildWrapper;
import eu.darkcode.helpify.discord.wrappers.User;
import eu.darkcode.helpify.objects.SelfHealingFactory;
import eu.darkcode.helpify.objects.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariadb.jdbc.Statement;

import java.awt.*;
import java.net.URI;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.darkcode.helpify.discord.voting.Voting.*;

public class VotingManager implements SelfHealingFactory<Voting> {

    private final GuildWrapper guild;
    private final HashMap<Long, Voting> votingCache = new HashMap<>();

    public VotingManager(GuildWrapper guild) {
        this.guild = guild;
        load();
    }

    public HashMap<Long, Voting> getVotingCache() {
        return votingCache;
    }

    private void load() {
        votingCache.clear();
        try (PreparedStatement st = Database.getConnection().prepareStatement("SELECT * FROM Voting WHERE guildId = " + guild.getGuild().getIdLong())) {
            ResultSet resultSet = st.executeQuery();
            while (resultSet.next()) {
                Voting voting = new Voting(
                        VotingType.fromId(resultSet.getInt("votingType")),
                        guild.getUserFactory().loadUser(resultSet.getLong("ownerId")),
                        resultSet.getLong("id"),
                        resultSet.getInt("votingFlags"),
                        resultSet.getString("title"),
                        resultSet.getString("description")
                );
                long channelMessage = resultSet.getLong("channelMessageId");
                if (!resultSet.wasNull())
                    voting.setChannelMessageId(channelMessage);
                long discordMessageId = resultSet.getLong("discordMessageId");
                if (!resultSet.wasNull())
                    voting.setDiscordMessageId(discordMessageId);

                try (PreparedStatement stOptions = Database.getConnection().prepareStatement("SELECT * FROM VotingOptions WHERE votingId = " + voting.getVotingId())) {
                    ResultSet resultSetOptions = stOptions.executeQuery();
                    while (resultSetOptions.next()) {
                        voting.putOption(new VotingOption(resultSetOptions.getLong("id"), resultSetOptions.getString("description")));
                    }
                    resultSetOptions.close();
                } catch (Throwable e) {
                    System.err.println("Error while loading voting options! (" + voting.getVotingId() + ")");
                    e.printStackTrace(System.err);
                    continue;
                }

                try (PreparedStatement stOptions = Database.getConnection().prepareStatement("SELECT * FROM Votes WHERE votingId = " + voting.getVotingId())) {
                    ResultSet resultSetOptions = stOptions.executeQuery();
                    while (resultSetOptions.next()) {
                        voting.getOption(resultSetOptions.getLong("optionId")).addVote(new Vote(guild.getUserFactory().loadUser(resultSetOptions.getLong("userId")), resultSetOptions.getDate("time")));
                    }
                    resultSetOptions.close();
                } catch (Throwable e) {
                    System.err.println("Error while loading votes! (" + voting.getVotingId() + ")");
                    e.printStackTrace(System.err);
                    continue;
                }
                votingCache.put(voting.getVotingId(), voting);
            }
            resultSet.close();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    public void sendVoting(Voting voting, MessageChannelUnion channel) {
        Long channelId = voting.getChannelMessageId();
        Long discordMessageId = voting.getDiscordMessageId();
        if (channelId != null && discordMessageId != null) {
            TextChannel textChannelById = guild.getGuild().getTextChannelById(channelId);
            if (textChannelById != null)
                textChannelById.deleteMessageById(discordMessageId).queue(null, e -> {});
        }
        channel.sendMessageEmbeds(genVotingEmbed(voting).build()).addActionRow(genButtons(voting)).queue(message -> updateMessageId(voting, channel.getIdLong(), message.getIdLong()));
    }

    public void updateVoting(long votingId) {
        Voting voting = loadVoting(votingId);
        if(voting == null || voting.getDiscordMessageId() == null || voting.getChannelMessageId() == null)
            return;
        TextChannel textChannelById = guild.getGuild().getTextChannelById(voting.getChannelMessageId());
        if(textChannelById == null)
            return;
        textChannelById.editMessageById(voting.getDiscordMessageId(),
                new MessageEditBuilder()
                        .setEmbeds(genVotingEmbed(voting).build())
                        .setComponents(ActionRow.of(genButtons(voting)))
                        .build()).queue(null, e -> {});
    }

    private EmbedBuilder genVotingEmbed(Voting voting){
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(voting.getTitle(), voting.generateVotingUrl());
        b.setDescription(voting.getDescription());
        b.setColor(Color.white);
        b.setFooter("Asker - " + voting.getOwner().effName() + " | Module - Voting");

        if (voting.getType().equals(VotingType.ONE_ANSWER) || voting.getType().equals(VotingType.MULTIPLE_ANSWERS)) {
            voting.getOptions().forEach((vOption) ->
                    b.addField(String.format("**%s** (%d)", vOption.getMessage(), vOption.getVoters().size()), vOption.formatVoters(), true));
        }
        return b;
    }

    private List<Button> genButtons(Voting voting){
        List<Button> components = new ArrayList<>();
        switch (voting.getType()) {
            case ONE_ANSWER, MULTIPLE_ANSWERS ->
                    components.add(Button.success("voting_answer_vote", "VOTE").withDisabled(voting.isClosed()));
            case OPEN_ANSWER ->
                    components.add(Button.success("voting_answer_submit", "SUBMIT ANSWER").withDisabled(voting.isClosed()));
        }
        components.add(Button.link(voting.generateVotingUrl(), "WEB"));
        return components;
    }

    public void updateMessageId(Voting voting, long channelId, long messageId) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("UPDATE Voting SET channelMessageId = %s, discordMessageId = %s WHERE id = %s", channelId, messageId, voting.getVotingId()))) {
            if (st.executeUpdate() != 1)
                return;
            voting.setChannelMessageId(channelId);
            voting.setDiscordMessageId(messageId);
            votingCache.put(voting.getVotingId(), voting);
        } catch (Throwable ignored) {}
    }

    public WebhookMessageEditAction<Message> showEditing(Voting voting, Function<MessageEmbed, WebhookMessageEditAction<Message>> o) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(voting.getTitle(), voting.generateVotingUrl());
        b.setDescription(voting.getDescription());
        b.setColor(Color.white);
        b.setFooter("Asker - " + voting.getOwner().effName() + " | Module - Voting");

        boolean tValid = (!voting.getTitle().equalsIgnoreCase("title") && voting.getTitle().length() >= MIN_CONTENT_LENGTH && voting.getTitle().length() <= MAX_TITLE_LENGTH);
        boolean dValid = (!voting.getDescription().equalsIgnoreCase("description") && voting.getDescription().length() >= MIN_CONTENT_LENGTH && voting.getDescription().length() <= MAX_DESCRIPTION_LENGTH);
        boolean oValid;

        String content = "Type: ***" + voting.getType().getMessage() + "***\n\nTitle: " + (tValid ? ":white_check_mark:" : ":x:") +
                "\nDescription: " + (dValid ? ":white_check_mark:" : ":x:");

        List<LayoutComponent> comps = new ArrayList<>();

        comps.add(ActionRow.of(Button.secondary("voting_edit_title", "TITLE"), Button.secondary("voting_edit_description", "DESCRIPTION")));

        if (voting.getType().equals(VotingType.ONE_ANSWER) || voting.getType().equals(VotingType.MULTIPLE_ANSWERS)) {
            oValid = (voting.getOptions().size() >= MIN_OPTION_COUNT && voting.getOptions().size() <= MAX_OPTION_COUNT);
            voting.getOptions().forEach((vOption) ->
                    b.addField(String.format("**%s** (%d)", vOption.getMessage(), 0), vOption.formatVoters(), true));
            comps.add(ActionRow.of(
                    Button.success("voting_edit_add_option", "ADD OPTION").withDisabled(voting.getOptions().size() >= MAX_OPTION_COUNT),
                    Button.danger("voting_edit_remove_option", "REMOVE OPTION").withDisabled(voting.getOptions().isEmpty())));
            content += "\nOptions: " + (oValid ? ":white_check_mark:" : ":x: (" + MIN_OPTION_COUNT + "-" + MAX_OPTION_COUNT + ")");
        } else {
            oValid = true;
        }

        comps.add(ActionRow.of(
                Button.link(voting.generateVotingUrl(), "WEB"),
                Button.primary("voting_edit_publish", "PUBLISH").withDisabled(!(tValid && dValid && oValid)),
                Button.danger("voting_edit_delete", "DELETE")
        ));

        return o.apply(b.build())
                .setContent(content)
                .setComponents(comps);
    }

    @Override
    public @Nullable Voting fromSelfHealingURL(String url) {
        String rawPath = URI.create(url).getRawPath();
        if(rawPath == null)
            return null;
        String[] split = rawPath.replaceAll("(^/)|(/$)", "").split("/");
        if(split.length == 0)
            return null;
        String slugId = split[split.length - 1];
        if(slugId.isBlank())
            return null;
        long id = Long.parseLong(StringUtil.lastPartOfSlug(slugId));
        return loadVoting(id);
    }

    public @Nullable Voting loadVoting(long votingId) {
        Voting voting = votingCache.getOrDefault(votingId, null);
        if (voting != null)
            return voting;
        //TODO LOAD ONLY ONE VOTING
        return null;
    }

    public @NotNull List<Voting> loadAllVotingFromUser(long ownerId) {
        return votingCache.values().stream().filter(a -> a.getOwner().getMemberId() == ownerId).collect(Collectors.toList());
    }

    public @Nullable Voting getUsersEditing(long ownerId) {
        return votingCache.values().stream().filter(a -> a.getOwner().getMemberId() == ownerId && a.isInCreatingMode()).findFirst().orElse(null);
    }

    public Voting newVoting(long ownerId, VotingType type) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("INSERT INTO Voting (guildId, ownerId, votingFlags, votingType, title, description) VALUES (%s, %s, %s, %s, '%s', '%s')",
                guild.getGuild().getIdLong(), ownerId, Voting.FLAG_CREATING, type.getId(), "Title", "Description"), Statement.RETURN_GENERATED_KEYS)) {
            st.executeUpdate();
            ResultSet result = st.getGeneratedKeys();
            if (!result.next()) {
                System.out.println("No result!");
                return null;
            }
            long votingId = result.getLong(1);
            Voting voting = new Voting(type, guild.getUserFactory().loadUser(ownerId), votingId, Voting.FLAG_CREATING, "Title", "Description");
            votingCache.put(votingId, voting);
            return voting;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public boolean deleteVoting(@NotNull Voting voting) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("DELETE FROM Voting WHERE id = %s", voting.getVotingId()))) {
            if (st.executeUpdate() < 1)
                throw new Exception("No rows deleted!");
            votingCache.remove(voting.getVotingId());
            return true;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean publish(@NotNull Voting voting) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("UPDATE Voting SET votingFlags = %s WHERE id = %s", FLAG_DEFAULT, voting.getVotingId()))) {
            if (st.executeUpdate() != 1)
                return false;
            voting.disableCreatingMode();
            votingCache.put(voting.getVotingId(), voting);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean updateTitle(@NotNull Voting voting, @NotNull String title) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("UPDATE Voting SET title = '%s' WHERE id = %s", title, voting.getVotingId()))) {
            if (st.executeUpdate() != 1)
                return false;
            voting.setTitle(title);
            votingCache.put(voting.getVotingId(), voting);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean updateDescription(@NotNull Voting voting, @NotNull String description) {
        try (PreparedStatement st = Database.getConnection().prepareStatement(String.format("UPDATE Voting SET description = '%s' WHERE id = %s", description, voting.getVotingId()))) {
            if (st.executeUpdate() != 1)
                return false;
            voting.setDescription(description);
            votingCache.put(voting.getVotingId(), voting);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean insertOption(@NotNull Voting voting, @NotNull String optionDescription) {
        Connection connection = Database.getConnection();
        try (PreparedStatement st = connection.prepareStatement(String.format("INSERT INTO VotingOptions (votingId,description) VALUES (%s,'%s')", voting.getVotingId(), optionDescription), Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            if (st.executeUpdate() != 1)
                return false;
            connection.commit();
            ResultSet keys = st.getGeneratedKeys();
            if (!keys.next()) throw new Exception("No new key!");
            long optId = keys.getLong(1);
            voting.putOption(new VotingOption(optId, optionDescription));
            votingCache.put(voting.getVotingId(), voting);
            return true;
        } catch (Throwable ignored) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace(System.err);
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public boolean removeOption(@NotNull Voting voting, long optionId) {
        Connection connection = Database.getConnection();
        try (PreparedStatement st = connection.prepareStatement(String.format("DELETE FROM VotingOptions WHERE id = %s", optionId))) {
            if (st.executeUpdate() != 1)
                return false;
            voting.removeOption(optionId);
            votingCache.put(voting.getVotingId(), voting);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean makeVote(long userId, long votingId, long[] votes) {
        Voting voting = loadVoting(votingId);
        if(voting == null)
            return false;
        Connection connection = Database.getConnection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try (PreparedStatement st = connection.prepareStatement("DELETE FROM Votes WHERE userId = " + userId + " AND votingId = " + voting.getVotingId())) {
            st.executeUpdate();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            try {
                connection.rollback();
            } catch (SQLException err) {
                err.printStackTrace(System.err);
            }
            return false;
        }
        try (PreparedStatement st = connection.prepareStatement("INSERT INTO Votes (userId, votingId, optionId) VALUES " +
                Arrays.stream(votes).mapToObj(optId -> "(" + userId + "," + voting.getVotingId() + "," + optId + ")").collect(Collectors.joining(",")))) {
            if (st.executeUpdate() != votes.length)
                return false;
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            try {
                connection.rollback();
            } catch (SQLException err) {
                err.printStackTrace(System.err);
            }
            return false;
        }
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            try {
                connection.rollback();
            } catch (SQLException err) {
                err.printStackTrace(System.err);
            }
            return false;
        }
        voting.getOptions().forEach(option -> option.getVoters().removeIf(a -> a.user().getMemberId() == userId));
        User user = guild.getUserFactory().loadUser(userId);
        for (long vote : votes) {
            voting.getOption(vote).addVote(new Vote(user, Date.valueOf(LocalDate.now())));
        }
        return true;
    }

}