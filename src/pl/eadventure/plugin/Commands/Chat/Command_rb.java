package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

public class Command_rb implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.rainbowChat) {
				pd.rainbowChat = false;
				sender.sendMessage(Utils.mm("<gray>Pisanie tęczowym kolorem <red>wyłączone<grey>."));
			} else {
				pd.rainbowChat = true;
				sender.sendMessage(Utils.mm("<gray>Pisanie tęczowym kolorem <green>włączone<gray>."));
			}
		}
		return true;
	}
}
