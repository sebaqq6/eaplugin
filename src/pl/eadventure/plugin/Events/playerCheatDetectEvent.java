package pl.eadventure.plugin.Events;

import me.frep.vulcan.api.event.VulcanFlagEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.eadventure.plugin.Utils.print;

public class playerCheatDetectEvent implements Listener {
	@EventHandler
	public void onPlayerCheat(VulcanFlagEvent e) {
		//print.error("onPlayerCheat: " + e.getPlayer());
		//	print.error("onPlayerCheat: " + e.getCheck().getName());
		//e.setCancelled(true);
	}
}
