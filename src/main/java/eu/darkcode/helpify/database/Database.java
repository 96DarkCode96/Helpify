package eu.darkcode.helpify.database;

import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Database {

    private static Connection connection;

    public static void init(){
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mariadb://192.168.1.46:9003/helpify", "helpify", "Helpify12#13#45#awd#127389");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }

        createTable("Modules", "guildId bigint(25) not null, moduleId int(11) not null, value tinyint(8) not null, primary key (guildId, moduleId)");
        createTable("Voting", "id bigint(25) not null auto_increment primary key, guildId bigint(25) not null, ownerId bigint(25) not null, votingType int(11) not null, votingFlags int(11) not null, title varchar(128) not null, description varchar(255) not null, channelMessageId bigint(25), discordMessageId bigint(25)");
        createTable("VotingOptions", "id bigint(25) not null auto_increment primary key, votingId bigint(25) not null, description varchar(64) not null, foreign key (votingId) references Voting(id) ON DELETE CASCADE");
        createTable("Votes", "userId bigint(25) not null, votingId bigint(25) not null, optionId bigint(25) not null, time timestamp not null default current_timestamp(), primary key (userId, votingId, optionId), foreign key (votingId) references Voting(id) ON DELETE CASCADE, foreign key (optionId) references VotingOptions(id) ON DELETE CASCADE");
        createTable("Settings", "`key` varchar(255) not null primary key, `value` LONGTEXT not null");

        checkSetting("admin.guild", (key) -> System.out.println("[DATABASE] Key '" + key + "' is not set!"));
        checkSetting("admin.channel.moduleRequest", (key) -> System.out.println("[DATABASE] Key '" + key + "' is not set!"));

    }

    public static void checkSetting(String key, Consumer<String> failHandler){
        if (getSetting(key, null, (a) -> a) == null) failHandler.accept(key);
    }

    public static <T> T getSetting(String key, @Nullable T def, Function<String, T> parse){
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("SELECT `value` FROM Settings WHERE `key` = ?")){
            preparedStatement.setString(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(!resultSet.next())
                return def;
            T value = parse.apply(resultSet.getString("value"));
            resultSet.close();
            return value;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean setSetting(String key, @NotNull Object value){
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("INSERT INTO Settings VALUES (?,?) ON DUPLICATE KEY UPDATE `value` = ?")){
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, value.toString());
            preparedStatement.setString(3, value.toString());
            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createTable(String name, String params) {
        String sql = "CREATE TABLE IF NOT EXISTS " + name + "(" + params + ");";
        try(PreparedStatement preparedStatement = getConnection().prepareStatement(sql)){
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        if(connection == null)
            throw new NullPointerException("Database is not initialized!");
        return connection;
    }

    public static HashMap<Module, ModuleStatus> fetchModules(long guildId) {
        HashMap<Module, ModuleStatus> modules = Arrays.stream(Module.values())
                .collect(Collectors.toMap(module -> module, module -> ModuleStatus.WAITING, (prev, next) -> next, HashMap::new));
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("SELECT moduleId, value FROM Modules WHERE guildId = " + guildId)){
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                modules.put(Module.fromId(resultSet.getInt("moduleId")), ModuleStatus.fromId(resultSet.getInt("value")));
            }
            resultSet.close();
            modules.remove(Module.UNKNOWN);
            return modules;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isModuleStatus(long guildId, Module module, ModuleStatus status) {
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("SELECT value FROM Modules WHERE guildId = " + guildId + " AND moduleId = " + module.getId() + " AND value = " + status.getId())){
            return preparedStatement.executeQuery().first();
        } catch (SQLException e) {
            return false;
        }
    }

    public static ModuleStatus getModuleStatus(long guildId, Module module) {
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("SELECT value FROM Modules WHERE guildId = " + guildId + " AND moduleId = " + module.getId())){
            ResultSet resultSet = preparedStatement.executeQuery();
            if(!resultSet.next()){
                return ModuleStatus.WAITING;
            }
            ModuleStatus status = ModuleStatus.fromId(resultSet.getInt("value"));
            resultSet.close();
            return status;
        } catch (SQLException e) {
            return ModuleStatus.UNKNOWN;
        }
    }

    public static boolean changeModuleStatus(long guildId, Module module, ModuleStatus moduleStatus) {
        try(PreparedStatement preparedStatement = getConnection().prepareStatement("INSERT INTO Modules VALUES(" + guildId + "," + module.getId() + "," + moduleStatus.getId() + ") ON DUPLICATE KEY UPDATE value = " + moduleStatus.getId())){
            return preparedStatement.executeUpdate() != 0;
        } catch (SQLException e) {
            return false;
        }
    }
}