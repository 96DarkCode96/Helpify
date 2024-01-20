package eu.darkcode.helpify.controllers;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.DiscordManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
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

    @GetMapping("/guilds/activated")
    public ResponseEntity<Object> guild(@RequestBody() String body) {
        try{
            HashMap<String, Object> result = new HashMap<>();
            Arrays.stream(body.split(";"))
                    .mapToLong(Long::parseLong)
                    .forEach(guildId -> result.put(String.valueOf(guildId), getDataOfGuild(guildId)));
            return ResponseEntity.ok(result);
        }catch(Throwable e){
            return ResponseEntity.internalServerError().build();
        }
    }

    private Object getDataOfGuild(long guildId){
        Guild guild = DiscordManager.getJDA().getGuildById(guildId);
        if(guild == null)
            return null;
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", guild.getIdLong());
        map.put("name", guild.getName());
        map.put("modules", Database.fetchModules(guildId));
        return map;
    }

    @GetMapping("/guild/{guildId}")
    public ResponseEntity<Object> guild(@PathVariable(name = "guildId") long guildId) {
        try{
            return ResponseEntity.ofNullable(getDataOfGuild(guildId));
        }catch(Throwable e){
            return ResponseEntity.internalServerError().build();
        }
    }

}