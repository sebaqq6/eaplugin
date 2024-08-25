package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.PunishmentSystem;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Command_kartoteka implements TabExecutor {
	// /kartoteka nickgracza
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			if (args.length == 0) {//get target nick args[0]
				PunishmentSystem.showAdminLogGUI(player, null, -1, 1, null);//show all
				return true;
			}
			String targetName = args[0];
			if (!PunishmentSystem.getListPlayersAll().contains(targetName)) {
				sender.sendMessage(Utils.color("&7Taki gracz nie istnieje."));
				return true;
			}
			PunishmentSystem.showAdminLogGUI(player, targetName, -1, 1, null);//show per player
		}
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//nick
			if(args[0].isEmpty()) return List.of("Wpisz pierwszą literę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersAll(), new ArrayList<>());
		} else
			return Collections.emptyList();
	}
}
