package pl.eadventure.plugin.Commands;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Command_unban implements TabExecutor {
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			if (args[0].isEmpty()) return Arrays.asList("Wpisz pierwszą literę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersCanBeUnbanned(), new ArrayList<>());
		} else
			return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			Utils.commandUsageMessage(sender, String.format("/%s [nick gracza]", label));
			return true;
		}
		if (PunishmentSystem.getListPlayersCanBeUnbanned().contains(args[0])) {
			sender.sendMessage(Utils.color(String.format("&7Gracz &2%s &7został &2pomyślnie &7odbanowany.", args[0])));
			PunishmentSystem.unbanPlayer(args[0]);
		} else {
			sender.sendMessage(Utils.color(String.format("&c%s nie jest zbanowany.", args[0])));
		}
		return true;
	}
}
