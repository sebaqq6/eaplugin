package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.print;

public class MobFixer {
	public static boolean autoModeEnabled = false;

	/**
	 * Ładowanie modułu MobFixer.
	 * Rejestruje zadanie cykliczne i komendę do manualnego naprawiania mobów.
	 */
	public static void load() {
		// Zadanie cykliczne
		Bukkit.getScheduler().runTaskTimer(EternalAdventurePlugin.getInstance(), MobFixer::checkAndFixMobs, 0L, 20L * 60); // Wykonywane co minutę (20 ticków = 1 sekunda)
	}

	/**
	 * Iteruje przez wszystkie światy i sprawdza wszystkie moby.
	 */
	private static void checkAndFixMobs() {
		if (!autoModeEnabled) return;
		Bukkit.getWorlds().forEach(world -> {
			world.getEntities().stream()
					.filter(entity -> entity instanceof LivingEntity) // Filtrujemy tylko LivingEntity
					.map(entity -> (LivingEntity) entity) // Rzutowanie na LivingEntity
					.forEach(MobFixer::handlePotentiallyBuggedMob); // Obsługa każdego moba
		});
	}

	/**
	 * Sprawdza i naprawia potencjalnie zbugowanego moba.
	 */
	private static void handlePotentiallyBuggedMob(LivingEntity mob) {
		if (isBugged(mob)) {
			print.info("Wykryto niezniszczalnego moba: " + mob.getType() + " w " + mob.getLocation());
			respawnMob(mob);
		}
	}

	/**
	 * Sprawdza, czy mob jest zbugowany (np. odporny na obrażenia).
	 */
	private static boolean isBugged(LivingEntity mob) {
		/*try {
			mob.damage(0); // Próba zadania obrażeń
			return false;  // Jeśli nie rzuci wyjątku, mob jest w porządku
		} catch (Exception e) {
			return true;   // Jeśli rzuci wyjątek, mob jest zbugowany
		}*/
		if (mob.isInvulnerable()) {
			return false;
		}

		double originalHealth = mob.getHealth();
		mob.damage(1); // Próba zadania 1 punktu obrażeń
		boolean bugged = mob.getHealth() == originalHealth; // Jeśli zdrowie się nie zmienia, mob może być "zbugowany"
		mob.setHealth(originalHealth); // Przywracamy oryginalne zdrowie

		return bugged;

	}

	/**
	 * Respawnuje zbugowanego moba, tworząc go na nowo w tej samej lokalizacji.
	 */
	private static void respawnMob(LivingEntity mob) {
		Location location = mob.getLocation();
		EntityType type = mob.getType();
		EntityEquipment equipment = mob.getEquipment();

		// Usuń starego moba
		mob.remove();

		// Stwórz nowego moba w tym samym miejscu
		World world = location.getWorld();
		if (world != null) {
			LivingEntity newMob = (LivingEntity) world.spawnEntity(location, type);

			// Odzyskaj ekwipunek (jeśli dotyczy)
			if (equipment != null) {
				newMob.getEquipment().setArmorContents(equipment.getArmorContents());
				newMob.getEquipment().setItemInMainHand(equipment.getItemInMainHand());
				newMob.getEquipment().setItemInOffHand(equipment.getItemInOffHand());
			}

			print.info("Zrespawnowano moba: " + newMob.getType() + " w " + newMob.getLocation());
		}
	}


	public static void manualFixMob(Player player, boolean force) {
		Entity target = player.getTargetEntity(10);
		if (target instanceof LivingEntity mob) {
			if (isBugged(mob) || force) {
				respawnMob(mob);
				player.sendMessage("Naprawiono zbugowanego moba: " + mob.getType());
			} else {
				player.sendMessage("Ten mob nie jest zbugowany: " + mob.getType());
			}
		} else {
			player.sendMessage("Nie patrzysz na moba!");
		}
	}
}
