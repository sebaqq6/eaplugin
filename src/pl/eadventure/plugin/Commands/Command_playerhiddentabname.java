package pl.eadventure.plugin.Commands;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Utils.Utils;

import java.util.List;

public class Command_playerhiddentabname implements TabExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		//TabAPI tabAPI = TabAPI.getInstance();
		//tabAPI.getNameTagManager().hideNameTag();
		if (args.length < 2) {//get target nick args[0]
			Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [true/false]", label));
			return true;
		}
		String targetName = args[0];
		String targetOption = args[1];
		Player player = Bukkit.getPlayer(targetName);
		if (player == null || !player.isOnline()) {
			sender.sendMessage("Ten gracz jest offline.");
			return true;
		}
		TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
		TabAPI tabAPI = TabAPI.getInstance();

		if (targetOption.equalsIgnoreCase("true")) {
			tabAPI.getNameTagManager().hideNameTag(tabPlayer);
		} else if (targetOption.equalsIgnoreCase("false")) {
			tabAPI.getNameTagManager().showNameTag(tabPlayer);
		} else {
			sender.sendMessage("Dostepne opcje: true, false");
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
