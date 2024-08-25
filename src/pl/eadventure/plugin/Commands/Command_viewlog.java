package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;

public class Command_viewlog implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		Player player = null;
		if(sender instanceof Player p) {
			player = p;
		}
		Player finalPlayer = player;
		new BukkitRunnable() {
			@Override
			public void run() {
				String session = Utils.generateRandomString(9);

				String playerName;
				String ip;
				if(finalPlayer != null) {
					playerName = finalPlayer.getName();
					ip = finalPlayer.getAddress().getAddress().getHostAddress();
				} else {
					playerName = session;
					ip = "-1";
				}


				int expire = (int) Utils.getUnixTimestamp() + 60 * 30;
				if(finalPlayer == null) expire = (int) Utils.getUnixTimestamp() + 60 * 2;
				//remove other sessions
				String sqlRemoveSessions = "DELETE FROM logviewersessions WHERE playerName = ?;";
				ArrayList<Object> sqlParams = new ArrayList<>();
				sqlParams.add(playerName);
				storage.executeSafe(sqlRemoveSessions, sqlParams);
				//create new sessions
				//player name is added, add other params...
				sqlParams.add(session);
				sqlParams.add(ip);
				sqlParams.add(expire);
				String sqlCreateSession = "INSERT INTO `logviewersessions` (`playerName`, `session`, `ip`, `expire`) VALUES (?, ?, ?, ?);";
				storage.executeSafe(sqlCreateSession, sqlParams);
				//http://play.eadventure.pl:3000/?session=SESJATUTAJ
				new BukkitRunnable() {
					@Override
					public void run() {
						if(finalPlayer != null) {
							PlayerUtils.sendColorMessage(finalPlayer, "&d&lTwoja sesja dostępna jest pod adresem:");
							String cmdTellRaw = String.format("tellraw %s {\"text\":\"https://logs.eadventure.pl/?session=%s\",\"bold\":true,\"underlined\":true,\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://logs.eadventure.pl/?session=%s\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Kliknij aby otworzyć link. Sesja trwa maksymalnie 30 minut.\",\"color\":\"dark_purple\"}]}}", finalPlayer.getName(), session, session);
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdTellRaw);
						} else {
							sender.sendMessage(Utils.color("&d&lTwoja sesja dostępna jest pod adresem:"));
							sender.sendMessage(Utils.color("&a&lhttps://logs.eadventure.pl/?session=" + session));
						}

					}
				}.runTask(EternalAdventurePlugin.getInstance());
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
		return true;
	}
}
