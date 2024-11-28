package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Modules.MobFixer;
import pl.eadventure.plugin.Utils.Utils;

import java.util.List;

public class Command_fixmob implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			boolean force = false;
			if (args.length == 1) {
				if (args[0].equalsIgnoreCase("force")) {
					player.sendMessage(Utils.mm("<#FF0000>Użyto parametru FORCE :)"));
					force = true;
				}
			}
			MobFixer.manualFixMob(player, force);
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
