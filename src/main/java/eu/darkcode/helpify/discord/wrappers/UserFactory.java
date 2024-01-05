package eu.darkcode.helpify.discord.wrappers;

import eu.darkcode.helpify.objects.SelfHealingFactory;
import eu.darkcode.helpify.objects.StringUtil;

public record UserFactory(GuildWrapper guild) implements SelfHealingFactory<User> {

    @Override
    public User fromSelfHealingURL(String URL) {
        return loadUser(Long.parseLong(StringUtil.lastPartOfSlug(URL)));
    }

    public User loadUser(long userId) {
        return new User(guild, userId); // TODO , realName
    }
}
