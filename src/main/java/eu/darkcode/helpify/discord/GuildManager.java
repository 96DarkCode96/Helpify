package eu.darkcode.helpify.discord;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import eu.darkcode.helpify.discord.voting.VotingType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuildManager {

    public static List<SlashCommandData> generateCommands(long guildId) {
        List<SlashCommandData> list = new ArrayList<>();
        //  /modules
        list.add(Commands.slash("modules", "Shows statuses of all modules!")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        list.add(Commands.slash("module", "Command used for managing modules")
                        .addOptions(
                                new OptionData(OptionType.STRING, "action", "Action to perform with the module")
                                        .addChoice("status of", "status")
                                        .addChoice("enable", "enable")
                                        .addChoice("disable", "disable")
                                        .addChoice("request for", "request")
                                        .setRequired(true),
                                new OptionData(OptionType.STRING, "module", "Module to perform action at")
                                        .addChoices(Module.useable().stream().map(a -> new Command.Choice(a.getMessage(), a.getId())).collect(Collectors.toList()))
                                        .setRequired(true)
                        )
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        if(Database.isModuleStatus(guildId, Module.VOTING, ModuleStatus.ENABLED)) {
            list.add(Commands.slash("voting", "Create voting!")
                    .addOptions(new OptionData(OptionType.STRING, "type", "Voting type")
                            .addChoices(VotingType.useable().stream().map(a -> new Command.Choice(a.getMessage(), a.getId())).collect(Collectors.toList()))
                            .setRequired(true))
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)));
            list.add(Commands.slash("resend-voting", "Resend voting message!")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)));
        }

        return list;
    }
}