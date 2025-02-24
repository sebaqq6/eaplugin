package pl.eadventure.plugin.Events;

import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import pl.eadventure.plugin.Utils.ArrowFix;

import java.util.UUID;

public class onProjectileLaunchEvent implements Listener {
	@EventHandler
	public void onArrowShoot(ProjectileLaunchEvent event) {
		// Sprawdź, czy wystrzelony obiekt to strzała
		if (event.getEntity() instanceof Arrow) {
			Arrow arrow = (Arrow) event.getEntity();
			UUID arrowId = arrow.getUniqueId();
			//print.okRed("Add new projectile");
			// Dodaj strzałę do mapy z aktualnym czasem
			ArrowFix.arrows.put(arrowId, System.currentTimeMillis());
		}
	}
}
