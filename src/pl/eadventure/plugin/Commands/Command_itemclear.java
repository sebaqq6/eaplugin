package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.Utils;

public class Command_itemclear implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(commandSender instanceof Player)) {
			commandSender.sendMessage("Ta komenda może być używana tylko w grze.");
			return true;
		}

		Player player = (Player) commandSender;
		if (args.length != 1) {
			Utils.commandUsageMessage(commandSender, "/itemclear [promień]");
			return true;
		}

		int radius;
		try {
			radius = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			player.sendMessage(Utils.mm("<#FF0000><bold>Podaj poprawną liczbę jako promień.</bold>"));
			return true;
		}

		if (radius < 1 || radius > 500) {
			player.sendMessage(Utils.mm("<#FF0000><bold>Niepoprawny promień.</bold> Dozwolony zakres: <#FFFF00><bold>1 - 500</bold> bloków."));
			return true;
		}

		Location playerLocation = player.getLocation();
		int removedItems = 0;

		for (Entity entity : Bukkit.getWorld(player.getWorld().getName()).getEntities()) {
			if (entity instanceof Item && entity.getLocation().distance(playerLocation) <= radius) {
				entity.remove();
				removedItems++;
			}
		}

		player.sendMessage(Utils.mm("<#FF0000><bold>Usunięto</bold> <#FFFF00><bold>" + removedItems + "</bold><#FF0000> przedmiotów w promieniu <#FFFF00><bold>" + radius + "</bold><#FF0000> bloków."));
		return true;
	}
}

