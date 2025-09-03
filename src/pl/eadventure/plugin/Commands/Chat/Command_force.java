package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Events.playerPrivateChatEvent;
import pl.eadventure.plugin.Modules.Chat.IgnoreList;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Command_force implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {//get target nick args[0]
			Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [treść]", label));
			return true;
		}
		String targetName = args[0];
		/*if (targetName.equalsIgnoreCase(sender.getName())) {//if target == player
			sender.sendMessage(Utils.color("&7Nie możesz tego użyć na sobie."));
			return true;
		}*/
		Player targetPlayer = Bukkit.getPlayer(targetName);
		if (targetPlayer == null || !targetPlayer.isOnline() || PlayerUtils.isVanished(targetPlayer)) {
			sender.sendMessage(Utils.mm("<grey>Gracz " + targetName + " <grey>jest offline."));
			return true;
		}
		PlayerData pdt = PlayerData.get(targetPlayer);

		if (args.length == 1) {//message args[1]
			Utils.commandUsageMessage(sender, String.format("/%s %s [treść]", label, targetName));
			return true;
		}
		StringBuilder messageBuilder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			messageBuilder.append(args[i]).append(" ");
		}
		String message = messageBuilder.toString().trim();
		sender.sendMessage(Utils.mm("<grey>Force-chat dla <red>" + targetPlayer.getName() + "<grey>. Treść: <yellow>" + message));
		targetPlayer.chat(message);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			if (args[0].isEmpty()) {
				return List.of("Wpisz nazwe gracza..."); // Prompt for input
			}

			List<String> playerNames = Bukkit.getOnlinePlayers().stream()
					.filter(player -> !PlayerUtils.isVanished(player))
					.map(Player::getName)
					.collect(Collectors.toList());

			return StringUtil.copyPartialMatches(args[0], playerNames, new ArrayList<>());
		} else {
			return Collections.emptyList(); // No suggestions for other arguments
		}
	}

}
