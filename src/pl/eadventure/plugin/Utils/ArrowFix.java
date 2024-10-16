package pl.eadventure.plugin.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ArrowFix {
	public static final Map<UUID, Long> arrows = new HashMap<>();
	private static final long ARROW_LIFETIME = 5 * 1000; // 30 sekund w milisekundach

	public static void run(Plugin plugin) {
		new BukkitRunnable() {
			@Override
			public void run() {
				check();
			}
		}.runTaskTimer(plugin, 0L, 20L); // Timer co 1 sekundę (20 ticków)
	}

	public static void check() {
		long currentTime = System.currentTimeMillis();
		//print.ok("checking... ");

		// Iteruj przez strzały i usuwaj te, które są starsze niż 30 sekund
		Iterator<Map.Entry<UUID, Long>> iterator = arrows.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();
			UUID arrowId = entry.getKey();
			long launchTime = entry.getValue();

			// Sprawdź, czy strzała powinna zostać usunięta
			if (currentTime - launchTime >= ARROW_LIFETIME) {
				Entity arrow = Bukkit.getEntity(arrowId);
				if (arrow != null && arrow instanceof Arrow && !arrow.isDead()) {
					//print.ok("removeprojectTile");
					arrow.remove();
				}
				iterator.remove();  // Usuń strzałę z mapy
			}
		}
	}
}
