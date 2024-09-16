package pl.eadventure.plugin.Events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import pl.eadventure.plugin.PlayerData;

public class playerPickupItemEvent implements Listener {
	@EventHandler
	public void onPlayerPickupItem(EntityPickupItemEvent e) {
		LivingEntity entity = e.getEntity();
		//playerPickupItemEvent
		if (entity instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.creativeMode) {
				e.setCancelled(true);
			}
		}
	}
}
