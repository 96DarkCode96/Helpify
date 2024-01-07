package eu.darkcode.helpify.discord.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;

@Repeatable(ButtonListeners.class)
@Target({ METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ButtonListener {
    String buttonId() default "";
}