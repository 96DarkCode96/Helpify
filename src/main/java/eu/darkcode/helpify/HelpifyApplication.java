package eu.darkcode.helpify;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.DiscordManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HelpifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpifyApplication.class, args);
		Database.init();
		DiscordManager.init();
	}

}
