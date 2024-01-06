package eu.darkcode.helpify.discord.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Target({ METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SlashListeners {
    SlashListener[] value();
}