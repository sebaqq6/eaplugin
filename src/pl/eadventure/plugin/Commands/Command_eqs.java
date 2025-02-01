package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Utils.Utils;

import java.util.List;

public class Command_eqs implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		sender.sendMessage(Utils.mm("<rainbow>Jeszcze nad tym pracuje//Desmond"));
		if (args.length == 0) {
			Utils.commandUsageMessage(sender, "/eqs [id ekwipunku]");
			return true;
		}
		if (sender instanceof Player player) {
			int eqId = -1;
			try {
				eqId = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				sender.sendMessage(Utils.mm("<#FF0000>Nieprawidłowa ID ekwipunku <bold>(wartość nie jest cyfrą)</bold>."));
				return true;
			}
			if (eqId < 1) {
				sender.sendMessage(Utils.mm("<#FF0000>Nieprawidłowe <bold>ID ekwipunku</bold>."));
				return true;
			}
			sender.sendMessage("EQID: " + eqId);
		} else {
			sender.sendMessage("Komenda dostępna tylko z poziomu gry.");
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
