package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Command_blueflag implements TabExecutor {
	public static String kickMessage = "&c&lWystąpił nieoczekiwany błąd.\n" +
			"\n" +
			"&cGracz: &3<player>\n" +
			"\n" +
			"&5Discord: &5&ndiscord.eadventure.pl\n";

	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		blueFlagExecute(commandSender, args);
		return true;
	}

	private void blueFlagExecute(CommandSender sender, String[] args) {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		List<String> wl = gVar.whiteList;
		if (args.length == 0) {
			Utils.commandUsageMessage(sender, "/blueflag [status/start/stop/add (nick)]");
			return;
		}
		switch (args[0]) {
			case "status" -> {
				if (wl.isEmpty()) {
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag: <red>NIEAKTYWNY <grey>(Domyślnie)"));
				} else {
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag: <green>AKTYWNY <grey>(Domyślnie: NIEAKTYWNY)"));
				}
			}
			case "start" -> {
				if (!wl.isEmpty()) {
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag jest już: <green>AKTYWNY <grey>(Domyślnie: NIEAKTYWNY)"));
					return;
				}
				String sql = "SELECT nick FROM players WHERE onlineHours >= 5;";
				storage.query(sql, queryResult -> {
					int numRows = (int) queryResult.get("num_rows");
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
					if (numRows > 0) {
						for (int i = 0; i < numRows; i++) {
							String nick = (String) rows.get(i).get("nick");
							wl.add(nick);
						}
					}
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag został: <green>WŁĄCZONY <grey>(Dodano: " + numRows + " wyjątków)"));
				});

			}
			case "stop" -> {
				if (wl.isEmpty()) {
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag jest już: <red>NIEAKTYWNY <grey>(Domyślnie)"));
				} else {
					wl.clear();
					sender.sendMessage(Utils.mm("<#0000FF><bold>System BlueFlag został: <red>WYŁĄCZONY"));
				}
			}
			case "add" -> {
				if (args.length < 2) {
					Utils.commandUsageMessage(sender, "/redflag blueflag [nick]");
					return;
				}
				if (wl.isEmpty()) {
					sender.sendMessage(Utils.mm("<grey>System jest wyłączony."));
					return;
				}
				String nick = args[1];
				if (nick.length() > 3) {
					if (wl.contains(nick)) {
						sender.sendMessage(Utils.mm("<grey>Nick: <blue>" + nick + "</blue> jest już dodany do wyjątków systemu Blueflag"));
					} else {
						wl.add(nick);
						sender.sendMessage(Utils.mm("<#00FF00>Dodano: <blue>" + nick));
					}
				} else {
					sender.sendMessage(Utils.mm("<grey>Niepoprawny nick."));
					return;
				}
			}
			default -> {
				Utils.commandUsageMessage(sender, "/blueflag [status/start/stop/add (nick)]");
				return;
			}
		}
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
