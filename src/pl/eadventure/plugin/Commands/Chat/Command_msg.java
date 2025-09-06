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

public class Command_msg implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {//get target nick args[0]
			Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [wiadomość]", label));
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
		if (pdt.disabledMsg) {
			sender.sendMessage(Utils.mm("<gray>Gracz <gold>" + targetPlayer.getName() + " <gray>ma <red>wyłączone <gray>otrzymywanie prywatnych wiadomości."));
			return true;
		}
		int ignoredType = pdt.ignoreList.isIgnored(sender.getName());
		if (ignoredType == IgnoreList.EntryType.ALL || ignoredType == IgnoreList.EntryType.PRIVATE) {
			sender.sendMessage(Utils.mm("<gray>Gracz <gold>" + targetPlayer.getName() + " <red>ignoruje <gray>Twoje wiadomości."));
			return true;
		}
		if (args.length == 1) {//message args[1]
			Utils.commandUsageMessage(sender, String.format("/%s %s [wiadomość]", label, targetName));
			return true;
		}
		StringBuilder messageBuilder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			messageBuilder.append(args[i]).append(" ");
		}
		String message = messageBuilder.toString().trim();
		//sender
		String playerInformation = String.format("<#FF0000>✉<#FF0000><bold>→>></bold><#809aff> %s:<#ffd966> %s", targetPlayer.getName(), message);
		sender.sendMessage(Utils.mm(playerInformation));
		//receiver
		playerInformation = String.format("<#00FF00><bold><<←</bold><#00FF00>✉<#809aff> %s:<#ffd966> %s", sender.getName(), message);
		targetPlayer.sendMessage(Utils.mm(playerInformation));
		targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
		//save for replay
		if (sender instanceof Player senderPlayer) {
			PlayerData pd = PlayerData.get(senderPlayer);
			pd.replayMsgNick = targetPlayer.getName();
			if (pd.disabledMsg) {
				sender.sendMessage(Utils.mm("<grey>Masz wyłączone otrzymywanie prywatnych wiadomości (<red>/msgtog</red>), gracz może Ci nie odpowiedzieć."));
			}
			pdt.replayMsgNick = senderPlayer.getName();
		}
		//log?
		playerPrivateChatEvent.onPlayerSendPrivateMessage(sender, targetPlayer, message);
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
