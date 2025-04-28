package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Modules.AutoSpectator;
import pl.eadventure.plugin.Utils.Utils;

public class Command_autospec implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			if (AutoSpectator.enable(player)) {
				player.sendMessage(Utils.mm("<grey>AutoSpec: <green>ON"));
			} else if (AutoSpectator.disable(player)) {
				player.sendMessage(Utils.mm("<grey>AutoSpec: <red>OFF"));
			}
		}
		return true;
	}
}
