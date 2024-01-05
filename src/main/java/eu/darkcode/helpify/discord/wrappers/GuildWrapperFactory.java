package eu.darkcode.helpify.discord.wrappers;

import eu.darkcode.helpify.discord.DiscordManager;
import eu.darkcode.helpify.objects.SelfHealingFactory;
import eu.darkcode.helpify.objects.StringUtil;

public class GuildWrapperFactory implements SelfHealingFactory<GuildWrapper> {

    public static final GuildWrapperFactory FACTORY = new GuildWrapperFactory();

    @Override
    public GuildWrapper fromSelfHealingURL(String URL) {
        return new GuildWrapper(DiscordManager.getJDA().getGuildById(StringUtil.lastPartOfSlug(URL)));
    }
}
