package eu.darkcode.helpify.discord.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Repeatable(ModalListeners.class)
@Target({ METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModalListener {
    String modalId() default "";
}