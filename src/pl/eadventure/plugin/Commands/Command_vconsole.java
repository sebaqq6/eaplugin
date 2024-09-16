package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.util.Collections;
import java.util.List;

public class Command_vconsole implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			sender.sendMessage("Użyj: /vconsole [komenda] - aby użyć konsoli Velocity.");
			sender.sendMessage("Przykład 1: /vconsole velocity plugins");
			sender.sendMessage("Przykład vSudo: /vconsole vsudo JakisGracz123 /velocity plugins");
			return true;
		}
		String commandToRelay = String.join(" ", args);
		byte[] data = commandToRelay.getBytes();

		Bukkit.getServer().sendPluginMessage(EternalAdventurePlugin.getInstance(), "velocity:relay", data);
		if (sender instanceof Player player) {
			if (player.isOp()) {
				sender.sendMessage("Wysłano do proxy Velocity: " + commandToRelay);
			}
		} else {
			sender.sendMessage("Wysłano do proxy Velocity: " + commandToRelay);
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		return Collections.emptyList();
	}
}
