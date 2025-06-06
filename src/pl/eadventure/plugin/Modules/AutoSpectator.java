package pl.eadventure.plugin.Modules;

import com.nickuc.login.api.nLoginAPI;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.ProtocolLibAPI;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.FunEvents.FunEventsManager;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
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
	public static String liveOperatorNick = "EternalCam";
	public static int changeTime = 30;//seconds
	Plugin plugin;
	List<Player> players = new ArrayList<>();
	HashMap<Player, Player> specNow = new HashMap<>();
	HashMap<Player, Integer> specCountdown = new HashMap<>();
	Listener listener;
	Location spawnCamera;
	Location spawnLocation;
	World world;
	World world_event;
	World world_utility;

	public AutoSpectator(Plugin plugin) {
		if (instance != null) return;
		this.plugin = plugin;
		instance = this;
		this.world = Bukkit.getWorld("world");
		this.world_event = Bukkit.getWorld("world_event");
		this.world_utility = Bukkit.getWorld("world_utility");
		this.spawnLocation = new Location(world, 31, 169, -23);
		this.spawnCamera = new Location(world, 21.10, 178.34, -34.41, -46.62F, 31.80F);
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(this.listener, plugin);
		Bukkit.getScheduler().runTaskTimer(plugin, this::scheduler, 20L, 20L);
		print.info("AutoSpectator - loaded.");
	}

	public void scheduler() {
		for (Player p : players) {
			updateCam(p);
		}
	}

	public void updateCam(Player p) {
		//timer section - start
		if (timeGoNextGet(p) > 0) {
			decreaseTimeGoNext(p, 1);
			return;
		}
		timeGoNextSet(p, 5);
		//timer section - end
		if (p.getGameMode() != GameMode.SPECTATOR) {
			p.setGameMode(GameMode.SPECTATOR);
			p.teleport(spawnCamera);
			specNow.put(p, null);
			return;
		}

		boolean isLiveOperator = isLiveOperator(p);

		List<Player> availablePlayers = new ArrayList<>();
		print.debug("updateCam -> isOperator: " + isLiveOperator + ", isEvent: " + isEvent() + ", nick: " + p.getName() + ", timeGoNext: " + timeGoNextGet(p));
		// Lista
		if (isLiveOperator) {//for operator list
			if (!OneVsOneBossBars.getActiveWarriors().isEmpty()) {
				//availablePlayers.addAll(OneVsOneBossBars.getActiveWarriors());
				p.teleport(new Location(world_utility, 133.43, 74.17, -284.84, 90.00F, 30.30F));
				p.setGameMode(GameMode.SPECTATOR);
				new BukkitRunnable() {
					@Override
					public void run() {
						p.teleport(new Location(world_utility, 133.43, 74.17, -284.84, 90.00F, 30.30F));
						p.setGameMode(GameMode.SPECTATOR);
					}
				}.runTaskLater(plugin, 20L);
				specNow.put(p, null);
				timeGoNextSet(p, 30);
				return;
			} else if (isEvent()) {//is event
				List<Player> forOperatorListEvent = Bukkit.getOnlinePlayers().stream()
						.filter(pl -> !pl.equals(p)) // Nie siebie
						.filter(pl -> !pl.hasPermission("eadventureplugin.autospec.bypass")) // Bez bypassa
						.filter(pl -> !PlayerUtils.isAfk(pl)) // Bez AFK'ów
						.filter(pl -> !PlayerUtils.isVanished(pl)) // Bez Vanisha
						.filter(pl -> !pl.isDead()) // Jest żywy
						.filter(pl -> world_event.equals(pl.getWorld())) //Jest na evencie
						.filter(pl -> pl.getGameMode() == GameMode.SURVIVAL) // Tylko survival
						.collect(Collectors.toList());
				availablePlayers.addAll(forOperatorListEvent);
			} else if (isFunEvent()) {//is funevent
				List<Player> forOperatorListFunEvent = Bukkit.getOnlinePlayers().stream()
						.filter(pl -> !pl.equals(p)) // Nie siebie
						.filter(pl -> !pl.hasPermission("eadventureplugin.autospec.bypass")) // Bez bypassa
						.filter(pl -> !PlayerUtils.isAfk(pl)) // Bez AFK'ów
						.filter(pl -> !PlayerUtils.isVanished(pl)) // Bez Vanisha
						.filter(pl -> !pl.isDead()) // Jest żywy
						.filter(pl -> world_utility.equals(pl.getWorld())) //Jest na FunEvencie
						.filter(pl -> pl.getGameMode() == GameMode.SURVIVAL) // Tylko survival
						.collect(Collectors.toList());
				availablePlayers.addAll(forOperatorListFunEvent);
			} else {//no event
				List<Player> forOperatorListStandard = Bukkit.getOnlinePlayers().stream()
						.filter(pl -> !pl.equals(p)) // Nie siebie
						.filter(pl -> !pl.hasPermission("eadventureplugin.autospec.bypass")) // Bez bypassa
						.filter(pl -> !PlayerUtils.isAfk(pl)) // Bez AFK'ów
						.filter(pl -> !PlayerUtils.isVanished(pl)) // Bez Vanisha
						.filter(pl -> !pl.isDead()) // Jest żywy
						.filter(pl -> PlayerData.get(pl).unParticipateLive == 0 || isSpecialWorld(pl)) // Chce uczestniczyć w live lub jest na evencie/arenie.
						.filter(pl -> pl.getGameMode() == GameMode.SURVIVAL) // Tylko survival
						.collect(Collectors.toList());
				availablePlayers.addAll(forOperatorListStandard);
			}
		} else {//for admin list
			List<Player> forAdminsList = Bukkit.getOnlinePlayers().stream()
					.filter(pl -> !pl.equals(p)) // Nie siebie
					.filter(pl -> !pl.hasPermission("eadventureplugin.autospec.bypass")) // Bez bypassa
					.filter(pl -> !PlayerUtils.isAfk(pl)) // Bez AFK'ów
					.filter(pl -> !PlayerUtils.isVanished(pl)) // Bez Vanisha
					.filter(pl -> !pl.isDead()) // Jest żywy
					.filter(pl -> pl.getGameMode() == GameMode.SURVIVAL) // Tylko survival
					.collect(Collectors.toList());
			availablePlayers.addAll(forAdminsList);
		}

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
		//set new target
		timeGoNextSet(p, changeTime);
		p.sendMessage(Utils.mm("<bold><grey>Obserwujesz gracza: <red>" + nextTarget.getName()));
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
		p.setSpectatorTarget(null);
		p.setSpectatorTarget(nextTarget);
		specNow.put(p, nextTarget);
		//
		PlayerData pd = PlayerData.get(p);
		pd.lastTeleport = Timestamp.from(Instant.now().minus(Duration.ofDays(1)));
	}


	public static boolean enable(Player player) {
		if (instance.players.contains(player)) return false;
		instance.players.add(player);
		instance.timeGoNextSet(player, changeTime);
		instance.updateCam(player);
		if (player.getGameMode() != GameMode.SPECTATOR) {
			player.setGameMode(GameMode.SPECTATOR);
			player.teleport(instance.spawnCamera);
			instance.specNow.put(player, null);
		}
		return true;
	}

	public static boolean disable(Player player) {
		if (!instance.players.contains(player)) return false;
		instance.players.remove(player);
		instance.specNow.remove(player);
		instance.specCountdown.remove(player);
		player.teleport(instance.spawnLocation);
		player.setGameMode(GameMode.SURVIVAL);
		player.setInvisible(false);
		return true;
	}

	public static boolean isEnabled(Player player) {
		return instance.players.contains(player);
	}

	private boolean isSpecialWorld(Player player) {
		return (player.getWorld().equals(world_event) || player.getWorld().equals(world_utility));
	}

	public boolean isEvent() {
		return (world_event.getPlayerCount() > 5);
	}

	public boolean isFunEvent() {
		int playersCount = 0;
		for (Player p : world_utility.getPlayers()) {
			if (FunEventsManager.isPlayerOnEvent(p) != null) {
				playersCount++;
				if (playersCount >= 4) {
					return true;
				}
			}
		}
		return false;
	}

	private static long movedTooQuicklyLastPrint = 0;

	public static void movedTooQuicklyEvent() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - movedTooQuicklyLastPrint >= 1000) {
			print.error(liveOperatorNick + " moved too quickly!");
			movedTooQuicklyLastPrint = currentTime;
		}
	}

	public static boolean isLiveOperator(Player p) {
		if (p.getName().equalsIgnoreCase(liveOperatorNick)) {
			p.getInventory().clear();
			return true;
		}
		return false;
	}

	private int timeGoNextGet(Player player) {
		return specCountdown.getOrDefault(player, 0);
	}

	private void timeGoNextSet(Player player, int value) {
		specCountdown.put(player, value);
	}

	private void decreaseTimeGoNext(Player player, int value) {
		timeGoNextSet(player, timeGoNextGet(player) - value);
	}

	private void increaseTimeGoNext(Player player, int value) {
		timeGoNextSet(player, timeGoNextGet(player) + value);
	}

	//LISTENER
	public static class Listeners implements Listener {
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			Player player = e.getPlayer();
			String ip = player.getAddress().getAddress().getHostAddress();
			if (player.getName().equals(liveOperatorNick)) {
				if (ip.equalsIgnoreCase("51.38.148.6")) {
					new BukkitRunnable() {
						@Override
						public void run() {
							print.info("Trwa logowanie automatyczne " + liveOperatorNick + "...");
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "vconsole nlogin forcelogin " + liveOperatorNick);
							AutoSpectator.enable(player);
							TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
							TabAPI.getInstance().getScoreboardManager().setScoreboardVisible(tabPlayer, false, false);
						}
					}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L);
				} else {
					player.kick(Utils.mm("Konto zarezerwowane."));
				}
			}
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent e) {
			disable(e.getPlayer());
			for (Player spectator : new ArrayList<>(instance.players)) {
				if (instance.specNow.get(spectator) != null && instance.specNow.get(spectator).equals(e.getPlayer())) {
					instance.timeGoNextSet(spectator, 0);
					instance.updateCam(spectator);
				}
			}
		}

		@EventHandler
		public void onPlayerDeath(PlayerQuitEvent e) {
			for (Player spectator : new ArrayList<>(instance.players)) {
				if (instance.specNow.get(spectator) != null && instance.specNow.get(spectator).equals(e.getPlayer())) {
					instance.timeGoNextSet(spectator, 0);
					instance.updateCam(spectator);
				}
			}
		}

		@EventHandler
		public void onGameModeChange(PlayerGameModeChangeEvent e) {
			if (e.getNewGameMode() != GameMode.SURVIVAL) {
				for (Player spectator : new ArrayList<>(instance.players)) {
					if (instance.specNow.get(spectator) != null && instance.specNow.get(spectator).equals(e.getPlayer())) {
						instance.timeGoNextSet(spectator, 0);
						instance.updateCam(spectator);
					}
				}
			}
		}
	}
}