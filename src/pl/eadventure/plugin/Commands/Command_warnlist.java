package pl.eadventure.plugin.Commands;

import org.bukkit.ChatColor;
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

public class Command_warnlist implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if(sender instanceof Player player) {
			// /warnlist [nick]
			if (args.length == 0) {//get target nick args[0]
				Utils.commandUsageMessage(sender, String.format("/%s [nick gracza]", label));
				return true;
			}
			String targetName = args[0];
			if (!PunishmentSystem.getListPlayersAll().contains(targetName)) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7nie istnieje.", targetName)));
				return true;
			}
			PunishmentSystem.showWarnListGUI(player, targetName);
		}
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//nick
			if(args[0].isEmpty()) return List.of("Wpisz pierwszą literę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersCanBeBanned(), new ArrayList<>());
		}
		else
			return Collections.emptyList();
	}
}
