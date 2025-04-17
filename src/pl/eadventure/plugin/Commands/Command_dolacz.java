package pl.eadventure.plugin.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.FunEvents.FunEventsManager;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import static pl.eadventure.plugin.FunEvents.FunEventsManager.inventoryHasOnlySet;

public class Command_dolacz implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			if (funEventManager.isRecords()) {
				if (funEventManager.getActualFunEvent().isOwnSet()) {
					if (!inventoryHasOnlySet(player)) {
						return true;
					}
				}
				if (funEventManager.registerPlayer(player)) {
					Component message = Utils.mm(String.format("" +
							"<green><bold>Zapisałeś/aś się na: <blue>%s<green>. Wpisz ponownie <#FF0000>/dolacz</#FF0000> aby zrezygnować</bold>. ", funEventManager.getActualFunEvent().getEventName()));
					player.sendMessage(message);
				} else if (funEventManager.unregisterPlayer(player)) {
					player.sendMessage(Utils.mm("<grey>Zrezygnowałeś/aś z zabawy: <blue><bold>" + funEventManager.getActualFunEvent().getEventName()));
				} else {
					player.sendMessage(Utils.mm("<grey>Brak wolnych miejsc, aby uczestniczyć w: <blue><bold>" + funEventManager.getActualFunEvent().getEventName()));
				}

			}
		}
		return true;
	}
}
