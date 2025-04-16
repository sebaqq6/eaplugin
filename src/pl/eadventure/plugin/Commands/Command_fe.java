package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.FunEvents.FunEventsCommands;
import pl.eadventure.plugin.gVar;

import java.util.Collections;
import java.util.List;

import static pl.eadventure.plugin.FunEvents.FunEventsCommands.onFunEventCommand;

public class Command_fe implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		onFunEventCommand(sender, args);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

		return FunEventsCommands.onTabComplete(sender, command, label, args);
	}
}
