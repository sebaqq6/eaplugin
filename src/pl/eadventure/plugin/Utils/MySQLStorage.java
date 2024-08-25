package pl.eadventure.plugin.Utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MySQLStorage {
    private Connection link;
    private Statement statement;
    private Boolean isConnect;

    public MySQLStorage(String hostname, int port, String database, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            link = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database + "?verifyServerCertificate=false&useSSL=false&useUnicode=true&characterEncoding=utf8", username, password);

            statement = link.createStatement();
            statement.execute("SET NAMES 'utf8'");
            statement.execute("SET CHARACTER SET utf8");
            statement.execute("SET CHARACTER_SET_CONNECTION=utf8");
            statement.execute("SET SQL_MODE = ''");

            isConnect = true;
        } catch (ClassNotFoundException | SQLException e) {
            isConnect = false;
        }
    }

    public Boolean isConnect() {
        return this.isConnect;
    }

    public void close() {
        try {
            link.close();
            isConnect = false;
        } catch (SQLException e) {
            isConnect = false;
        }
    }

    public void query(String sql, QueryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
            	try (Statement stmt = link.createStatement()) {
                    ResultSet result = stmt.executeQuery(sql);

                    int i = 0;
                    ArrayList<HashMap<Object, Object>> data = new ArrayList<>();

                    while (result.next()) {
                        HashMap<Object, Object> row = new HashMap<>();
                        for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
                            row.put(result.getMetaData().getColumnName(index), result.getObject(result.getMetaData().getColumnName(index)));
                        }

                        data.add(i, row);
                        i++;
                    }

                    HashMap<Object, Object> queryResult = new HashMap<>();
                    queryResult.put("row", (data.size() > 0 ? data.get(0) : new ArrayList<>()));
                    queryResult.put("rows", data);
                    queryResult.put("num_rows", i);

                    // Call the callback method on the main server thread
                    Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(queryResult));
                } catch (SQLException e) {
                    // Call the callback method on the main server thread with null query result
                    Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(null));
                    print.error(e.getMessage());
                }
            }
        }.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
    }

    public void querySafe(String sql, ArrayList<Object> parameters, QueryCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = link.prepareStatement(sql)) {
                    // Set parameters for the prepared statement
                    for (int i = 0; i < parameters.size(); i++) {
                        stmt.setObject(i + 1, parameters.get(i));
                    }

                    ResultSet result = stmt.executeQuery();

                    int i = 0;
                    ArrayList<HashMap<Object, Object>> data = new ArrayList<>();

                    while (result.next()) {
                        HashMap<Object, Object> row = new HashMap<>();
                        for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
                            row.put(result.getMetaData().getColumnName(index), result.getObject(result.getMetaData().getColumnName(index)));
                        }

                        data.add(i, row);
                        i++;
                    }

                    HashMap<Object, Object> queryResult = new HashMap<>();
                    queryResult.put("row", (data.size() > 0 ? data.get(0) : new ArrayList<>()));
                    queryResult.put("rows", data);
                    queryResult.put("num_rows", i);

                    // Call the callback method on the main server thread
                    Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(queryResult));
                } catch (SQLException e) {
                    // Call the callback method on the main server thread with null query result
                    Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(null));
                    print.error(e.getMessage());
                }
            }
        }.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
    }


    public void execute(String sql) {
        try (PreparedStatement stmt = link.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void executeSafe(String sql, ArrayList<Object> parameters) {
        try (PreparedStatement stmt = link.prepareStatement(sql)) {
            // Set parameters for the prepared statement
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            // Wykonanie zapytania
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int executeGetInsertID(String sql, ArrayList<Object> parameters) {
        int insertid = 0;
        try (PreparedStatement stmt = link.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Set parameters for the prepared statement
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            stmt.executeUpdate();
            ResultSet result = stmt.getGeneratedKeys();
            if (result.next()) {
                insertid = result.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            insertid = 0;
        }
        return insertid;
    }

    public Object escape(Object value) {
        String string = String.valueOf(value);

        if (string == null) {
            return null;
        }

        if (string.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").length() < 1) {
            return string;
        }

        String clean_string = string;
        clean_string = clean_string.replaceAll("\\\\", "\\\\\\\\");
        clean_string = clean_string.replaceAll("\\n", "\\\\n");
        clean_string = clean_string.replaceAll("\\r", "\\\\r");
        clean_string = clean_string.replaceAll("\\t", "\\\\t");
        clean_string = clean_string.replaceAll("\\00", "\\\\0");
        clean_string = clean_string.replaceAll("'", "\\\\'");
        clean_string = clean_string.replaceAll("\\\"", "\\\\\"");

        return clean_string;
    }

    public static String getDebugSQL(String sql, ArrayList<Object> parameters) {
        StringBuilder debugSQL = new StringBuilder(sql);

        // Wstawienie wartości parametrów do zapytania SQL
        for (Object param : parameters) {
            String valueStr = (param != null) ? param.toString() : "null";
            int index = debugSQL.indexOf("?");
            if (index != -1) {
                debugSQL.replace(index, index + 1, valueStr);
            }
        }

        return debugSQL.toString();
    }



    public interface QueryCallback {
        void onQueryComplete(HashMap<Object, Object> queryResult);
    }
}