package pl.eadventure.plugin.FunEvents;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.FunEvents.Event.TestEvent;
import pl.eadventure.plugin.FunEvents.Event.WarGangs;
import pl.eadventure.plugin.Modules.GearScoreCalculator;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.HashMap;
import java.util.Map;

public class FunEventsManager {
	Plugin plugin;
	private static Map<String, FunEvent> events = new HashMap<>();//eventy
	private boolean records;//czy są zapisy
	private int recordsCountDown;//ilość czasu pozostała do zakończenia zapisów (sekundy)
	private int recordsCountDownMax;//max czas zapisów (sekundy)
	private BossBar bossBar;//bossbar
	private FunEvent actualFunEvent;//event aktualnie ogarniany przez menagera
	private FunEventManagerListeners listeners;
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
		registerEvent("wg", new WarGangs("Wojna Gangów", 2, 20, true));
		registerEvent("test", new TestEvent("Event Testowy", 1, 1, false));
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
	}//rejestracja eventy

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
				String title = "_";
				switch (barTitleStep) {
					case 0, 1 -> title = String.format("&5&lZapisy na &6%s &d- &a/123", actualFunEvent.getEventName());
					case 2, 3 ->
							title = String.format("&aZapisało się już &6&l%d &aosób!", actualFunEvent.getPlayersCount());
				}
				//String.format("&d&lZapisy na &6%s &d- &a/event", actualFunEvent.getEventName());
				bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
				recordsCountDown--;
				barTitleStep++;
				if (barTitleStep > 3) barTitleStep = 0;
			} else {
				if (actualFunEvent.getPlayersCount() < actualFunEvent.getMinPlayers()) {//zbyt mało osób
					actualFunEvent.msgAll(String.format("<grey>Niestety, na <blue><bold>%s</bold></blue> zapisało się <bold>zbyt mało osób</bold>, aby mogło się odbyć.", actualFunEvent.getEventName()));
					actualFunEvent.setStatus(FunEvent.Status.FREE);
				} else {
					actualFunEvent.setStatus(FunEvent.Status.IN_PROGRESS);
					actualFunEvent.clearPlayersVariables();//clear players variables
					actualFunEvent.saveEqBeforeJoinForAll();//save player variables
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
				funEvent.getEvPlayer(player).restoreEqBeforeJoin();
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
				if (worldEvents.getWorld().equals(player.getLocation().getWorld())) {
					if (worldEvents.distance(e.getRespawnLocation()) <= 1) {
						Bukkit.getScheduler().runTaskLater(funEventManager.plugin, r -> player.teleport(spawnLocation), 20L);
						print.error("Gracz " + player.getName() + " zrespawnował się na spawnie  " + worldEvents.getWorld().getName() + ". Teleportuje go na główny spawn w world.");
					}
				}

			}
		}

		//==============================COMMAND===========
		@EventHandler
		public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
			String rawData = e.getMessage();
			String[] args = rawData.split(" ");
			String command = args[0];
			if (funEventManager.isRecords()) {
				if (command.equalsIgnoreCase("/123")) {
					if (funEventManager.actualFunEvent.isOwnSet()) {
						if (!inventoryHasOnlySet(player)) {
							e.setCancelled(true);
							return;
						}
					}
					if (funEventManager.registerPlayer(player)) {
						Component message = Utils.mm(String.format("" +
								"<green><bold>Zapisałeś/aś</bold> się na: <blue><bold>%s</bold><green>. Wpisz ponownie <#FF0000>/123</#FF0000> aby <bold>zrezygnować</bold>. ", funEventManager.actualFunEvent.getEventName()));
						player.sendMessage(message);
					} else if (funEventManager.unregisterPlayer(player)) {
						player.sendMessage(Utils.mm("<#FF0000>Zrezygnowałeś/aś z zabawy: <blue><bold>" + funEventManager.actualFunEvent.getEventName()));
					} else {
						player.sendMessage(Utils.mm("<#FF0000>Brak wolnych miejsc, aby uczestniczyć w: <blue><bold>" + funEventManager.actualFunEvent.getEventName()));
					}
					e.setCancelled(true);
				}
			}
		}
	}
}
