package pl.eadventure.plugin.Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MySQLStorage {

	private HikariDataSource dataSource;
	private Boolean isConnect;
	private String tag;

	public MySQLStorage(String hostname, int port, String database, String username, String password, String tag) {
		this.tag = tag;
		open(hostname, port, database, username, password);
	}

	public Boolean isConnect() {
		return this.isConnect;
	}

	public void open(String hostname, int port, String database, String username, String password) {
		try {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8&useSSL=false");
			config.setUsername(username);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");

			// Opcjonalne ustawienia
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(2);
			config.setIdleTimeout(60000);
			config.setMaxLifetime(1800000);
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			config.setConnectionInitSql(
					"SET NAMES 'utf8' COLLATE 'utf8_general_ci', " +
							"CHARACTER SET utf8, " +
							"CHARACTER_SET_CONNECTION=utf8, " +
							"SQL_MODE=''"
			);


			dataSource = new HikariDataSource(config);
			this.isConnect = true;
			print.ok("Połączono z bazą danych MySQL przez HikariCP! - TAG: " + tag);

		} catch (Exception e) {
			this.isConnect = false;
			print.error("Błąd połączenia z MySQL! - TAG: " + tag);
			e.printStackTrace();
		}
	}

	public void close() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
			isConnect = false;
		}
	}

	public void query(String sql, QueryCallback callback) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection conn = dataSource.getConnection();
					 Statement stmt = conn.createStatement();
					 ResultSet result = stmt.executeQuery(sql)) {

					ArrayList<HashMap<Object, Object>> data = new ArrayList<>();
					int i = 0;

					while (result.next()) {
						HashMap<Object, Object> row = new HashMap<>();
						for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
							row.put(result.getMetaData().getColumnLabel(index), result.getObject(result.getMetaData().getColumnLabel(index)));
						}
						data.add(i, row);
						i++;
					}

					HashMap<Object, Object> queryResult = new HashMap<>();
					queryResult.put("row", (data.size() > 0 ? data.get(0) : new ArrayList<>()));
					queryResult.put("rows", data);
					queryResult.put("num_rows", i);

					Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(queryResult));

				} catch (SQLException e) {
					catchMySQLException(e);
					Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(null));
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	public void querySafe(String sql, ArrayList<Object> parameters, QueryCallback callback) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection conn = dataSource.getConnection();
					 PreparedStatement stmt = conn.prepareStatement(sql)) {

					for (int i = 0; i < parameters.size(); i++) {
						stmt.setObject(i + 1, parameters.get(i));
					}

					try (ResultSet result = stmt.executeQuery()) {
						ArrayList<HashMap<Object, Object>> data = new ArrayList<>();
						int i = 0;

						while (result.next()) {
							HashMap<Object, Object> row = new HashMap<>();
							for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
								row.put(result.getMetaData().getColumnLabel(index), result.getObject(result.getMetaData().getColumnLabel(index)));
							}
							data.add(i, row);
							i++;
						}

						HashMap<Object, Object> queryResult = new HashMap<>();
						queryResult.put("row", (data.size() > 0 ? data.get(0) : new ArrayList<>()));
						queryResult.put("rows", data);
						queryResult.put("num_rows", i);

						Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(queryResult));
					}

				} catch (SQLException e) {
					catchMySQLException(e);
					Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> callback.onQueryComplete(null));
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	public void execute(String sql) {
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
		} catch (SQLException e) {
			catchMySQLException(e);
		}
	}

	public void executeSafe(String sql, ArrayList<Object> parameters) {
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			for (int i = 0; i < parameters.size(); i++) {
				stmt.setObject(i + 1, parameters.get(i));
			}
			stmt.execute();
		} catch (SQLException e) {
			catchMySQLException(e);
		}
	}

	public int executeGetInsertID(String sql, ArrayList<Object> parameters) {
		int insertid = 0;
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			for (int i = 0; i < parameters.size(); i++) {
				stmt.setObject(i + 1, parameters.get(i));
			}
			stmt.executeUpdate();
			try (ResultSet result = stmt.getGeneratedKeys()) {
				if (result.next()) {
					insertid = result.getInt(1);
				}
			}
		} catch (SQLException e) {
			catchMySQLException(e);
		}
		return insertid;
	}

	public static String getDebugSQL(String sql, ArrayList<Object> parameters) {
		StringBuilder debugSQL = new StringBuilder(sql);

		for (Object param : parameters) {
			String valueStr = (param != null) ? param.toString() : "null";
			int index = debugSQL.indexOf("?");
			if (index != -1) {
				debugSQL.replace(index, index + 1, "'" + valueStr + "'");
			}
		}

		return debugSQL.toString();
	}

	public interface QueryCallback {
		void onQueryComplete(HashMap<Object, Object> queryResult);
	}

	private void catchMySQLException(SQLException exception) {
		String message = exception.getMessage();
		int errorCode = exception.getErrorCode();
		print.error(message + " [ErrorCode: " + errorCode + "][TAG: " + tag + "]");
		exception.printStackTrace();
	}
}
