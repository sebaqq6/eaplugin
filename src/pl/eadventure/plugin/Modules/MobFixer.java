package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MobFixer {
	public static boolean autoModeEnabled = false;
	private static boolean working = false;

	/**
	 * Ładowanie modułu MobFixer.
	 * Rejestruje zadanie cykliczne i komendę do manualnego naprawiania mobów.
	 */
	public static void load() {
		// Zadanie cykliczne
		Bukkit.getScheduler().runTaskTimer(EternalAdventurePlugin.getInstance(), MobFixer::checkAndFixMobs, 0L, 20L * 60L); // Wykonywane co minutę (20 ticków = 1 sekunda)
	}

	private static class MobInfo {
		private static HashMap<UUID, MobInfo> mobs = new HashMap<>();
		private LivingEntity mob;
		private int mobWarns;
		private boolean mobGood;
		private Timestamp lastChecked;

		public MobInfo(LivingEntity entity) {
			if (entity == null) return;
			this.mobWarns = 0;
			this.mobGood = false;
			this.mob = entity;
			this.lastChecked = Timestamp.from(Instant.now());
			mobs.put(entity.getUniqueId(), this);
		}


		public static MobInfo get(LivingEntity entity) {
			if (mobs.containsKey(entity.getUniqueId())) return mobs.get(entity.getUniqueId());
			else return new MobInfo(entity);
		}

		public static void free(LivingEntity entity) {
			if (mobs.containsKey(entity.getUniqueId())) mobs.remove(entity.getUniqueId());
		}

		public LivingEntity getMob() {
			return mob;
		}

		public void setMob(LivingEntity mob) {
			this.mob = mob;
		}

		public int getMobWarns() {
			return mobWarns;
		}

		public void setMobWarns(int mobWarns) {
			this.mobWarns = mobWarns;
		}

		public boolean isMobGood() {
			return mobGood;
		}

		public void setMobGood(boolean mobGood) {
			this.mobGood = mobGood;
		}

		public Timestamp getLastChecked() {
			return lastChecked;
		}

		public void setLastChecked(Timestamp lastChecked) {
			this.lastChecked = lastChecked;
		}

		public static void clearUnused() {
			Timestamp now = Timestamp.from(Instant.now());
			long expire = now.getTime() - 10 * 60 * 1000; // min * x * x;

			mobs.entrySet().removeIf(entry -> {
				MobInfo mobInfo = entry.getValue();
				return mobInfo.getLastChecked().getTime() < expire;
			});
		}

		public static int getSize() {
			return mobs.size();
		}
	}

	/**
	 * Iteruje przez wszystkie światy i sprawdza wszystkie moby.
	 */
	private static void checkAndFixMobs() {
		working = false;
		if (!autoModeEnabled) return;
		working = true;
		long bm = Utils.benchmarkStart();
		Bukkit.getWorlds().forEach(world -> {
			world.getEntities().stream()
					.filter(entity -> entity instanceof LivingEntity) // Filtrujemy tylko LivingEntity
					.filter(entity -> !(entity instanceof Player)) // Pomijamy graczy
					.filter(entity -> !(entity instanceof ArmorStand)) // Pomijamy ArmorStandy
					.map(entity -> (LivingEntity) entity) // Rzutowanie na LivingEntity
					.forEach(MobFixer::handlePotentiallyBuggedMob); // Obsługa każdego moba
		});
		Utils.benchmarkEnd(bm, "checkAndFixMobs");
		working = false;
		print.debug("Przed usunieciem cache: " + MobInfo.getSize());
		MobInfo.clearUnused();
		print.debug("PO usunieciem cache: " + MobInfo.getSize());
	}


	/**
	 * Sprawdza i naprawia potencjalnie zbugowanego moba.
	 */
	public static void handlePotentiallyBuggedMob(LivingEntity mob) {
		MobInfo mi = MobInfo.get(mob);
		mi.setLastChecked(Timestamp.from(Instant.now()));
		if (isBugged(mob)) {
			mi.setMobWarns(mi.getMobWarns() + 1);
			//print.info("Wykryto niezniszczalnego moba: " + mob.getType() + " w " + mob.getLocation());
			if (mi.getMobWarns() > 5) {
				respawnMob(mob);
			}
		} else {
			mi.setMobGood(true);
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

		MobInfo mi = MobInfo.get(mob);
		if (mi.isMobGood()) {
			return false;
		}
		if (mob.isInvulnerable()) {
			return false;
		}

		double originalHealth = mob.getHealth();
		if (originalHealth < 2.0) {
			return false;
		}
		//print.info("Health" + originalHealth);
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
		MobInfo.free(mob);
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
		if (target instanceof Player) {
			player.sendMessage(Utils.mm("<gray>To jest gracz/npc."));
			return;
		}
		if (target instanceof LivingEntity mob) {
			if (isBugged(mob) || force) {
				respawnMob(mob);
				player.sendMessage(Utils.mm("<#00FF00>Zrespawnowano (naprawiono?) moba: " + mob.getType()));
			} else {
				player.sendMessage(Utils.mm("<red>Ten mob nie jest zbugowany: " + mob.getType()));
			}
		} else {
			player.sendMessage(Utils.mm("<gray>Nie patrzysz na moba!"));
		}
	}

	public static boolean isWorking() {
		return working;
	}
}
