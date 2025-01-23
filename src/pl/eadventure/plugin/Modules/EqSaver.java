package pl.eadventure.plugin.Modules;

import com.sk89q.worldedit.world.item.ItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.core.net.Priority;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EqSaver {
	Plugin plugin;
	MySQLStorage storage;
	EqSaverListener listener;

	public EqSaver(Plugin plugin, MySQLStorage storage) {
		this.plugin = plugin;
		this.storage = storage;
		listener = new EqSaverListener();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
	}

	//***************************************************************************************************LISTENERS
	public static class EqSaverListener implements Listener {
		//----------------------------------------------JOIN
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerJoin(PlayerJoinEvent e) {
			print.debug("EqSaverListener - join: " + e.getPlayer().getName());
		}

		//----------------------------------------------LEAVE
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent e) {
			print.debug("EqSaverListener - leave: " + e.getPlayer().getName());
			//saveInventoryToFile(e.getPlayer().getInventory().getContents(), e.getPlayer().getName());
			Player player = e.getPlayer();
			gVar.eqSaver.taskSaveInventory(player, player.getInventory().getContents(), "leave");
		}
	}

	//***************************************************************************************************TASK SAVE INVENTORY
	public void taskSaveInventory(Player player, ItemStack[] inventory, String event) {
		String uuid = player.getUniqueId().toString();
		String playerName = player.getName();
		new BukkitRunnable() {
			@Override
			public void run() {
				StringBuilder infoBuilder = new StringBuilder();

				for (ItemStack item : inventory) {
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
				//query
				ArrayList<Object> parameters = new ArrayList<>();
				parameters.add(uuid);
				parameters.add(event);
				parameters.add(info);
				String sql = "INSERT INTO `invback` (`uuid`, `event`, `info`) VALUES (?, ?, ?);";
				int insertId = storage.executeGetInsertID(sql, parameters);
				//save file
				saveInventoryToFile(inventory, String.valueOf(insertId));
				//Log data
				String log;
				log = String.format("Zapis EQ gracza: %s. Zdarzenie: %s. Zarządzaj InGame: /eqs %d info:%s", playerName, event, insertId, info);
				ServerLogManager.log(log, ServerLogManager.LogType.Inventory);
			}
		}.runTaskAsynchronously(plugin);
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
