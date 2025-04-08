package pl.eadventure.plugin.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.eadventure.plugin.PlayerData;

public class playerMoveEvent implements Listener {
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		PlayerData pd = PlayerData.get(e.getPlayer());
		if (pd.freeze) {
			if (e.getFrom().getX() == e.getTo().getX() &&
					e.getFrom().getY() == e.getTo().getY() &&
					e.getFrom().getZ() == e.getTo().getZ()) return;
			e.setTo(e.getFrom());
			//e.setCancelled(true);
		}
	}
}
