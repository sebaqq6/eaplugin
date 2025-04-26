package pl.eadventure.plugin.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.eadventure.plugin.PlayerData;

import java.sql.Timestamp;
import java.time.Instant;

public class playerTeleportEvent implements Listener {
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent e) {
		if (e.isCancelled())
			return;
		PlayerData pd = PlayerData.get(e.getPlayer());
		pd.lastTeleport = Timestamp.from(Instant.now());
	}
}
