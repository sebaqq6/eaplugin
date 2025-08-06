package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

public class Command_r implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.replayMsgNick == null) {
				sender.sendMessage(Utils.mm("<grey>Z nikim ostatnio nie prowadzono konwersacji."));
				Utils.commandUsageMessage(sender, "/msg [nick] [wiadomość]");
				return true;
			}
			if (args.length == 0) {//get message
				Utils.commandUsageMessage(sender, String.format("/%s [wiadomość]", label));
				return true;
			}
			StringBuilder messageBuilder = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				messageBuilder.append(args[i]).append(" ");
			}
			String message = messageBuilder.toString().trim();
			player.performCommand(String.format("msg %s %s", pd.replayMsgNick, message));
		}
		return true;
	}
}
