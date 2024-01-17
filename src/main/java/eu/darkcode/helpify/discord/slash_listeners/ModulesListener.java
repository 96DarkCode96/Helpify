package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.annotations.ButtonListener;
import eu.darkcode.helpify.discord.GuildManager;
import eu.darkcode.helpify.discord.annotations.SlashListener;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ModulesListener {

    @ButtonListener(buttonId = "module_request_accept")
    @ButtonListener(buttonId = "module_request_deny")
    public static void moduleRequestAcceptDeny(ButtonInteractionEvent event){
        if(!event.isFromGuild()){
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 1)").setColor(Color.red).build()).queue();
            return;
        }
        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        if(embeds.isEmpty()){
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 2)").setColor(Color.red).build()).queue();
            return;
        }
        MessageEmbed messageEmbed = embeds.get(0);
        Long guildId = messageEmbed.getFields().stream()
                    .filter(a -> Objects.equals(a.getName(), "GuildID"))
                    .map(a -> Long.parseLong(Objects.requireNonNull(a.getValue())))
                    .findFirst().orElse(null);
        if(guildId == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 3)").setColor(Color.red).build()).queue();
            return;
        }

        String guildName = messageEmbed.getFields().stream()
                .filter(a -> Objects.equals(a.getName(), "GuildName")).map(a -> String.valueOf(a.getValue())).findFirst().orElse(null);
        if(guildName == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 4)").setColor(Color.red).build()).queue();
            return;
        }

        Module module = messageEmbed.getFields().stream()
                .filter(a -> Objects.equals(a.getName(), "ModuleID"))
                .map(a -> Module.fromId(Integer.parseInt(Objects.requireNonNull(a.getValue()))))
                .findFirst().orElse(null);
        if(module == null || module.equals(Module.UNKNOWN)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 5)").setColor(Color.red).build()).queue();
            return;
        }

        Long requesterId = messageEmbed.getFields().stream()
                .filter(a -> Objects.equals(a.getName(), "RequesterID"))
                .map(a -> Long.parseLong(Objects.requireNonNull(a.getValue())))
                .findFirst().orElse(null);
        if(requesterId == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 6)").setColor(Color.red).build()).queue();
            return;
        }

        String requesterName = messageEmbed.getFields().stream()
                .filter(a -> Objects.equals(a.getName(), "RequesterName")).map(a -> String.valueOf(a.getValue())).findFirst().orElse(null);
        if(requesterName == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 7)").setColor(Color.red).build()).queue();
            return;
        }

        Long ownerId = messageEmbed.getFields().stream()
                .filter(a -> Objects.equals(a.getName(), "OwnerID"))
                .map(a -> Long.parseLong(Objects.requireNonNull(a.getValue())))
                .findFirst().orElse(null);
        if(ownerId == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 8)").setColor(Color.red).build()).queue();
            return;
        }

        boolean isAccept = event.getComponentId().equals("module_request_accept");

        if (!Database.changeModuleStatus(guildId, module, isAccept ? ModuleStatus.DISABLED : ModuleStatus.BLOCKED)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error (Err. 9)").setColor(Color.red).build()).queue();
            return;
        }

        event.deferEdit().queue();
        event.getHook().editOriginalComponents(ActionRow.of((isAccept ? Button.success("noID", "ACCEPTED") : Button.danger("noID", "DENIED")))
                .asDisabled()).queue();

        if(isAccept){
            if(!requesterId.equals(ownerId)){
                EmbedBuilder ownerMsg = new EmbedBuilder();
                if(messageEmbed.getThumbnail() != null)
                    ownerMsg.setThumbnail(messageEmbed.getThumbnail().getUrl());
                ownerMsg.setTitle("> Request was approved!").setColor(Color.green)
                        .addField("GuildID", String.valueOf(guildId), true)
                        .addField("GuildName", "`" + guildName + "`", true)
                        .addField("---", "", false)
                        .addField("RequesterID", String.valueOf(requesterId), true)
                        .addField("RequesterName", "`" + requesterName + "`", true)
                        .addField("---", "", false)
                        .addField("Approved by", "`" + event.getUser().getName() + "` (" + event.getUser().getId() + ")", true)
                        .addField("Module", module.getMessage(), true);
                event.getJDA().openPrivateChannelById(ownerId).flatMap(channel -> channel.sendMessageEmbeds(ownerMsg.build())).queue(e -> {}, (t) -> {});
            }
            EmbedBuilder requesterMsg = new EmbedBuilder();
            if(messageEmbed.getThumbnail() != null)
                requesterMsg.setThumbnail(messageEmbed.getThumbnail().getUrl());
            requesterMsg.setTitle("> Your request was approved!").setDescription("Now you can enable module using: ```/module enable " + module.getMessage() + "```")
                    .setColor(Color.green)
                    .addField("GuildID", String.valueOf(guildId), true)
                    .addField("GuildName", "`" + guildName + "`", true)
                    .addField("---", "", false)
                    .addField("Approved by", "`" + event.getUser().getName() + "` (" + event.getUser().getId() + ")", true)
                    .addField("Module", module.getMessage(), true);
            event.getJDA().openPrivateChannelById(requesterId).flatMap(channel -> channel.sendMessageEmbeds(requesterMsg.build())).queue(e -> {}, (t) -> {});
            return;
        }
        EmbedBuilder requesterMsg = new EmbedBuilder();
        if(messageEmbed.getThumbnail() != null)
            requesterMsg.setThumbnail(messageEmbed.getThumbnail().getUrl());
        requesterMsg.setTitle("> Your request was denied!")
                .setDescription("Something about you or guild was not all right.\nYou can try it again after unblock wave or contact administrator!")
                .setColor(Color.red)
                .addField("GuildID", String.valueOf(guildId), true)
                .addField("GuildName", "`" + guildName + "`", true)
                .addField("---", "", false)
                .addField("Denied by", "`" + event.getUser().getName() + "` (" + event.getUser().getId() + ")", true)
                .addField("Module", module.getMessage(), true);
        event.getJDA().openPrivateChannelById(requesterId).flatMap(channel -> channel.sendMessageEmbeds(requesterMsg.build())).queue(e -> {}, (t) -> {});
    }

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
                //TODO properly enable each module
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
                //TODO properly disable each module
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
                Member owner = event.getGuild().retrieveOwner().complete();
                if(owner == null){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 5)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                if(!Database.changeModuleStatus(event.getGuild().getIdLong(), module, ModuleStatus.REQUESTED)){
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("> Failed to send request! Try it again later! (Err. 6)").setColor(Color.red)
                            .build()).queue();
                    return;
                }
                channel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle( "Module requested!")
                        .addField("GuildID", String.valueOf(event.getGuild().getIdLong()), true)
                        .addField("GuildName", "`" + event.getGuild().getName() + "`", true)
                        .addField("---", "", false)
                        .addField("ModuleID", String.valueOf(module.getId()), true)
                        .addField("ModuleName", module.getMessage(), true)
                        .addField("---", "", false)
                        .addField("Members", String.valueOf(event.getGuild().retrieveMetaData().complete().getApproximateMembers()), true)
                        .addField("Created", "<t:" + event.getGuild().getTimeCreated().toEpochSecond() + ">", true)
                        .addField("---", "", false)
                        .addField("OwnerID", event.getGuild().getOwnerId(), true)
                        .addField("OwnerName", "`" + owner.getUser().getName() + "`", true)
                        .addField("---", "", false)
                        .addField("RequesterID", event.getUser().getId(), true)
                        .addField("RequesterName", "`" + event.getUser().getName() + "`", true)
                        .setThumbnail(event.getGuild().getIconUrl())
                        .setColor(Color.white)
                        .build()).addActionRow(Button.success("module_request_accept", "ACCEPT"), Button.danger("module_request_deny", "DENY")).queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setDescription("> Successfully sent a request for enabling module *" + module.getMessage() +
                                "*\n> Admin was notified! Please wait, he has life too :heart:")
                        .setColor(Color.green)
                        .build()).queue();
                break;
            }
            default:
                throw new IllegalArgumentException("");
        }
    }

}