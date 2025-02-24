package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.Utils.wgAPI;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
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
			//long expire = now.getTime() - 2 * 60 * 60 * 1000; // hours * min * x * x;
			long expire = now.getTime() - 10 * 1000; // hours * min * x * x;
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
		/*working = false;

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
		working = false;*/
		if (!autoModeEnabled) return;
		//utility world checker
		World worldUtility = Bukkit.getWorld("world_utility");
		if (worldUtility != null) {
			worldUtility.getEntities().stream()
					.filter(entity -> entity instanceof LivingEntity) // Filtrujemy tylko LivingEntity
					.filter(entity -> !(entity instanceof Player)) // Pomijamy graczy
					.filter(entity -> !(entity instanceof ArmorStand)) // Pomijamy ArmorStandy
					.map(entity -> (LivingEntity) entity) // Rzutowanie na LivingEntity
					.forEach(MobFixer::handlePotentiallyBuggedMob); // Obsługa każdego mob;
		}

		//other
		print.debug("Przed usunieciem cache: " + MobInfo.getSize());
		MobInfo.clearUnused();
		print.debug("PO usunieciem cache: " + MobInfo.getSize());
	}

	public static void timerFixTargetMob(Player player) {
		if (!autoModeEnabled) return;
		Entity targetMob = player.getTargetEntity(10);
		if (targetMob instanceof LivingEntity livingEntity) {
			MobInfo mi = MobInfo.get(livingEntity);
			EntityType type = livingEntity.getType();
			if (type == EntityType.ENDER_DRAGON
					|| type == EntityType.CAT
					|| type == EntityType.PARROT
					|| type == EntityType.WOLF) {
				mi.setMobGood(true);
				return;
			}
			print.debug(player.getName() + " target fix mob: " + targetMob.getType());
			handlePotentiallyBuggedMob(livingEntity);
		}
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
			if (mi.getMobWarns() >= 1) {
				respawnMob(mob);
			}
		} else {
			//mi.setMobGood(true);
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
			print.debug("isBugged: good");
			return false;
		}
		if (mob.isInvulnerable()) {
			print.debug("isBugged: isInvulnerable");
			return false;
		}

		if (mob instanceof Player) {
			mi.setMobGood(true);
			print.debug("isBugged: Player");
			return false;
		}

		if (mob instanceof ArmorStand) {
			mi.setMobGood(true);
			print.debug("isBugged: ArmorStand");
			return false;
		}

		if (wgAPI.isOnRegion(mob, "_spawn_") || wgAPI.isOnRegion(mob, "_spawn2_")) {
			mi.setMobGood(true);
			print.debug("isBugged: spawn?");
			return false;
		}

		double originalHealth = mob.getHealth();
		if (originalHealth < 2.0) {
			print.debug("isBugged: originalHealth: " + originalHealth);
			return false;
		}
		//print.info("Health" + originalHealth);
		working = true;
		mob.damage(1); // Próba zadania 1 punktu obrażeń
		boolean bugged = mob.getHealth() == originalHealth; // Jeśli zdrowie się nie zmienia, mob może być "zbugowany"
		mob.setHealth(originalHealth); // Przywracamy oryginalne zdrowie
		working = false;

		return bugged;

	}

	/**
	 * Respawnuje zbugowanego moba, tworząc go na nowo w tej samej lokalizacji.
	 */
	private static void respawnMob(LivingEntity mob) {
		Location location = mob.getLocation();
		EntityType type = mob.getType();
		EntityEquipment equipment = mob.getEquipment();

		// Uniwersalne właściwości
		String customName = mob.getCustomName();
		boolean nameVisible = mob.isCustomNameVisible();
		boolean isGlowing = mob.isGlowing();
		boolean isInvisible = mob.isInvisible();
		boolean isAI = mob.hasAI();
		boolean isSilent = mob.isSilent();
		boolean isCollidable = mob.isCollidable();
		boolean isInvulnerable = mob.isInvulnerable();
		double health = mob.getHealth();
		double maxHealth = mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();

		// Efekty mobów
		Collection<PotionEffect> activeEffects = mob.getActivePotionEffects();

		// Oswajalne moby
		boolean isTamed = false;
		UUID owner = null;
		boolean isSitting = false;

		// Specyficzne dla wilków
		DyeColor collarColor = null;
		Wolf.Variant wolfVariant = null;

		// Specyficzne dla kotów
		Cat.Type catType = null;

		// Specyficzne dla owiec
		DyeColor sheepColor = null;
		boolean isSheared = false;

		// Specyficzne dla axolotli
		Axolotl.Variant axolotlVariant = null;

		// Specyficzne dla papug
		Parrot.Variant parrotVariant = null;
		boolean isTamedParrot = false;

		// Specyficzne dla koni, osłów i wielbłądów
		boolean isTamedHorse = false;
		Horse.Variant horseVariant = null;
		boolean isHorseJumping = false;
		double horseJumpStrength = 0;
		Horse.Color horseColor = null;
		Horse.Style horseStyle = null;
		boolean isDonkey = false;
		boolean isCamel = false;

		// Oswajalność i specyficzne właściwości
		if (mob instanceof Tameable) {
			Tameable tameable = (Tameable) mob;
			isTamed = tameable.isTamed();
			if (isTamed && tameable.getOwner() != null) {
				owner = tameable.getOwner().getUniqueId();
			}
			if (mob instanceof Wolf) {
				Wolf wolf = (Wolf) mob;
				isSitting = wolf.isSitting();
				collarColor = wolf.getCollarColor();
				wolfVariant = wolf.getVariant();
			} else if (mob instanceof Cat) {
				Cat cat = (Cat) mob;
				isSitting = cat.isSitting();
				catType = cat.getCatType();
			} else if (mob instanceof Parrot) {
				Parrot parrot = (Parrot) mob;
				isTamedParrot = parrot.isTamed();
				isSitting = parrot.isSitting();
				parrotVariant = parrot.getVariant();
			}
		}

		// Owiec
		if (mob instanceof Sheep) {
			Sheep sheep = (Sheep) mob;
			sheepColor = sheep.getColor();
		}

		// Axolotl
		if (mob instanceof Axolotl) {
			Axolotl axolotl = (Axolotl) mob;
			axolotlVariant = axolotl.getVariant();
		}

		// Zombie / oskryptowane humanoidy
		boolean isBaby = false;
		Villager.Profession villagerProfession = null;

		if (mob instanceof Zombie) {
			Zombie zombie = (Zombie) mob;
			isBaby = zombie.isBaby();
		}
		if (mob instanceof Villager) {
			Villager villager = (Villager) mob;
			villagerProfession = villager.getProfession();
		}

		// Konie, osły, wielbłądy
		if (mob instanceof Horse) {
			Horse horse = (Horse) mob;
			isTamedHorse = horse.isTamed();
			horseVariant = horse.getVariant();
			isHorseJumping = horse.isJumping();
			horseJumpStrength = horse.getJumpStrength();
			horseColor = horse.getColor();
			horseStyle = horse.getStyle();
		} else if (mob instanceof Donkey) {
			isDonkey = true;
		} else if (mob instanceof Camel) {
			isCamel = true;
		}

		// Usuń starego moba
		mob.remove();

		// Stwórz nowego moba
		World world = location.getWorld();
		if (world != null) {
			LivingEntity newMob = (LivingEntity) world.spawnEntity(location, type);

			// Przywróć ekwipunek
			if (equipment != null) {
				newMob.getEquipment().setArmorContents(equipment.getArmorContents());
				newMob.getEquipment().setItemInMainHand(equipment.getItemInMainHand());
				newMob.getEquipment().setItemInOffHand(equipment.getItemInOffHand());
			}

			// Uniwersalne właściwości
			newMob.setCustomName(customName);
			newMob.setCustomNameVisible(nameVisible);
			newMob.setGlowing(isGlowing);
			newMob.setInvisible(isInvisible);
			newMob.setAI(isAI);
			newMob.setSilent(isSilent);
			newMob.setCollidable(isCollidable);
			newMob.setInvulnerable(isInvulnerable);
			newMob.setHealth(Math.min(health, maxHealth));

			// Przywróć efekty
			for (PotionEffect effect : activeEffects) {
				newMob.addPotionEffect(effect);
			}

			// Specyficzne dla oswajalnych mobów
			if (newMob instanceof Tameable) {
				Tameable tameable = (Tameable) newMob;
				tameable.setTamed(isTamed);
				if (isTamed && owner != null) {
					tameable.setOwner(Bukkit.getOfflinePlayer(owner));
				}
				if (newMob instanceof Wolf) {
					Wolf wolf = (Wolf) newMob;
					wolf.setSitting(isSitting);
					if (collarColor != null) wolf.setCollarColor(collarColor);
					if (wolfVariant != null) wolf.setVariant(wolfVariant);
				} else if (newMob instanceof Cat) {
					Cat cat = (Cat) newMob;
					cat.setSitting(isSitting);
					if (catType != null) cat.setCatType(catType);
				} else if (newMob instanceof Parrot) {
					Parrot parrot = (Parrot) newMob;
					parrot.setTamed(isTamedParrot);
					parrot.setSitting(isSitting);
					if (parrotVariant != null) parrot.setVariant(parrotVariant);
				}
			}

			// Owiec
			if (newMob instanceof Sheep) {
				Sheep sheep = (Sheep) newMob;
				if (sheepColor != null) sheep.setColor(sheepColor);
				//sheep.setSheared(isSheared);
			}

			// Axolotl
			if (newMob instanceof Axolotl) {
				Axolotl axolotl = (Axolotl) newMob;
				if (axolotlVariant != null) axolotl.setVariant(axolotlVariant);
			}

			// Papuga
			if (newMob instanceof Parrot) {
				Parrot parrot = (Parrot) newMob;
				parrot.setTamed(isTamedParrot);
				parrot.setSitting(isSitting);
				if (parrotVariant != null) parrot.setVariant(parrotVariant);
			}

			// Zombie
			if (newMob instanceof Zombie) {
				Zombie zombie = (Zombie) newMob;
				zombie.setBaby(isBaby);
			}

			// Osadnicy
			if (newMob instanceof Villager) {
				Villager villager = (Villager) newMob;
				if (villagerProfession != null) villager.setProfession(villagerProfession);
			}

			// Konie, osły, wielbłądy
			if (newMob instanceof Horse) {
				Horse horse = (Horse) newMob;
				horse.setTamed(isTamedHorse);
				horse.setVariant(horseVariant);
				horse.setJumping(isHorseJumping);
				horse.setJumpStrength(horseJumpStrength);
				horse.setColor(horseColor);
				horse.setStyle(horseStyle);
			} else if (newMob instanceof Donkey) {
				Donkey donkey = (Donkey) newMob;
				donkey.setTamed(isTamedHorse);
			} else if (newMob instanceof Camel) {
				Camel camel = (Camel) newMob;
				camel.setTamed(isTamedHorse);
			}

			// Logowanie
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
