package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.GuildManager;
import eu.darkcode.helpify.discord.SlashListener;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;

public class ModulesListener {

    @SlashListener(command = "modules")
    public static void listenModules(SlashCommandInteractionEvent event){
        if(!event.isFromGuild())
            return;
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Modules");
        b.setColor(Color.white);
        assert event.getGuild() != null;
        HashMap<Module, ModuleStatus> modules = Database.fetchModules(event.getGuild().getIdLong());
        modules.forEach((module, moduleStatus) -> b.addField(module.getMessage(), "> " + moduleStatus.getMessage(), false));
        event.replyEmbeds(b.build())
                .setEphemeral(true)
                .setActionRow(Button.link("https://helpify.matejtomecek.eu/dashboard/", "DASHBOARD"))
                .queue();
    }

    @SlashListener(command = "module")
    public static void listenModule(SlashCommandInteractionEvent event){
        if(!event.isFromGuild())
            return;
        Guild guild = event.getGuild();
        assert guild != null;
        String action = event.getOption("action", () -> null, OptionMapping::getAsString);
        Module module = event.getOption("module", () -> null, (o) -> Module.fromId(o.getAsInt()));
        if(action == null){
            event.replyEmbeds(new EmbedBuilder().setDescription("> Unknown action!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        if(module == null || module.equals(Module.UNKNOWN)){
            event.replyEmbeds(new EmbedBuilder().setDescription("> Unknown module!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        ModuleStatus status = Database.getModuleStatus(event.getGuild().getIdLong(), module);
        if(status.equals(ModuleStatus.UNKNOWN)){
            event.replyEmbeds(new EmbedBuilder().setDescription("> Something went wrong! Try it again later!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        switch (action) {
            case "status": {
                event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Module - " + module.getMessage())
                        .setDescription("> " + status.getMessage())
                        .setColor(Color.green)
                        .build()).setEphemeral(true).queue();
                break;
            }
            case "enable": {
                if(status.equals(ModuleStatus.ENABLED)){
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Module is already enabled!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                if(!status.equals(ModuleStatus.DISABLED)) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> You are not allowed to use this module!\n> You can find more info in *status* of this module!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                if(!Database.changeModuleStatus(event.getGuild().getIdLong(), module, ModuleStatus.ENABLED)){
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to enable! Try it again later!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("> Module *" + module.getMessage() + "* was successfully enabled!")
                        .setColor(Color.green)
                        .build()).setEphemeral(true).queue();
                guild.updateCommands().addCommands(GuildManager.generateCommands(guild.getIdLong())).queue();
                //TODO
                break;
            }
            case "disable": {
                if(status.equals(ModuleStatus.DISABLED)){
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Module is already disabled!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                if(!status.equals(ModuleStatus.ENABLED)) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> You are not allowed to use this module!\n> You can find more info in *status* of this module!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                if(!Database.changeModuleStatus(event.getGuild().getIdLong(), module, ModuleStatus.DISABLED)){
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to disable! Try it again later!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("> Module *" + module.getMessage() + "* was successfully disabled!")
                        .setColor(Color.gray)
                        .build()).setEphemeral(true).queue();
                guild.updateCommands().addCommands(GuildManager.generateCommands(guild.getIdLong())).queue();
                //TODO
                break;
            }
            case "request": {
                if(status.equals(ModuleStatus.BLOCKED)){
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Your request was already denied! You can contact us via website if you want to change that!")
                            .setColor(Color.red)
                            .build())
                            .addActionRow(Button.link("https://helpify.matejtomecek.eu/dashboard/contact/", "CONTACT")).setEphemeral(true).queue();
                    return;
                }
                if(status.equals(ModuleStatus.REQUESTED)){
                    event.replyEmbeds(new EmbedBuilder()
                                    .setDescription("> You have already sent a request for enabling this module! Please wait!")
                                    .setColor(Color.red)
                                    .build()).setEphemeral(true).queue();
                    return;
                }
                if(!status.equals(ModuleStatus.WAITING)) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setDescription("> Your module has already been requested and accepted! You can *enable* it!")
                            .setColor(Color.red)
                            .build()).setEphemeral(true).queue();
                    return;
                }
                event.deferReply(true).queue();
                Long guildID = Database.getSetting("admin.guild", null, Long::parseLong);
                if(guildID == null){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 1)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                Long channelID = Database.getSetting("admin.channel.moduleRequest", null, Long::parseLong);
                if(channelID == null){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 2)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                Guild adminGuild = event.getJDA().getGuildById(guildID);
                if(adminGuild == null){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 3)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                TextChannel channel = adminGuild.getTextChannelById(channelID);
                if(channel == null || !channel.canTalk(adminGuild.getSelfMember())){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 4)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                if(!Database.changeModuleStatus(event.getGuild().getIdLong(), module, ModuleStatus.REQUESTED)){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 5)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                Member owner = event.getGuild().getOwner();
                if(owner == null){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 6)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                channel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle( "Module '"+module.getMessage()+"' requested!")
                        .addField("GuildID", String.valueOf(event.getGuild().getIdLong()), true)
                        .addField("GuildName", event.getGuild().getName(), true)
                        .addField("---", "", false)
                        .addField("Members", String.valueOf(event.getGuild().retrieveMetaData().complete().getApproximateMembers()), true)
                        .addField("Created", "<t:" + event.getGuild().getTimeCreated().toEpochSecond() + ">", true)
                        .addField("---", "", false)
                        .addField("OwnerID", event.getGuild().getOwnerId(), true)
                        .addField("OwnerName", owner.getEffectiveName(), true)
                        .addField("---", "", false)
                        .addField("RequesterID", event.getUser().getId(), true)
                        .addField("RequesterName",event.getUser().getEffectiveName(), true)
                        .setThumbnail(event.getGuild().getIconUrl())
                        .setColor(Color.white)
                        .build()).queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setDescription("> Successfully sent a request for enabling module *" + module.getMessage() + "*\n> Admin was notified! Please wait, he has life too :heart:")
                        .setColor(Color.green)
                        .build()).queue();
                break;
            }
            default:
                throw new IllegalArgumentException("");
        }
    }

}