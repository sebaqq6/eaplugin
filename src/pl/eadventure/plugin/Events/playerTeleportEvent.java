package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
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
		Player player = e.getPlayer();
		if (e.isCancelled())
			return;
		boolean isCitizensNPC = player.hasMetadata("NPC");
		if (isCitizensNPC) return;
		PlayerData pd = PlayerData.get(player);
		pd.lastTeleport = Timestamp.from(Instant.now());
	}
}
