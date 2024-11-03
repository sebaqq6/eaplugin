package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Utils.Utils;

import java.util.List;

public class Command_extitle implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

		if (args.length < 3) {
			sender.sendMessage(Utils.mm("<gray>Użyj: /extitle [subtitle/title/all] [nick/*] [text/title|opcjonalny subtitle jeśli wybrano: all]"));
			return true;
		}
		String option = args[0];
		String target = args[1];

		StringBuilder textBuilder = new StringBuilder();
		for (int i = 2; i < args.length; i++) {
			textBuilder.append(args[i]).append(" ");
		}
		String text = textBuilder.toString().trim();
		Player playerTarget = Bukkit.getPlayer(target);
		if ((playerTarget == null || !playerTarget.isOnline()) && !target.equalsIgnoreCase("*")) {
			sender.sendMessage("Wybrany gracz nie jest online.");
			return true;
		}
		if (option.equalsIgnoreCase("subtitle")) {
			if (target.equalsIgnoreCase("*")) {
				Bukkit.getOnlinePlayers().forEach(allPlayers -> {
					allPlayers.sendTitle("", ChatColor.translateAlternateColorCodes('&', text), 10,
							140, 10);
				});
			} else {
				playerTarget.sendTitle("", ChatColor.translateAlternateColorCodes('&', text), 10,
						140, 10);
			}
		} else if (option.equalsIgnoreCase("title")) {
			if (target.equalsIgnoreCase("*")) {
				Bukkit.getOnlinePlayers().forEach(allPlayers -> {
					allPlayers.sendTitle(ChatColor.translateAlternateColorCodes('&', text), "", 10,
							140, 10);
				});
			} else {
				playerTarget.sendTitle(ChatColor.translateAlternateColorCodes('&', text), "", 10,
						140, 10);
			}
		} else if (option.equalsIgnoreCase("all")) {
			sender.sendMessage(Utils.mm("<#FF0000>Ta opcja jeszcze nie została ogarnięta..."));
		} else {
			sender.sendMessage(Utils.mm("<#FF0000>Dostępne opcje: <#00FF00>subtitle, title, all"));
			return true;
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
