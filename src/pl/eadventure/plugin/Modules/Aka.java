package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

public class Aka {
	static final MySQLStorage storage = EternalAdventurePlugin.getMySQL();

	public static void checkPlayer(String playerName) {
		List<String> akaPlayers = new ArrayList<>();
		String sql = "SELECT p2.nick, p2.banned AS banned " +
				"FROM players p1 " +
				"JOIN players p2 ON p1.ip = p2.ip " +
				"WHERE p1.nick = ? " +
				"AND p2.nick != ?;";
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(playerName);
		parameters.add(playerName);
		storage.querySafe(sql, parameters, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					String nick = (String) rows.get(i).get("nick");
					int banned = (int) rows.get(i).get("banned");
					if (banned > 0) {
						akaPlayers.add("<st>" + nick + "</st>");
					} else {
						akaPlayers.add(nick);
					}

				}
			}

			if (!akaPlayers.isEmpty()) {
				// Przygotowanie informacji o kontach
				StringJoiner joiner = new StringJoiner("<gray>,</gray> ");
				int accountsToShow = Math.min(akaPlayers.size(), 5);  // Pokaż maksymalnie 5 kont
				for (int i = 0; i < accountsToShow; i++) {
					joiner.add(akaPlayers.get(i));
				}

				// Reszta kont, które się nie zmieściły
				int remainingAccounts = akaPlayers.size() - accountsToShow;

				// Przygotowanie listy pozostałych graczy do wyświetlenia po najechaniu myszką
				String remainingPlayersList = String.join("<gray>,</gray> ", akaPlayers.subList(accountsToShow, akaPlayers.size()));

				// Tworzenie wiadomości
				String akaInfo;
				if (remainingAccounts > 0) {
					akaInfo = String.format("<gray><bold>[<#FF0000>MULTIKONTO</#FF0000>]</bold> <#00FF00><bold>%s</bold></#00FF00> aka. <#FF0000><i>%s</i></#FF0000></gray> " +
									"<underlined><#00AA00><hover:show_text:'<#00AA00><i>%s</i></#00AA00>'>(%d więcej)</hover></#00AA00></underlined>",
							playerName, joiner.toString(), remainingPlayersList, remainingAccounts);
					Bukkit.getConsoleSender().sendMessage(Utils.mm(akaInfo + " <#00AA00><i>[" + remainingPlayersList + "]</i></#00AA00>"));
				} else {
					akaInfo = String.format("<gray><bold>[<#FF0000>MULTIKONTO</#FF0000>]</bold> <#00FF00><bold>%s</bold></#00FF00> aka. <#FF0000><i>%s</i></#FF0000></gray>",
							playerName, joiner.toString());
					Bukkit.getConsoleSender().sendMessage(Utils.mm(akaInfo));
				}

				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.hasPermission("eadventureplugin.showaka")) {
						player.sendMessage(Utils.mm(akaInfo));
					}
				}
			}
		});
	}
}
