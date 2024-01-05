package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.ButtonListener;
import eu.darkcode.helpify.discord.ModalListener;
import eu.darkcode.helpify.discord.SlashListener;
import eu.darkcode.helpify.discord.StringSelectListener;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import eu.darkcode.helpify.discord.voting.Voting;
import eu.darkcode.helpify.discord.voting.VotingManager;
import eu.darkcode.helpify.discord.voting.VotingType;
import eu.darkcode.helpify.discord.wrappers.GuildWrapper;
import eu.darkcode.helpify.objects.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

import static eu.darkcode.helpify.discord.voting.Voting.*;


public class VotingListener {

    @SlashListener(command = "resend-voting")
    public static void listenResend(SlashCommandInteractionEvent event){
        event.deferReply(true).queue();

        if (!event.isFromGuild())
            return;
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        event.getHook().editOriginalComponents(ActionRow.of(StringSelectMenu.create("resend-voting-menu")
                .addOptions(guild.getVotingManager().getVotingCache().values().stream()
                        .map(a -> SelectOption.of(StringUtil.limit(a.getTitle(), 90), String.valueOf(a.getVotingId())))
                        .toList()).setMaxValues(1).setMinValues(1).build())).setContent("Choose voting to be resent!").queue();
    }

    @StringSelectListener(componentId = "resend-voting-menu")
    public static void resendListener(StringSelectInteractionEvent event){
        event.deferEdit().queue();

        if (!event.isFromGuild()) {
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)){
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }
        event.getValues().stream().mapToLong(Long::parseLong).mapToObj(id -> guild.getVotingManager().loadVoting(id)).filter(Objects::nonNull).forEach(value ->
                guild.getVotingManager().sendVoting(value, event.getChannel()));
        event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Successfully resent!").setColor(Color.green).build())
                .setContent("").setComponents(List.of()).queue();
    }

    @SlashListener(command = "voting")
    public static void listen(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (!event.isFromGuild())
            return;
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());

        if (prevVoting != null) {
            guild.getVotingManager().showEditing(prevVoting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
            return;
        }

        VotingType type = event.getOption("type", () -> null, (o) -> VotingType.fromId(o.getAsInt()));

        if(!type.equals(VotingType.ONE_ANSWER)){
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("> This voting type is disabled! Development in progress!").build()).queue();
            return;
        }

        Voting voting = guild.getVotingManager().newVoting(event.getUser().getIdLong(), type);
        if (voting == null) {
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("> Internal Error!").build()).queue();
            return;
        }
        guild.getVotingManager().showEditing(voting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
    }

    @ButtonListener(buttonId = "voting_answer_vote")
    public static void buttonVote(ButtonInteractionEvent event){
        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        Button web = event.getMessage().getButtonsByLabel("WEB", false).stream().findFirst().orElse(null);
        if(web == null){
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 3)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        String url = web.getUrl();
        if(url == null){
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 4)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        Voting voting = guild.getVotingManager().fromSelfHealingURL(url);
        if(voting == null){
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 5)").setColor(Color.red).build()).queue();
            return;
        }
        if (voting.getType().equals(VotingType.ONE_ANSWER)) {
            event.getHook().editOriginalComponents(ActionRow.of(StringSelectMenu.create("voting_menu_vote_option")
                    .addOptions(voting.getOptions().stream()
                            .map(a -> SelectOption.of(a.getMessage(), a.getId() + "|" + voting.getVotingId()))
                            .toList()).setMaxValues(1).setMinValues(1).build())).setContent("Make your vote").setEmbeds(new ArrayList<>()).queue();
            return;
        }else if (voting.getType().equals(VotingType.MULTIPLE_ANSWERS)) {
            event.getHook().editOriginalComponents(ActionRow.of(StringSelectMenu.create("voting_menu_vote_option")
                    .addOptions(voting.getOptions().stream()
                            .map(a -> SelectOption.of(a.getMessage(), a.getId() + "|" + voting.getVotingId()))
                            .toList()).setMaxValues(voting.getOptions().size()).build())).setContent("Make your vote(s)").setEmbeds(new ArrayList<>()).queue();
            return;
        }
        event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 6)").setColor(Color.red).build()).queue();
    }

    @ButtonListener(buttonId = "voting_edit_title")
    public static void buttonTitle(ButtonInteractionEvent event) {
        Modal.Builder modal = Modal.create("voting_modal_edit_title", "Editing 'Title'");
        modal.addActionRow(TextInput.create("title", "Title", TextInputStyle.SHORT)
                .setPlaceholder("Some magic title")
                .setRequired(true)
                .setRequiredRange(MIN_CONTENT_LENGTH, MAX_TITLE_LENGTH)
                .build());
        event.replyModal(modal.build()).queue();
    }

    @ButtonListener(buttonId = "voting_edit_description")
    public static void buttonDescription(ButtonInteractionEvent event) {
        Modal.Builder modal = Modal.create("voting_modal_edit_description", "Editing 'Description'");
        modal.addActionRow(TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Some magic description")
                .setRequired(true)
                .setRequiredRange(MIN_CONTENT_LENGTH, MAX_DESCRIPTION_LENGTH)
                .build());
        event.replyModal(modal.build()).queue();
    }

    @ButtonListener(buttonId = "voting_edit_add_option")
    public static void buttonAddOption(ButtonInteractionEvent event) {
        Modal.Builder modal = Modal.create("voting_modal_edit_add_option", "Editing 'Add option'");
        modal.addActionRow(TextInput.create("title", "Option title", TextInputStyle.SHORT)
                .setPlaceholder("Some magic response")
                .setRequired(true)
                .setRequiredRange(MIN_OPTION_LENGTH, MAX_OPTION_LENGTH)
                .build());
        event.replyModal(modal.build()).queue();
    }

    @ButtonListener(buttonId = "voting_edit_remove_option")
    public static void buttonRemoveOption(ButtonInteractionEvent event) {
        event.deferEdit().queue();

        if (!event.isFromGuild())
            return;
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());
        if (prevVoting == null)
            return;

        event.getHook().editOriginalComponents(ActionRow.of(StringSelectMenu.create("voting_menu_remove_option")
                .addOptions(prevVoting.getOptions().stream()
                        .map(a -> SelectOption.of(a.getMessage(), String.valueOf(a.getId())))
                        .toList()).setMaxValues(1).setMinValues(1).build())).setContent("Remove option").setEmbeds(new ArrayList<>()).queue();
    }

    @ButtonListener(buttonId = "voting_edit_publish")
    public static void buttonPublish(ButtonInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());
        if (prevVoting == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 3)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        if (!guild.getVotingManager().publish(prevVoting)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 4)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }

        event.editMessage("").setEmbeds(new EmbedBuilder().setColor(Color.green).setTitle("> Successfully published! We'll do the rest for you!").build()).setComponents(new ArrayList<>()).queue();

        guild.getVotingManager().sendVoting(prevVoting, event.getChannel());
    }

    @ButtonListener(buttonId = "voting_edit_delete")
    public static void buttonDelete(ButtonInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());
        if (prevVoting == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 3)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        if (!guild.getVotingManager().deleteVoting(prevVoting)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 4)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        event.editMessage("").setEmbeds(new EmbedBuilder().setColor(Color.green).setTitle("> Successfully removed!").build()).setComponents(new ArrayList<>()).queue();
    }

    @ModalListener(modalId = "voting_modal_edit_title")
    public static void modalTitle(ModalInteractionEvent event) {
        ModalMapping titleMapping = event.getValue("title");
        if (titleMapping == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        String title = titleMapping.getAsString();
        if (!title.matches(STRING_REGEX + "{" + MIN_CONTENT_LENGTH + "," + MAX_TITLE_LENGTH + "}")) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Invalid character(s) !").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 3)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());
        if (prevVoting == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 4)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        if (!guild.getVotingManager().updateTitle(prevVoting, title)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 5)").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue();
        guild.getVotingManager().showEditing(prevVoting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
    }

    @ModalListener(modalId = "voting_modal_edit_description")
    public static void modalDescription(ModalInteractionEvent event) {
        ModalMapping descriptionMapping = event.getValue("description");
        if (descriptionMapping == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        String description = descriptionMapping.getAsString();
        if (!description.matches(STRING_REGEX + "{" + MIN_CONTENT_LENGTH + "," + MAX_DESCRIPTION_LENGTH + "}")) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Invalid character(s) !").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }

        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());

        if (prevVoting == null)
            return;

        if (!guild.getVotingManager().updateDescription(prevVoting, description)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue();
        guild.getVotingManager().showEditing(prevVoting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
    }

    @ModalListener(modalId = "voting_modal_edit_add_option")
    public static void modalAddOption(ModalInteractionEvent event) {
        ModalMapping descriptionMapping = event.getValue("title");
        if (descriptionMapping == null) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        String description = descriptionMapping.getAsString();
        if (!description.matches(STRING_REGEX + "{" + MIN_OPTION_LENGTH + "," + MAX_OPTION_LENGTH + "}")) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Invalid character(s) !").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }

        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());

        if (prevVoting == null)
            return;

        if (!guild.getVotingManager().insertOption(prevVoting, description)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue();
        guild.getVotingManager().showEditing(prevVoting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
    }

    @StringSelectListener(componentId = "voting_menu_remove_option")
    public static void selectRemove(StringSelectInteractionEvent event) {
        List<String> values = event.getValues();
        if (values.size() != 1) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        long id = Integer.parseInt(values.get(0));

        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED))
            return;

        Voting prevVoting = guild.getVotingManager().getUsersEditing(event.getUser().getIdLong());

        if (prevVoting == null)
            return;

        if (!guild.getVotingManager().removeOption(prevVoting, id)) {
            event.replyEmbeds(new EmbedBuilder().setTitle("> Internal error!").setColor(Color.red).build()).setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue();
        guild.getVotingManager().showEditing(prevVoting, (e) -> event.getHook().editOriginalEmbeds(e)).queue();
    }

    @StringSelectListener(componentId = "voting_menu_vote_option")
    public static void makeVote(StringSelectInteractionEvent event){
        event.deferEdit().queue();

        if (!event.isFromGuild()) {
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 1)").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }
        GuildWrapper guild = new GuildWrapper(event.getGuild());
        assert guild.getGuild() != null;
        if (!Database.isModuleStatus(guild.getGuild().getIdLong(), Module.VOTING, ModuleStatus.ENABLED)){
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 2)").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }

        List<String> values = event.getValues();
        if(values.isEmpty()){
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Internal error! (Err. 3)").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }

        long votingId = Long.parseLong(values.stream().findFirst().get().split("\\|")[1]);
        long[] votes = values.stream().mapToLong(a -> Long.parseLong(a.split("\\|")[0])).toArray();

        if (!guild.getVotingManager().makeVote(event.getUser().getIdLong(), votingId, votes)) {
            event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Something went wrong! Try it again later!").setColor(Color.red).build())
                    .setContent("")
                    .setComponents(List.of()).queue();
            return;
        }

        guild.getVotingManager().updateVoting(votingId);

        event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("> Successfully voted!").setColor(Color.green).build())
                .setContent("")
                .setComponents(List.of()).queue();
    }

}