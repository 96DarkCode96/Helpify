package eu.darkcode.helpify.discord.wrappers;

import eu.darkcode.helpify.discord.voting.VotingManager;
import eu.darkcode.helpify.objects.SelfHealingURL;
import eu.darkcode.helpify.objects.StringUtil;
import net.dv8tion.jda.api.entities.Guild;

public class GuildWrapper implements SelfHealingURL {

    private final Guild guild;
    private final VotingManager votingManager;
    private final UserFactory userFactory;

    public GuildWrapper(Guild guild) {
        this.guild = guild;
        this.userFactory = new UserFactory(this);
        this.votingManager = new VotingManager(this);
    }

    public UserFactory getUserFactory() {
        return userFactory;
    }

    public VotingManager getVotingManager() {
        return votingManager;
    }

    public Guild getGuild() {
        return guild;
    }

    @Override
    public String generateSelfHealingURL() {
        return StringUtil.slug(guild.getName()) + "-" + guild.getIdLong();
    }
}