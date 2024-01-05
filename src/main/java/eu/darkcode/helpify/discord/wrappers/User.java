package eu.darkcode.helpify.discord.wrappers;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import eu.darkcode.helpify.objects.SelfHealingURL;
import eu.darkcode.helpify.objects.StringUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class User implements SelfHealingURL {

    private final GuildWrapper guild;
    private final long memberId;
    private @Nullable String realName; // REAL NAME MODULE

    public User(GuildWrapper guild, long memberId) {
        this.guild = guild;
        this.memberId = memberId;
    }

    public User(Guild guild, long memberId, @Nullable String realName) {
        this.guild = new GuildWrapper(guild);
        this.memberId = memberId;
        this.realName = realName;
    }

    public void setRealName(@Nullable String realName) {
        this.realName = realName;
    }

    public long getMemberId() {
        return memberId;
    }

    public GuildWrapper getGuildWrapper() {
        return guild;
    }

    public Guild getGuild() {
        return guild.getGuild();
    }

    @Nullable
    public String getRealName() {
        return realName;
    }

    public @NotNull String effName() {
        if (Database.isModuleStatus(guild.getGuild().getIdLong(), Module.REAL_NAMES, ModuleStatus.ENABLED) && realName != null)
            return realName;
        Member member = guild.getGuild().getMemberById(getMemberId());
        return member == null ? "Unknown-User-" + getMemberId() : member.getEffectiveName();
    }

    @Override
    public String generateSelfHealingURL() {
        return StringUtil.slug(effName()) + "-" + getMemberId();
    }
}