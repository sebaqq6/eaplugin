package pl.eadventure.plugin.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class entityDamageEvent implements Listener {
	@EventHandler
	public void onDamageEntityByEntity(EntityDamageByEntityEvent e) {
		//print.okRed("[EntityDamage] " + e.getDamager().getName() + " -> " + e.getEntity().getName() + " (" + e.isCancelled() + ")");

	}
}
