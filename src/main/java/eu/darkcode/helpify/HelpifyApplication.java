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
		if(args.length != 4){
			System.out.println("java -jar helpify.jar <dbUrl> <dbUser> <dbPassword> <botToken>");
			System.exit(-1);
		}
		SpringApplication.run(HelpifyApplication.class, args);
		Database.init(args[0], args[1], args[2]);
		DiscordManager.init(args[3]);
	}

}
