package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.List;

public class Command_redflag implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		new BukkitRunnable() {
			@Override
			public void run() {
				redFlagExecute(commandSender, args);
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
		return true;
	}

	private void redFlagExecute(CommandSender sender, String[] args) {
		String redflagToken = gVar.redFlagToken;
		if (args.length == 0) {
			Utils.commandUsageMessage(sender, "/redflag [status/start/stop/add (ip)]");
			return;
		}
		String apiUrl = "";
		switch (args[0]) {
			case "status" -> {
				apiUrl = "https://eadventure.pl/api/redflag.php?arg=status&access_token=" + redflagToken;
			}
			case "start" -> {
				apiUrl = "https://eadventure.pl/api/redflag.php?arg=start&access_token=" + redflagToken;
			}
			case "stop" -> {
				apiUrl = "https://eadventure.pl/api/redflag.php?arg=stop&access_token=" + redflagToken;
			}
			case "add" -> {
				if (args.length < 2) {
					Utils.commandUsageMessage(sender, "/redflag add [ip]");
					return;
				}
				String ip = args[1];
				if (Utils.containsIPAddress(ip)) {
					apiUrl = "https://eadventure.pl/api/redflag.php?access_token=" + redflagToken + "&arg=add&ip=" + ip;
				} else {
					sender.sendMessage(Utils.mm("<#FF0000>Niepoprawny adres IP."));
					return;
				}
			}
			default -> {
				Utils.commandUsageMessage(sender, "/redflag [status/start/stop/add (ip)]");
				return;
			}
		}
		sender.sendMessage("Przetwarzanie...");
		// Wykonanie zapytania GET
		Utils.sendHttpsRequest(apiUrl, "GET", null)
				.thenAccept(response -> {
					// Wyświetlenie odpowiedzi graczowi
					String[] lines = response.split("<br>");
					for (String line : lines) {
						sender.sendMessage(line);
					}

				})
				.exceptionally(ex -> {
					// Obsługa błędów
					sender.sendMessage("Wystąpił błąd. Zgłoś to!");
					print.error("Wystąpił błąd podczas pobierania danych: " + ex.getMessage());
					return null;
				});
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
