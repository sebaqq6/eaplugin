package pl.eadventure.plugin.Modules;

import com.nickuc.login.api.nLoginAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.ProtocolLibAPI;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.print;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AutoSpectator {
	public static AutoSpectator instance;
	Plugin plugin;
	List<Player> players = new ArrayList<>();
	HashMap<Player, Player> specNow = new HashMap<>();
	Listener listener;
	Location spawnCamera;
	Location spawnLocation;
	World world;

	public AutoSpectator(Plugin plugin) {
		if (instance != null) return;
		this.plugin = plugin;
		instance = this;
		this.world = Bukkit.getWorld("world");
		this.spawnLocation = new Location(world, 31, 169, -23);
		this.spawnCamera = new Location(world, 21.10, 178.34, -34.41, -46.62F, 31.80F);
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(this.listener, plugin);
		Bukkit.getScheduler().runTaskTimer(plugin, this::scheduler, 20L, 20L * 45L);
		print.info("AutoSpectator - loaded.");
	}

	public void scheduler() {
		for (Player p : players) {
			updateCam(p);
		}
	}

	public void updateCam(Player p) {
		if (p.getGameMode() != GameMode.SPECTATOR) {
			p.setGameMode(GameMode.SPECTATOR);
			p.teleport(spawnCamera);
			specNow.put(p, null);
			return;
		}

		// Tworzymy listę żywych graczy
		List<Player> availablePlayers = Bukkit.getOnlinePlayers().stream()
				.filter(pl -> !pl.equals(p)) // Nie siebie
				.filter(pl -> !pl.hasPermission("eadventureplugin.autospec.bypass")) // Bez bypassa
				.filter(pl -> pl.getGameMode() == GameMode.SURVIVAL || pl.getGameMode() == GameMode.ADVENTURE) // Tylko żywi
				.collect(Collectors.toList());

		if (availablePlayers.isEmpty()) {
			p.teleport(spawnCamera);
			specNow.put(p, null);
			return;
		}

		Player currentTarget = specNow.get(p);
		int nextIndex = 0;

		if (currentTarget != null && availablePlayers.contains(currentTarget)) {
			int currentIndex = availablePlayers.indexOf(currentTarget);
			nextIndex = (currentIndex + 1) % availablePlayers.size();
		}

		Player nextTarget = availablePlayers.get(nextIndex);
		if (nextTarget.equals(currentTarget)) return;
		if (!nextTarget.getWorld().equals(p.getWorld())) {//other world?
			p.setInvisible(true);
			Location targetLocation = nextTarget.getLocation();
			targetLocation.setY(targetLocation.getY() + 256);
			p.teleport(targetLocation);
			Bukkit.getScheduler().runTaskLater(EternalAdventurePlugin.getInstance(), () -> {
				p.setGameMode(GameMode.SPECTATOR);
				p.setSpectatorTarget(nextTarget);
			}, 20L);
		}
		p.setSpectatorTarget(nextTarget);
		specNow.put(p, nextTarget);
		//
		PlayerData pd = PlayerData.get(p);
		pd.lastTeleport = Timestamp.from(Instant.now().minus(Duration.ofDays(1)));
	}


	public static boolean enable(Player player) {
		if (instance.players.contains(player)) return false;
		instance.players.add(player);
		instance.updateCam(player);
		return true;
	}

	public static boolean disable(Player player) {
		if (!instance.players.contains(player)) return false;
		instance.players.remove(player);
		instance.specNow.remove(player);
		player.teleport(instance.spawnLocation);
		player.setGameMode(GameMode.SURVIVAL);
		player.setInvisible(false);
		return true;
	}

	public static boolean isEnabled(Player player) {
		return instance.players.contains(player);
	}

	//LISTENER
	public static class Listeners implements Listener {
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			Player player = e.getPlayer();
			String ip = player.getAddress().getAddress().getHostAddress();
			if (player.getName().equals("EternalCam") && ip.equalsIgnoreCase("51.38.148.6")) {
				new BukkitRunnable() {
					@Override
					public void run() {
						print.info("Trwa logowanie automatyczne EternalCam...");
						if (nLoginAPI.getApi().forceLogin("EternalCam")) {
							print.info("Zalogowano automatycznie EternalCam - uruchamianie AutoSpec");
							AutoSpectator.enable(player);
						} else {
							print.error("Nie udało się zalogować automatycznie EternalCam.");
						}
					}
				}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L);
			}
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent e) {
			disable(e.getPlayer());
			for (Player spectator : new ArrayList<>(instance.players)) {
				if (instance.specNow.get(spectator) != null && instance.specNow.get(spectator).equals(e.getPlayer())) {
					instance.updateCam(spectator);
				}
			}
		}
	}
}