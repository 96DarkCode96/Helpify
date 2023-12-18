package eu.darkcode.helpify;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.DiscordManager;
import eu.darkcode.helpify.discord.SlashListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

@SpringBootApplication
@RestController
public class HelpifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpifyApplication.class, args);
		Database.init();
		DiscordManager.init();
	}

}
