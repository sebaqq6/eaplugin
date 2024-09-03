package pl.eadventure.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.Utils.wgAPI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RegionCommandLooper {
	static Plugin plugin;
	static List<RegionData> regions = new ArrayList<>();
	static String filePath = "plugins/EternalAdventurePlugin/RegionCommandLooper.yml";
	Player player;
	RegionData lastRegion;
	boolean looping;
	int countEnd;

	record RegionData(String name, String command, String commandExit, int time) {
	}

	public RegionCommandLooper(Player player) {
		this.player = player;
		looping = false;
	}

	public static void load(Plugin plugin1) {
		plugin = plugin1;

		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getOnlinePlayers().forEach(RegionCommandLooper::refresh);
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L);
		//new regionData("test", "tps", 10);
		loadFromConfig();
	}

	public static void reload() {
		loadFromConfig();
	}

	private static void loadFromConfig() {
		print.info("Ładowanie configu: " + filePath);
		regions.clear();
		File file = new File(filePath);
		if (!file.exists()) {
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			config.set("region_name.command", "broadcast Gracz %username% wszedl/przebywa na regionie. Wyswietle sie co minute!");
			config.set("region_name.commandExit", "broadcast Gracz %username% opuscil region.");
			config.set("region_name.seconds", 60);
			Utils.saveConfig(file, config);
		} else {
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			Set<String> keys = config.getKeys(false);
			for (String key : keys) {
				String command = config.getString(key + ".command");
				String commandExit = config.getString(key + ".commandExit");
				int seconds = config.getInt(key + ".seconds");
				regions.add(new RegionData(key, command, commandExit, seconds));
			}
		}
	}


	private static void refresh(Player player) {
		PlayerData pd = PlayerData.get(player);
		if (pd.regionCommandLooper != null) {
			pd.regionCommandLooper.check();
		}
	}

	private void check() {
		boolean regionFound = false;
		for (RegionData region : regions) {
			if (wgAPI.isOnRegion(player, region.name)) {
				if (looping && countEnd > 0) {
					//print.debug("countdown: " + countEnd);
					countEnd--;
					if (lastRegion != null && !region.name.equalsIgnoreCase(lastRegion.name)) {//change region
						print.debug("[RGC] Changed region: " + region.name + " -> " + lastRegion.name);
						performCommand(lastRegion.commandExit);
						performCommand(region.command);
						countEnd = region.time;
						lastRegion = region;
					}
				} else if (looping && countEnd == 0) {
					performCommand(region.command);
					print.debug("[RGC] RESTART");
					countEnd = region.time;
				} else if (!looping) {
					performCommand(region.command);
					countEnd = region.time;
					looping = true;
					lastRegion = region;
					print.debug("[RGC] START!");
				}
				regionFound = true;
				break;
			}
		}
		//any regions not found
		if (!regionFound) {
			if (looping && lastRegion != null) {
				looping = false;
				performCommand(lastRegion.commandExit);
				print.debug("STOPP");
				lastRegion = null;
			}
		}
	}

	private void performCommand(String command) {
		new BukkitRunnable() {
			@Override
			public void run() {
				String finalCommand = command.replaceAll("%username%", player.getName());
				print.debug("finalCommand: " + finalCommand);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
			}
		}.runTask(plugin);
	}
}
