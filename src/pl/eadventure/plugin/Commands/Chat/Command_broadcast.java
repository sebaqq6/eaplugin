package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.Utils;

public class Command_broadcast implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {//get target nick args[0]
			Utils.commandUsageMessage(sender, String.format("/%s [wiadomość]", label));
			return true;
		}
		StringBuilder messageBuilder = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			messageBuilder.append(args[i]).append(" ");
		}
		String message = messageBuilder.toString().trim();
		Bukkit.broadcastMessage(Utils.color(message));
		return true;
	}
}
