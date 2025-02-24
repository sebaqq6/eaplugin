package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Command_donating implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		if (args.length < 3) {
			sender.sendMessage(Utils.mm("<gray>Użyj: /donating [playerNick] [add/del] [ilość]"));
			return true;
		}
		String playerNick = args[0];
		String operation = args[1];
		String strCount = args[2];
		int count = 0;
		try {
			count = Integer.parseInt(strCount);
		} catch (NumberFormatException e) {
			sender.sendMessage(Utils.mm("<#FF0000>Nieprawidłowo podana ilość (dozwolone tylko cyfry)."));
			return true;
		}
		if (operation.equalsIgnoreCase("add")) {
			print.info(String.format("[DONATE] Gracz %s zakupił %d Diamentowe Serca.", playerNick, count));

			String queryNick = "SELECT id FROM players WHERE nick=?";
			ArrayList<Object> arguments = new ArrayList<>();
			arguments.add(playerNick);
			int finalCount = count;
			storage.querySafe(queryNick, arguments, queryResult -> {
				int numRows = (int) queryResult.get("num_rows");
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
				int playerid = 0;
				if (numRows > 0) {
					for (int i = 0; i < numRows; i++) {
						playerid = (int) rows.get(i).get("id");
					}
				}
				arguments.clear();
				String insertNewDonate = "INSERT INTO donate (playerid, nick, count) VALUES (?, ?, ?);";
				arguments.add(playerid);
				arguments.add(playerNick);
				arguments.add(finalCount);
				storage.executeSafe(insertNewDonate, arguments);
			});
		} else if (operation.equalsIgnoreCase("del")) {
			sender.sendMessage(Utils.mm("<#FF0000>Nieobsługiwane, potrzebne?"));
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
