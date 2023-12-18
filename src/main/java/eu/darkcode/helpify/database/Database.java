package eu.darkcode.helpify.database;

import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;

import java.sql.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    }

    private static void createTable(String name, String params) {
        try {
            getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + name + "(" + params + ");").executeUpdate();
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
        try{
            HashMap<Module, ModuleStatus> modules = Arrays.stream(Module.values())
                    .collect(Collectors.toMap(module -> module, module -> ModuleStatus.WAITING, (prev, next) -> next, HashMap::new));
            ResultSet resultSet = getConnection().prepareStatement("SELECT moduleId, value FROM Modules WHERE guildId = " + guildId).executeQuery();
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
}