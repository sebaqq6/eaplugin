package pl.eadventure.plugin.FunEvents;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.FunEvents.Event.ParkourEvent;
import pl.eadventure.plugin.Utils.print;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FunEventsManager {
	Plugin plugin;
	FunEventsCommands funEventsCommands;
	private Map<String, FunEvent> events = new HashMap<>();
	private Map<UUID, String> playerEventMap = new HashMap<>();
	private boolean records;
	private int recordsCountDown;
	private BossBar bossBar;

	public FunEventsManager(Plugin plugin) {
		this.plugin = plugin;
		records = false;

		bossBar = Bukkit.createBossBar("Zapisy na event...", BarColor.YELLOW, BarStyle.SEGMENTED_20);
		funEventsCommands = new FunEventsCommands();
		runTask();
		//Events
		registerEvent("parkour", new ParkourEvent());
	}

	public boolean startRecord(String eventName, int recordsCountDown) {
		if(!records) {
			records = true;
			this.recordsCountDown = recordsCountDown;
			for (Player p : Bukkit.getOnlinePlayers()) {
				bossBar.addPlayer(p);
			}
			//bossBar.setVisible(true);
			return true;
		}
		return false;
	}

	public boolean stopRecord() {
		if(records) {
			records = false;
			bossBar.removeAll();
			bossBar.setVisible(false);
			this.recordsCountDown = 0;
			return true;
		}
		return false;
	}

	public void registerEvent(String name, FunEvent event) {
		events.put(name, event);
	}

	public FunEvent getEvent(String name) {
		return events.get(name);
	}

	public boolean registerPlayer(Player player, String eventName) {
		FunEvent event = events.get(eventName);
		if (event == null) {
			return false;
		}
		playerEventMap.put(player.getUniqueId(), eventName);
		return event.addPlayer(player);
	}

	public boolean unregisterPlayer(Player player) {
		String eventName = playerEventMap.remove(player.getUniqueId());
		if (eventName != null) {
			FunEvent event = events.get(eventName);
			if (event != null) {
				return event.removePlayer(player);
			}
		}
		return false;
	}

	public FunEventsCommands getFunEventsCommands() {
		return funEventsCommands;
	}

	private void runTask() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if(records) {
					if(recordsCountDown > 0) {
						print.debug("Setprogress..");
						bossBar.setProgress(1.0/recordsCountDown);
						recordsCountDown--;
					} else {
						stopRecord();
						//Run Event
					}
				}
			}
		}.runTaskTimer(plugin, 20L, 20L);
	}
}
