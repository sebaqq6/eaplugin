package pl.eadventure.plugin.FunEvents;

import dev.geco.gsit.api.GSitAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.FunEvents.Event.TestEvent;
import pl.eadventure.plugin.FunEvents.Event.StarcieEternal;
import pl.eadventure.plugin.Modules.GearScoreCalculator;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.io.File;
import java.util.*;

public class FunEventsManager {
	Plugin plugin;
	private static Map<String, FunEvent> events = new HashMap<>();//eventy
	private boolean records;//czy są zapisy
	private int recordsCountDown;//ilość czasu pozostała do zakończenia zapisów (sekundy)
	private int recordsCountDownMax;//max czas zapisów (sekundy)
	private BossBar bossBar;//bossbar
	private FunEvent actualFunEvent;//event aktualnie ogarniany przez menagera
	private FunEventManagerListeners listeners;
	static File fileRewards = new File("plugins/EternalAdventurePlugin/FunEventsRewards.yml");
	public static Location spawnLocation = new Location(Bukkit.getWorld("world"), 31, 169, -23);
	public static Location worldEvents = new Location(Bukkit.getWorld("world_utility"), 0, 59, 0);

	public FunEventsManager(Plugin plugin) {
		this.plugin = plugin;
		records = false;
		bossBar = Bukkit.createBossBar("_", BarColor.YELLOW, BarStyle.SEGMENTED_20);
		new BukkitRunnable() {
			@Override
			public void run() {
				oneSecondTimer();
			}
		}.runTaskTimer(plugin, 20L, 20L);
		listeners = new FunEventManagerListeners();
		Bukkit.getPluginManager().registerEvents(listeners, plugin);
		//Events
		registerEvents();
	}

	public void registerEvents() {
		events.clear();
		registerEvent("test", new TestEvent("Event Testowy", 1, 1, false));
		registerEvent("starcieeternal", new StarcieEternal("Starcie Eternal", 2, 30, true));
	}

	public boolean startRecord(String eventName, int recordsCountDown) {//rozpoczynanie zapisów
		if (!records) {
			FunEvent event = events.get(eventName);
			if (event == null) {
				return false;
			}
			if (event.getStatus() != FunEvent.Status.FREE) return false;
			event.setStatus(FunEvent.Status.RECORDS);
			records = true;
			this.recordsCountDownMax = recordsCountDown;
			this.recordsCountDown = recordsCountDown;
			this.actualFunEvent = event;
			oneSecondTimer();
			for (Player p : Bukkit.getOnlinePlayers()) {
				bossBar.addPlayer(p);
			}
			bossBar.setVisible(true);
			return true;
		}
		return false;
	}

	public boolean stopRecord() {//kończenei zapisów
		if (records) {
			records = false;
			bossBar.removeAll();
			bossBar.setVisible(false);
			recordsCountDown = 0;
			actualFunEvent = null;
			return true;
		}
		return false;
	}

	public boolean isRecords() {
		return records;
	}

	public void registerEvent(String name, FunEvent event) {
		events.put(name, event);
		//load rewards
		YamlConfiguration yamlRewards = YamlConfiguration.loadConfiguration(fileRewards);
		List<String> defaultCommands = Arrays.asList(
				"give %username% diamond 16",
				"give %username% netherite_ingot 1"
		);
		if (!fileRewards.exists()) {
			yamlRewards.set(name + ".win", defaultCommands);
			yamlRewards.set(name + ".lose", defaultCommands);
			Utils.saveConfig(fileRewards, yamlRewards);
		} else {
			//wczytanie nagród
			//win
			List<String> commandsWin = yamlRewards.getStringList(name + ".win");
			if (commandsWin != null && !commandsWin.isEmpty()) {
				event.rewardCommandsWin.addAll(commandsWin);
			} else {
				event.rewardCommandsWin.addAll(defaultCommands);
				yamlRewards.set(name + ".win", defaultCommands);
				Utils.saveConfig(fileRewards, yamlRewards);
				print.error("Nie znaleziono komend nagród wygranych dla: " + name);
			}
			//lose
			List<String> commandsLose = yamlRewards.getStringList(name + ".lose");
			if (commandsLose != null && !commandsLose.isEmpty()) {
				event.rewardCommandsLose.addAll(commandsLose);
			} else {
				event.rewardCommandsLose.addAll(defaultCommands);
				yamlRewards.set(name + ".lose", defaultCommands);
				Utils.saveConfig(fileRewards, yamlRewards);
				print.error("Nie znaleziono komend nagród przegranych dla: " + name);
			}
		}
	}//rejestracja eventy


	public static List<String> getEventKeysAsList() {
		return new ArrayList<>(events.keySet());
	}

	public FunEvent getActualFunEvent() {
		return actualFunEvent;
	}

	public FunEvent getEvent(String name) {
		return events.get(name);
	}//pobieranie eventu po nazwie

	public boolean registerPlayer(Player player) {//dodawanie gracza do eventu
		if (!records) return false;
		if (actualFunEvent.getPlayersCount() >= actualFunEvent.getMaxPlayers()) {
			return false;
		}
		if (actualFunEvent == null) {
			return false;
		}
		return actualFunEvent.addPlayer(player);
	}

	public boolean unregisterPlayer(Player player) {//usuwanie gracza z zapisów
		if (actualFunEvent != null) {
			return actualFunEvent.removePlayer(player);
		}
		return false;
	}

	int barTitleStep = 0;

	private void oneSecondTimer() {
		if (records) {
			if (recordsCountDown > 0) {
				double progressValue = (double) recordsCountDown / (double) recordsCountDownMax;
				bossBar.setProgress(progressValue);
				String title = String.format("&c \uD83D\uDDE1 &5&l%s &f&l- &a&l/dolacz &8&l(&7&l%d&8&l/&7&l%d&8&l)", actualFunEvent.getEventName(), actualFunEvent.getPlayersCount(), actualFunEvent.getMaxPlayers());
				/*switch (barTitleStep) {
					case 0, 1 -> title = String.format("&5&lZapisy na &6%s &d- &a/123", actualFunEvent.getEventName());
					case 2, 3 ->
							title = String.format("&aZapisało się &6&l%d/%d &aosób!", actualFunEvent.getPlayersCount(), actualFunEvent.getMaxPlayers());
				}
				barTitleStep++;
				if (barTitleStep > 3) barTitleStep = 0;*/
				//String.format("&d&lZapisy na &6%s &d- &a/event", actualFunEvent.getEventName());
				bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));

				if (recordsCountDown > 10 && actualFunEvent.getPlayers().size() >= actualFunEvent.getMaxPlayers()) {
					recordsCountDown -= 10;
				} else {
					recordsCountDown--;
				}
			} else {
				if (actualFunEvent.getPlayersCount() < actualFunEvent.getMinPlayers()) {//zbyt mało osób
					actualFunEvent.msgAll(String.format("<grey>Niestety, na <blue><bold>%s</bold></blue> zapisało się zbyt mało osób, aby mogło się odbyć.", actualFunEvent.getEventName()));
					actualFunEvent.setStatus(FunEvent.Status.FREE);
				} else {
					actualFunEvent.setStatus(FunEvent.Status.IN_PROGRESS);
					actualFunEvent.clearPlayersVariables();//clear players variables
					actualFunEvent.saveEqBeforeJoinForAll();//save player eq
					actualFunEvent.start();
				}
				stopRecord();
			}
		}
	}

	public static boolean inventoryHasOnlySet(Player player) {
		final int MAX_ARMORS = 4;
		final int MAX_MAINHAND = 2;
		final int MAX_OFFHAND = 1;
		final int MAX_OTHERITEMS = 0;
		//check valid inventory
		ItemStack[] playerInventory = player.getInventory().getContents();
		ItemStack mobArenaTicket = gVar.customItems.get("mobArenaTicket");
		int armors = 0;
		int mainhands = 0;
		int offhands = 0;
		int otheritems = 0;
		for (ItemStack item : playerInventory) {
			if (item == null) continue;
			if (mobArenaTicket.isSimilar(item)) continue;
			Material type = item.getType();
			if (type == Material.ARROW || type == Material.SPECTRAL_ARROW || type == Material.TIPPED_ARROW)
				continue;
			GearScoreCalculator gsc = new GearScoreCalculator(null);
			String itemType = gsc.getItemType(item);
			if (gsc.getItemType(item) == null) {
				otheritems++;
				continue;
			}
			//player.sendMessage(item.getItemMeta().getDisplayName() + ":" + gsc.getItemType(item));
			if (itemType.equalsIgnoreCase("armor")) {
				armors++;
			} else if (itemType.equalsIgnoreCase("mainhand")) {
				mainhands++;
			} else if (itemType.equalsIgnoreCase("offhand")) {
				//offhands++;
				mainhands++;
			} else if (itemType.equalsIgnoreCase("default")) {
				otheritems++;
			}
		}
		if (armors > MAX_ARMORS || mainhands > MAX_MAINHAND || offhands > MAX_OFFHAND || otheritems > MAX_OTHERITEMS) {
			player.sendMessage(Utils.mm("<red><bold>Twoje wyposażenie przekracza limity, odłóż zbędny sprzęt:"));
			player.sendMessage(Utils.mm(String.format("<bold><gray>Pancerz: <%s>%d/%d", armors > MAX_ARMORS ? "#FF0000" : "#00FF00", armors, MAX_ARMORS)));
			player.sendMessage(Utils.mm(String.format("<bold><gray>Broń: <%s>%d/%d", mainhands > MAX_MAINHAND ? "#FF0000" : "#00FF00", mainhands, MAX_MAINHAND)));
			//player.sendMessage(Utils.mm(String.format("<bold><gray>Leworęczny przedmiot: <%s>%d/%d", offhands > MAX_OFFHAND ? "#FF0000" : "#00FF00", offhands, MAX_OFFHAND)));
			player.sendMessage(Utils.mm(String.format("<bold><gray>Pozostałe przedmioty: <%s>%d/%d", otheritems > MAX_OTHERITEMS ? "#FF0000" : "#00FF00", otheritems, MAX_OTHERITEMS)));
			return false;
		}
		return true;
	}

	//----------------------------------STATIC SECTION------------------------------------------------------------------
	public static FunEvent isPlayerSavedOnEvent(Player player) {
		for (Map.Entry<String, FunEvent> eventMap : events.entrySet()) {
			FunEvent event = eventMap.getValue();
			if (event.getStatus() == FunEvent.Status.RECORDS) {
				if (event.isPlayerOnEvent(player)) {
					return event;
				}
			}
		}
		return null;
	}

	public static FunEvent isPlayerOnEvent(Player player) {
		for (Map.Entry<String, FunEvent> eventMap : events.entrySet()) {
			FunEvent event = eventMap.getValue();
			if (event.getStatus() == FunEvent.Status.IN_PROGRESS) {
				if (event.isPlayerOnEvent(player)) {
					return event;
				}
			}
		}
		return null;
	}

	//---------------------------------------------------------------------------------------------------------LISTENERS
	public static class FunEventManagerListeners implements Listener {
		//==============================JOIN TO THE SERVER=============
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			if (funEventManager.isRecords()) {
				funEventManager.bossBar.addPlayer(player);
			}
		}

		//==============================QUIT FROM THE SERVER===========
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			FunEvent funEvent = isPlayerOnEvent(player);
			if (funEvent != null) {//in progress
				funEvent.getEvPlayer(player).restoreEqBeforeJoin();//restore eq before join to the event
				funEvent.removePlayer(player);
				player.teleport(spawnLocation);
				funEvent.playerQuit(player);
			} else {//only saved on event
				funEvent = isPlayerSavedOnEvent(player);
				if (funEvent != null) {
					funEvent.removePlayer(player);
				}
			}
		}

		//==============================DEATH=============
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			FunEvent funEvent = isPlayerOnEvent(player);
			if (funEvent != null) {
				funEvent.playerDeath(e);
			}
		}

		//==============================RESPAWN===========
		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			FunEvent funEvent = isPlayerOnEvent(player);
			if (funEvent != null) {
				Bukkit.getScheduler().runTaskLater(funEventManager.plugin, r -> funEvent.playerRespawn(e), 20L);
			} else {
				if (worldEvents.getWorld().equals(e.getRespawnLocation().getWorld())) {
					if (worldEvents.distance(e.getRespawnLocation()) <= 1) {
						Bukkit.getScheduler().runTaskLater(funEventManager.plugin, r -> player.teleport(spawnLocation), 20L);
						print.error("Gracz " + player.getName() + " zrespawnował się na spawnie  " + worldEvents.getWorld().getName() + ". Teleportuje go na główny spawn w world.");
					}
				}

			}
		}

		//==============================DROP ITEM===========
		@EventHandler
		public void onPlayerDrop(PlayerDropItemEvent e) {
			Player player = e.getPlayer();
			FunEvent funEvent = isPlayerSavedOnEvent(player);
			if (funEvent != null && funEvent.isOwnSet()) {
				player.sendMessage(Utils.mm("<grey>Jesteś zapisany/a na event - czynność niedozwolona."));
				e.setCancelled(true);
			}
		}

		//==============================INVENTORY OPEN===========
		@EventHandler
		public void onPlayerInventoryOpen(InventoryOpenEvent e) {
			if (e.getPlayer() instanceof Player player) {
				FunEvent funEvent = isPlayerSavedOnEvent(player);
				if (funEvent != null && funEvent.isOwnSet()) {
					player.sendMessage(Utils.mm("<grey>Jesteś zapisany/a na event - czynność niedozwolona."));
					e.setCancelled(true);
				}
			}
		}

		//=============================PICKUP ITEM===========
		@EventHandler
		public void onPlayerPickupItem(EntityPickupItemEvent e) {
			if (e.getEntity() instanceof Player player) {
				FunEvent funEvent = isPlayerSavedOnEvent(player);
				if (funEvent != null && funEvent.isOwnSet()) {
					e.setCancelled(true);
				}
			}
		}

		//=============================INTERACT ENTITY==========
		@EventHandler
		public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
			Player player = e.getPlayer();
			FunEvent funEvent = isPlayerSavedOnEvent(player);
			if (e.getRightClicked() instanceof ItemFrame || e.getRightClicked() instanceof LivingEntity) {
				if (funEvent != null && funEvent.isOwnSet()) {
					player.sendMessage(Utils.mm("<grey>Jesteś zapisany/a na event - czynność niedozwolona."));
					e.setCancelled(true);
				}
			}
		}

		//=============================COMMAND===========
		@EventHandler
		public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			String rawData = e.getMessage();
			String[] args = rawData.split(" ");
			String command = args[0];
			//block ah when player is saved on event
			if (isPlayerSavedOnEvent(player) != null && command.equalsIgnoreCase("/ah")) {
				player.sendMessage(Utils.mm("<grey>Jesteś zapisany/a na event - czynność niedozwolona."));
				e.setCancelled(true);
				return;
			}
			//block commands on event
			/*if (isPlayerOnEvent(player) != null
					&& !command.equalsIgnoreCase("/playerhiddencmdspawnsoundtrack")
					&& !command.equalsIgnoreCase("/playerhiddencmdspawnsoundtrackstop") && !player.isOp()) {
				player.sendMessage(Utils.mm("<grey>Nie możesz używać tutaj komend."));
				e.setCancelled(true);
			}*/
		}
	}
}
