package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.List;

public class Command_streamer implements TabExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			if (args.length >= 1 && player.hasPermission("eadventureplugin.streamer.manage")) {
				if (args[0].equalsIgnoreCase("list")) {
					gVar.liveStream.showStreamersList(player);
				} else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("del")) {
					if (args.length == 2) {
						String nick = args[1];
						Player target = Bukkit.getPlayer(nick);
						if (args[0].equalsIgnoreCase("add")) {
							if (target != null && target.isOnline()) {
								gVar.liveStream.addStreamer(target);
								sender.sendMessage(Utils.mm(String.format("<green>Dodałeś gracza %s jako streamera live.", target.getName())));
							} else {
								sender.sendMessage(Utils.mm("<red>Ten gracz nie jest online."));
							}
						} else if (args[0].equalsIgnoreCase("del")) {
							gVar.liveStream.delStreamer(nick);
							sender.sendMessage(Utils.mm(String.format("<yellow>Usunąłeś gracza %s z streamerów live.", target.getName())));
						}
					} else {
						Utils.commandUsageMessage(sender, String.format("/%s [add/del] [nick]", label));
					}
				}
				return true;
			}
			PlayerData pd = PlayerData.get(player);
			if (pd.isStreamer == 1) {
				gVar.liveStream.showGui(player);
			}
		} else {
			sender.sendMessage("Komenda dostępna tylko z poziomu gry.");
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		return List.of();
	}
}
