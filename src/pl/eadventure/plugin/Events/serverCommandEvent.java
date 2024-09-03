package pl.eadventure.plugin.Events;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import pl.eadventure.plugin.Utils.print;

public class serverCommandEvent implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerCommand(ServerCommandEvent e) {
		CommandSender sender = e.getSender();
		String command = e.getCommand();
		//print.debug(sender+" - wpisano komendę: "+command);
	}
}
