package eu.darkcode.helpify.controllers;

import eu.darkcode.helpify.discord.DiscordManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

@RestController()
@RequestMapping("/@me")
public class MeController {

    @GetMapping("")
    public ResponseEntity<HashMap<String, Object>> me() {
        try{
            HashMap<String, Object> map = new HashMap<>();
            SelfUser user = DiscordManager.getJDA().getSelfUser();
            map.put("name", user.getName());
            map.put("id", user.getId());
            map.put("avatarUrl", user.getEffectiveAvatarUrl());
            return ResponseEntity.ok(map);
        }catch(Throwable ignored){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/guilds")
    public ResponseEntity<List<HashMap<String, Object>>> guilds() {
        try{
            return ResponseEntity.ok(DiscordManager.getJDA().getSelfUser().getMutualGuilds().stream().map((guild) ->{
                HashMap<String, Object> map = new HashMap<>();
                map.put("id", guild.getId());
                map.put("icon", guild.getIconUrl());
                map.put("name", guild.getName());
                map.put("description", guild.getDescription());
                map.put("memberCount", guild.getMemberCount());
                map.put("online", guild.retrieveMetaData().map(Guild.MetaData::getApproximatePresences).complete());
                return map;
            }).toList());
        }catch(Throwable ignored){
            return ResponseEntity.internalServerError().build();
        }
    }

}