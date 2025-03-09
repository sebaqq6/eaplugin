package pl.eadventure.plugin.Modules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.ProtocolLibAPI;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MagicGUI;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class EqSaver {
	Plugin plugin;
	MySQLStorage storage;
	EqSaverListener listener;
	static ItemStack hBlackX = gVar.customItems.get("hBlackX");
	private final Map<Player, LinkedList<ItemStack[]>> inventoryHistory = new HashMap<>();

	public EqSaver(Plugin plugin, MySQLStorage storage) {
		this.plugin = plugin;
		this.storage = storage;
		listener = new EqSaverListener();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
		new BukkitRunnable() {
			@Override
			public void run() {
				trackInventoryLast();
				taskRestoreInventory();
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L);
	}

	//***************************************************************************************************LISTENERS
	public static class EqSaverListener implements Listener {
		//----------------------------------------------DEATH
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerDeath(PlayerDeathEvent e) {
			Player player = e.getPlayer();
			EntityDamageEvent lastDamage = player.getLastDamageCause();
			String reason = "nieznany";
			String killer = "brak";
			//who kill
			if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
				Entity damager = entityEvent.getDamager();

				if (damager instanceof Player playerKiller) {
					killer = playerKiller.getName();
				} else if (damager instanceof LivingEntity mob) {
					killer = mob.getName();
				} else {
					killer = damager.getType().toString();
				}
			}
			//reason
			if (lastDamage != null) {
				reason = lastDamage.getCause().toString();
			}
			//add _ for spaces killer
			killer = killer.replaceAll(" ", "#");
			gVar.eqSaver.taskSaveInventory(player, player.getInventory().getContents(), "death_{" + reason + "}_{" + killer + "}");
		}

		//----------------------------------------------RESPAWN
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerRespawn(PlayerRespawnEvent e) {
			Player player = e.getPlayer();
			new BukkitRunnable() {
				@Override
				public void run() {
					gVar.eqSaver.taskSaveInventory(player, player.getInventory().getContents(), "respawn");
				}
			}.runTaskLater(EternalAdventurePlugin.getInstance(), 10L);
		}

		//----------------------------------------------CHANGE WORLD
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
			Player player = e.getPlayer();
			gVar.eqSaver.taskSaveInventory(player, player.getInventory().getContents(), "change_world");
		}

		//----------------------------------------------LEAVE
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent e) {
			Player player = e.getPlayer();
			EqSaver eqSaver = gVar.eqSaver;
			eqSaver.taskSaveInventory(player, player.getInventory().getContents(), "leave");
			int lastSeen = (int) ProtocolLibAPI.getLastSeen(e.getPlayer().getUniqueId());
			if (lastSeen > 0 && lastSeen < 60) {
				eqSaver.taskSaveInventory(player, eqSaver.getTrackedInv(player, lastSeen), lastSeen + "s_before_leave");
			} else {
				eqSaver.taskSaveInventory(player, eqSaver.getTrackedInv(player, 1), "1s_before_leave");
			}
			eqSaver.taskSaveInventory(player, eqSaver.getTrackedInv(player, 35), "35s_before_leave");
		}
	}

	//***************************************************************************************************TASK SAVE INVENTORY
	public void taskSaveInventory(Player player, ItemStack[] inventory, String event) {
		if (inventory == null) return;
		String uuid = player.getUniqueId().toString();
		String playerName = player.getName();
		Location location = player.getLocation();
		ItemStack[] snapshot = Arrays.stream(inventory)
				.map(item -> item != null ? item.clone() : null)
				.toArray(ItemStack[]::new);
		new BukkitRunnable() {
			@Override
			public void run() {
				StringBuilder infoBuilder = new StringBuilder();

				for (ItemStack item : snapshot) {
					if (item != null && item.getType() != Material.AIR) {
						// Pobierz komponent tekstowy
						Component displayName = item.displayName();
						if (displayName != null) {
							if (infoBuilder.length() > 0) {
								infoBuilder.append(", ");
							}
							infoBuilder.append(PlainTextComponentSerializer.plainText().serialize(displayName));
							infoBuilder.append("x").append(item.getAmount());
						}
					}
				}
				String info = infoBuilder.toString(); // Konwertuj StringBuilder na String
				//null invent?
				if (info.length() < 1) {
					info = "[Pusty ekwipunek]x0";
				}
				//query
				ArrayList<Object> parameters = new ArrayList<>();
				parameters.add(uuid);
				parameters.add(event);
				parameters.add(info);
				String worldName = location.getWorld().getName();
				parameters.add(String.format("%.2f, %.2f, %.2f, %s", location.getX(), location.getY(), location.getZ(), worldName));
				String sql = "INSERT INTO `invback` (`uuid`, `event`, `info`, `location`) VALUES (?, ?, ?, ?);";
				int insertId = storage.executeGetInsertID(sql, parameters);
				//save file
				saveInventoryToFile(snapshot, String.valueOf(insertId));
				//Log data
				String log;
				log = String.format("Zapis EQ gracza: %s. Zdarzenie: %s. Lokalizacja: %.2f, %.2f, %.2f, %s. %s /eqs %d info:%s", playerName, event, location.getX(), location.getY(), location.getZ(), worldName, Utils.getCurrentDate(), insertId, info);
				ServerLogManager.log(log, ServerLogManager.LogType.Inventory);
			}
		}.runTaskAsynchronously(plugin);
	}

	//************************************************************************************TASK HISTORY INVENT LAST MINUTE
	private void trackInventoryLast() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			inventoryHistory.putIfAbsent(player, new LinkedList<>());
			LinkedList<ItemStack[]> history = inventoryHistory.get(player);

			if (history.size() >= 60) {
				history.removeFirst(); // Usuwamy najstarszy zapis
			}
			ItemStack[] snapshot = Arrays.stream(player.getInventory().getContents())
					.map(item -> item != null ? item.clone() : null)
					.toArray(ItemStack[]::new);

			history.add(snapshot);
		}
	}

	public ItemStack[] getTrackedInv(Player player, int lastSeconds) {
		LinkedList<ItemStack[]> history = inventoryHistory.get(player);

		if (history == null || lastSeconds <= 0 || lastSeconds > history.size()) {
			return null;
		}

		return history.get(history.size() - lastSeconds - 1);
	}

	public void debugInventoryHistory(CommandSender sender) {
		print.info("----------------------------");
		if (sender instanceof Player player) {
			for (int x = 1; x < 60; x++) {

				ItemStack[] eq = getTrackedInv(player, x);
				if (eq == null) {
					print.info(x + ": Brak danych");
					continue;
				}
				StringBuilder infoBuilder = new StringBuilder();
				for (ItemStack item : eq) {
					if (item != null && item.getType() != Material.AIR) {
						Component displayName = item.displayName();
						if (displayName != null) {
							if (infoBuilder.length() > 0) {
								infoBuilder.append(", ");
							}
							infoBuilder.append(PlainTextComponentSerializer.plainText().serialize(displayName));
						}
					}
				}
				String info = infoBuilder.toString();
				print.info(x + " sekund temu: " + info);
			}
			print.info("----------------------------");
		}
	}

	//**************************************************************************************************RESTORE EQ TASK
	private void taskRestoreInventory() {
		String sql = "SELECT * FROM invback WHERE status = 1;";
		storage.query(sql, queryResult ->
		{
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					int id = (int) rows.get(i).get("id");
					String uuid = (String) rows.get(i).get("uuid");
					Timestamp dateTimestamp = (Timestamp) rows.get(i).get("date");
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String date = sdf.format(dateTimestamp);
					//restore eq...
					Player player = Bukkit.getPlayer(UUID.fromString(uuid));
					if (player != null && player.isOnline()) {
						taskSaveInventory(player, player.getInventory().getContents(), "before_restore");

						player.getInventory().clear();
						ItemStack[] backup = loadInventoryFromFile(String.valueOf(id));
						player.getInventory().setContents(backup);
						//update db
						storage.execute("UPDATE invback SET status = 2 WHERE id = " + id);
						//log
						new BukkitRunnable() {
							@Override
							public void run() {
								print.info(String.format("Przywrócono EQ gracza %s (/eqs %d)", player.getName(), id));
								String log = String.format("Zapis EQ gracza: %s - Przywrócono ekwipunek (/eqs %d).", player.getName(), id);
								ServerLogManager.log(log, ServerLogManager.LogType.Inventory);
							}
						}.runTaskLaterAsynchronously(plugin, 20L);
					}
				}
			}
		});
	}

	//**************************************************************************************************GUI
	public void showEqsGUI(Player player, int eqid) {
		MagicGUI mainGui = MagicGUI.create(Utils.mm("<bold><#FF0000>/eqs " + eqid), 54);
		mainGui.setAutoRemove(true);
		ItemStack[] items = loadInventoryFromFile(String.valueOf(eqid));
		for (int i = 0; i < items.length; i++) {
			final int index = i;
			ItemStack item = items[i];
			if (item == null) {
				mainGui.setItem(i, ItemStack.of(Material.AIR));
			} else {
				mainGui.setItem(i, item, ((player1, gui, slot, type) -> {
					if (type.isShiftClick()) {
						items[index] = null;
						player1.getInventory().addItem(item);
						mainGui.setUpdateItem(index, ItemStack.of(Material.AIR));
						saveInventoryToFile(items, String.valueOf(eqid));
						storage.execute("UPDATE invback SET status = 200 WHERE id = " + eqid);
						Component displayName = item.displayName();
						String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
						String log = String.format("Admin o nicku: %s wyciągnął przedmiot %sx%d z (/eqs %d).", player1.getName(), itemName, item.getAmount(), eqid);
						ServerLogManager.log(log, ServerLogManager.LogType.Inventory);
					}
				}));
			}
		}
		//menu
		for (int m = 45; m <= 53; m++) {
			mainGui.setItem(m, Utils.itemWithDisplayName(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE), " ", null));
		}
		//info
		ArrayList<Component> description = new ArrayList<>();
		description.add(Utils.mm("<!i><gray>Aby wyciągnąć przedmiot użyj <red><bold>SHIFT+LPM</bold></red>."));
		description.add(Utils.mm(""));
		description.add(Utils.mm("<!i><gray>Jeśli jakikolwiek przedmiot zostanie wyciągnięty"));
		description.add(Utils.mm("<!i><gray>nie będzie go można przywrócić. Pozostanie ręczne"));
		description.add(Utils.mm("<!i><gray>wyciągniecie przedmiotów i użycie <bold>/invsee</bold> do"));
		description.add(Utils.mm("<!i><gray>umieszczenia ich w ekwipunku gracza."));
		ItemStack infoItem = Utils.itemWithDisplayName(gVar.customItems.get("hInfo"), Utils.mm("<yellow><bold><!i>Informacja"), description);
		mainGui.setItem(45, infoItem);//Info
		//end info
		ItemStack exitButton = Utils.itemWithDisplayName(hBlackX, Utils.color("&r&7&lZamknij"), null);
		mainGui.setItem(49, exitButton, ((player1, gui, slot, type) -> {
			mainGui.close(player1);
		}));
		mainGui.open(player);
	}

	//**************************************************************************************************UTILS
	public static void saveInventoryToFile(ItemStack[] inventory, String fileName) {
		File folder = new File("plugins/EternalAdventurePlugin/inventory_backups");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		File file = new File(folder, fileName + ".yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

		config.set("inventory", inventory);

		try {
			config.save(file);
		} catch (IOException e) {
			print.error("Wystąpił nieoczekiwany błąd:");
			e.printStackTrace();
		}
	}


	public static ItemStack[] loadInventoryFromFile(String fileName) {
		File folder = new File("plugins/EternalAdventurePlugin/inventory_backups");
		if (!folder.exists()) {
			folder.mkdirs();
			return new ItemStack[0];
		}
		File file = new File(folder, fileName + ".yml");
		if (!file.exists()) {
			print.error("loadInventoryFromFile: nie znaleziono pliku: " + fileName);
			return new ItemStack[0];
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

		List<ItemStack> itemList = (List<ItemStack>) config.get("inventory");
		return itemList != null ? itemList.toArray(new ItemStack[0]) : new ItemStack[0];
	}
}
