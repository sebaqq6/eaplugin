package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Events.playerPrivateChatEvent;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

public class Command_msg implements CommandExecutor {
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
			PlayerData.get(senderPlayer).replayMsgNick = targetPlayer.getName();
		}
		//log?
		playerPrivateChatEvent.onPlayerSendPrivateMessage(sender.getName(), targetPlayer.getName(), message);
		//TODO SPY
		return true;
	}
}
