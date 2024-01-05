package eu.darkcode.helpify.discord.voting;

import eu.darkcode.helpify.discord.wrappers.User;

import java.sql.Date;

public record Vote(User user, Date time) {}