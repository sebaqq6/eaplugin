package pl.eadventure.plugin.FunEvents;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.FunEvents.Event.ParkourEvent;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

	public FunEventsManager(Plugin plugin) {
		this.plugin = plugin;
		records = false;
		bossBar = Bukkit.createBossBar("Zapisy na event...", BarColor.YELLOW, BarStyle.SEGMENTED_20);
		runTask();
		listeners = new FunEventManagerListeners();
		Bukkit.getPluginManager().registerEvents(listeners, plugin);
		//Events
		registerEvent("parkour", new ParkourEvent("parkour"));
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

	public FunEvent getEvent(String name) {
		return events.get(name);
	}//pobieranie eventu po nazwie

	public boolean registerPlayer(Player player) {//dodawanie gracza do eventu
		if (!records) return false;
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

	private void runTask() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (records) {
					if (recordsCountDown > 0) {
						double progressValue = (double) recordsCountDown / (double) recordsCountDownMax;
						print.debug("Setprogress(" + recordsCountDown + "/" + recordsCountDownMax + ": " + progressValue);
						bossBar.setProgress(progressValue);
						recordsCountDown--;
					} else {
						actualFunEvent.setStatus(FunEvent.Status.IN_PROGRESS);
						actualFunEvent.start();
						stopRecord();
					}
				}
			}
		}.runTaskTimer(plugin, 20L, 20L);
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
			if (funEvent != null) {
				funEvent.removePlayer(player);
				player.teleport(spawnLocation);
			} else {
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
		}

		//==============================RESPAWN===========
		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
		}

		//==============================COMMAND===========
		@EventHandler
		public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
			FunEventsManager funEventManager = gVar.funEventsManager;
			Player player = e.getPlayer();
		}
	}
}
