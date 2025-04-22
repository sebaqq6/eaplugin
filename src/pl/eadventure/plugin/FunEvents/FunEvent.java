package pl.eadventure.plugin.FunEvents;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.object.GStopReason;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.API.PvpManagerAPI;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.*;

public abstract class FunEvent {
	private String eventName;
	private Set<Player> players = new LinkedHashSet<>();//Lista graczy uczestniczących na evencie
	private HashMap<Player, EvPlayer> playersVariables = new HashMap<>();
	private HashMap<Player, ItemStack[]> ownSets = new HashMap<>();
	protected List<String> rewardCommandsWin = new ArrayList<>();
	protected List<String> rewardCommandsLose = new ArrayList<>();
	protected List<Player> winPlayers = new ArrayList<>();
	protected final int TEAM_RED = 1;
	protected final int TEAM_BLUE = 2;
	private int status;
	private int minPlayers;
	private int maxPlayers;
	private Listener listener;
	private boolean ownSet;
	protected static World world_utility = Bukkit.getWorld("world_utility");
	public static ChatColor colorSpawnProtect = ChatColor.GOLD;
	protected int countDown;
	protected Location arenaPos;

	//STATUS
	public interface Status {
		int FREE = 0;
		int RECORDS = 1;
		int IN_PROGRESS = 2;
	}

	public FunEvent(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
		this.eventName = eventName;
		this.status = Status.FREE;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.ownSet = ownSet;
		this.listener = new Listeners();
		this.countDown = -1;
		Bukkit.getPluginManager().registerEvents(listener, getPlugin());
		Bukkit.getScheduler().runTaskTimerAsynchronously(getPlugin(), () -> oneSecondTimer(), 20L, 20L);
	}

	//FePlayer
	protected class EvPlayer {
		private Player player;
		private int team = 0;
		private ItemStack[] beforeJoinEq = null;
		private boolean beforeJoinEqSaved = false;
		private int spawnProtection = 0;
		private final HashMap<String, String> strings = new HashMap<>();
		private final HashMap<String, Integer> integers = new HashMap<>();
		private final HashMap<String, Float> floatvar = new HashMap<>();


		public EvPlayer(Player player) {
			this.player = player;
		}

		public Player getPlayer() {
			return player;
		}

		//String
		public void setStr(String key, String val) {
			strings.put(key, val);
		}

		public String getStr(String key) {
			return strings.getOrDefault(key, "");
		}

		// Integer
		public void setInt(String key, int val) {
			integers.put(key, val);
		}

		public int getInt(String key) {
			return integers.getOrDefault(key, 0);
		}

		public void addInt(String key, int val) {
			integers.put(key, getInt(key) + val);
		}

		public void subtractInt(String key, int val) {
			integers.put(key, getInt(key) - val);
		}

		// Float
		public void setFloat(String key, Float val) {
			floatvar.put(key, val);
		}

		public Float getFloat(String key) {
			return floatvar.getOrDefault(key, 0.0F);
		}

		public void addFloat(String key, Float val) {
			floatvar.put(key, getFloat(key) + val);
		}

		public void subtractFloat(String key, Float val) {
			floatvar.put(key, getFloat(key) - val);
		}


		//Others
		public void setTeam(int team) {
			this.team = team;
		}

		public int getTeam() {
			return this.team;
		}

		public void setSpawnProtection(int seconds) {
			this.spawnProtection = seconds;
		}

		public int getSpawnProtection() {
			return this.spawnProtection;
		}

		public void saveEqBeforeJoin() {
			if (!beforeJoinEqSaved) {
				beforeJoinEq = Arrays.stream(player.getInventory().getContents())
						.map(item -> item != null ? item.clone() : null)
						.toArray(ItemStack[]::new);
				beforeJoinEqSaved = true;
			}
		}

		public void restoreEqBeforeJoin() {
			if (beforeJoinEqSaved) {
				beforeJoinEqSaved = false;
				player.getInventory().clear();
				player.getInventory().setContents(beforeJoinEq);
				beforeJoinEq = null;
			}
		}
	}

	protected EvPlayer getEvPlayer(Player player) {
		return playersVariables.computeIfAbsent(player, EvPlayer::new);
	}

	public void clearPlayersVariables() {
		playersVariables.clear();
	}


	public Plugin getPlugin() {
		return EternalAdventurePlugin.getInstance();
	}

	public String getEventName() {
		return eventName;
	}

	public boolean addPlayer(Player player) {
		ItemStack[] items = Arrays.stream(player.getInventory().getContents())
				.map(item -> item != null ? item.clone() : null)
				.toArray(ItemStack[]::new);
		ownSets.put(player, items);
		return players.add(player);
	}

	public boolean removePlayer(Player player) {
		return players.remove(player);
	}

	public Set<Player> getPlayers() {
		return players;
	}

	public boolean isPlayerOnEvent(Player player) {
		return players.contains(player);
	}

	public int getPlayersCount() {
		return players.size();
	}

	public int getMinPlayers() {
		return minPlayers;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public boolean isOwnSet() {
		return ownSet;
	}

	public ArrayList<Player> getPlayersFromTeam(int t) {
		ArrayList<Player> playersTeam = new ArrayList<>();
		for (Player player : getPlayers()) {
			EvPlayer ep = getEvPlayer(player);
			if (ep.getTeam() == t) {
				playersTeam.add(player);
			}
		}
		return playersTeam;
	}

	public void setOwnSet(Player player) {
		ItemStack[] items = ownSets.getOrDefault(player, null);
		if (items != null) {
			player.getInventory().setContents(items);
		} else {
			print.error("Nie udało się ustawić własnego seta (FunEvent->setOwnSet) dla gracza: " + player.getName());
		}
	}

	public abstract void start();

	public abstract void playerQuit(Player player);

	public abstract void playerDeath(PlayerDeathEvent e);

	public abstract void playerRespawn(PlayerRespawnEvent e);


	public void setStatus(int status) {
		if (status == Status.FREE) {
			players.clear();
			winPlayers.clear();
			countDown = -1;
			clearPlayersVariables();
		}
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public boolean finishEvent() {
		if (status != Status.FREE) {
			int playersDead = 0;
			for (Player player : players) {
				if (player.isDead()) {
					playersDead++;
				}
			}
			if (playersDead == 0) {
				for (Player player : players) {
					//restore eq
					getEvPlayer(player).restoreEqBeforeJoin();
					//tp to spawn
					player.teleport(FunEventsManager.spawnLocation);
					//remove freeze
					PlayerData.get(player).freeze = false;
					//restore newbie
					PvpManagerAPI.restoreNewbie(player);
					//give rewards
					if (!rewardCommandsWin.isEmpty()) {
						if (winPlayers.contains(player)) {//execute command for win
							for (String rewardCmd : rewardCommandsWin) {
								String finalRewardCmd = rewardCmd.replace("%username%", player.getName());
								print.info(String.format("[%s] Nagroda wygrana: %s", eventName, finalRewardCmd));
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalRewardCmd);
							}
						} else {//execute command for lose
							for (String rewardCmd : rewardCommandsLose) {
								String finalRewardCmd = rewardCmd.replace("%username%", player.getName());
								print.info(String.format("[%s] Nagroda przegrana: %s", eventName, finalRewardCmd));
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalRewardCmd);
							}
						}
					}
					//restore scoreboard
					TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
					if (tabPlayer != null) {
						Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).resetScoreboard(tabPlayer);
					}
					//save data
					player.saveData();
				}
				setStatus(Status.FREE);
				print.ok("Event " + eventName + " pomyślnie zakończony!");
			} else {
				print.error("Nie udało się zakończyć eventu, gdyż " + playersDead + " graczy nie żyje. Ponawiam próbę...");
				Bukkit.getScheduler().runTaskLater(getPlugin(), r -> finishEvent(), 30L);
			}

			return true;
		} else return false;
	}

	public void msgAll(String msg) {
		print.debug("[msgAll-" + eventName + "] " + msg);
		for (Player player : players) {
			if (player.isOnline()) {
				player.sendMessage(Utils.mm(msg));
			}
		}
	}

	public void titleAll(String strTitle, String strSubtitle) {
		print.debug("[titleAll-" + eventName + "] " + strTitle + " (" + strSubtitle + ")");
		for (Player player : players) {
			if (player.isOnline()) {
				title(player, strTitle, strSubtitle);
			}
		}
	}

	public void title(Player p, String strTitle, String strSubtitle) {
		p.showTitle(Title.title(Utils.mm(strTitle), Utils.mm(strSubtitle)));
	}

	public void tpAll(Location location) {
		for (Player player : players) {
			if (player.isOnline()) {
				GSitAPI.stopPlayerSit(player, GStopReason.PLUGIN);
			}
		}
		final Location finalLocation = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : players) {
					if (player.isOnline()) {
						player.teleport(finalLocation);
					}
				}
			}
		}.runTaskLater(getPlugin(), 10L);
	}

	public void tp(Player player, Location location) {
		if (player.isOnline()) {
			GSitAPI.stopPlayerSit(player, GStopReason.PLUGIN);
		}
		new BukkitRunnable() {
			final Location finalLocation = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

			@Override
			public void run() {
				if (player.isOnline()) {
					player.teleport(finalLocation);
				}
			}
		}.runTaskLater(getPlugin(), 10L);
	}

	public void saveEqBeforeJoinForAll() {
		for (Player player : players) {
			if (player.isOnline()) {
				getEvPlayer(player).saveEqBeforeJoin();
			}
		}
	}

	public void clearPlayerInventory(Player player) {
		player.getInventory().clear();
	}


	public interface GlowTeamType {
		int OnlyOwn = 0;
		int AllForAll = 1;
	}

	@SuppressWarnings("deprecation")
	protected void updateGlowTeam(int type) {
		for (Player player : getPlayers()) {
			EvPlayer ep = getEvPlayer(player);
			for (Player otherPlayer : getPlayers()) {
				EvPlayer eop = getEvPlayer(otherPlayer);
				if (type == GlowTeamType.OnlyOwn) {//Show only own team
					if (ep.getTeam() != 0 & ep.getTeam() == eop.getTeam()) {
						ChatColor color = ChatColor.WHITE;
						if (ep.getTeam() == TEAM_RED) {
							if (ep.getSpawnProtection() > 0) {
								color = colorSpawnProtect;
							} else {
								color = ChatColor.RED;
							}
						} else if (ep.getTeam() == TEAM_BLUE) {
							if (ep.getSpawnProtection() > 0) {
								color = colorSpawnProtect;
							} else {
								color = ChatColor.BLUE;
							}
						}
						if (player.isInvisible()) {
							GlowAPI.unGlowPlayer(ep.getPlayer(), eop.getPlayer());
						} else {
							GlowAPI.glowPlayer(ep.getPlayer(), eop.getPlayer(), color, 0);
						}

					}
				} else if (type == GlowTeamType.AllForAll) {//Show glow for all
					if (ep.getTeam() != 0) {
						ChatColor color = ChatColor.WHITE;
						if (ep.getTeam() == TEAM_RED) {
							if (ep.getSpawnProtection() > 0) {
								color = colorSpawnProtect;
							} else {
								color = ChatColor.RED;
							}
						} else if (ep.getTeam() == TEAM_BLUE) {
							if (ep.getSpawnProtection() > 0) {
								color = colorSpawnProtect;
							} else {
								color = ChatColor.BLUE;
							}
						}
						if (player.isInvisible()) {
							GlowAPI.unGlowPlayer(ep.getPlayer(), eop.getPlayer());
						} else {
							GlowAPI.glowPlayer(ep.getPlayer(), eop.getPlayer(), color, 0);
						}
					}
				}
			}
		}
	}

	protected void setArenaPos(Location arenaPos) {
		this.arenaPos = arenaPos;
	}

	public Location getArenaPos() {
		return this.arenaPos;
	}

	//countdown
	public void startCountdown(int seconds, CountDownEnd callback) {
		if (this.countDown != -1) return;
		if (seconds < 1) return;
		this.countDown = seconds;
		titleAll("<#FF0000><bold>" + this.countDown + "</bold>", " ");//odliczanie
		new BukkitRunnable() {
			@Override
			public void run() {
				if (countDown > 0) {
					for (Player player : getPlayers()) {
						title(player, "<#FF0000><bold>" + countDown + "</bold>", " ");//odliczanie
						player.playSound(player.getLocation(), "my_sounds:sounds.warning",
								SoundCategory.MASTER, 1.0f, 1.5f);
					}
					countDown--;
				} else {
					cancel();
					countDown = -1;
					for (Player player : getPlayers()) {
						title(player, "<#00FF00><bold>START!</bold>", " ");//START!
						player.playSound(player.getLocation(), "my_sounds:sounds.warning",
								SoundCategory.MASTER, 1.0f, 0.8f);
					}
					callback.onCountDownEnd();
				}
			}
		}.runTaskTimer(getPlugin(), 20L, 20L);
	}

	public interface CountDownEnd {
		void onCountDownEnd();
	}

	//******************************************************************************************************************
	//Timers
	//******************************************************************************************************************
	private void oneSecondTimer() {//ASYNC
		if (getStatus() != Status.IN_PROGRESS) return;
		for (Player player : getPlayers()) {
			//count down spawn protect
			spawnProtectProcess(player);
		}
	}

	private void spawnProtectProcess(Player player) {
		EvPlayer ep = getEvPlayer(player);
		if (ep.getSpawnProtection() > 0) {
			ep.setSpawnProtection(ep.getSpawnProtection() - 1);
			if (ep.getSpawnProtection() == 0) {
				title(player, " ", "<red>Koniec ochrony spawnu.");
			} else {
				title(player, " ", "<grey>Ochrona spawnu: <yellow>" + ep.getSpawnProtection() + " <grey>sekund.");
			}
		}
	}

	//******************************************************************************************************************
	//Listeners
	//******************************************************************************************************************
	public class Listeners implements Listener {
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
			if (status != Status.IN_PROGRESS) return;
			if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player victim) {
				if (!isPlayerOnEvent(damager)) return;
				if (!isPlayerOnEvent(victim)) return;
				EvPlayer epDamager = getEvPlayer(damager);
				EvPlayer epVictim = getEvPlayer(victim);
				int damagerTeam = epDamager.getTeam();
				int victimTeam = epVictim.getTeam();

				if (damagerTeam != 0 && damagerTeam == victimTeam) {
					event.setCancelled(true);
					damager.sendMessage(Utils.mm("<grey>Nie możesz atakować członków swojej drużyny!"));
					return;
				}
				//spawn protect
				if (epVictim.getSpawnProtection() > 0) {
					event.setCancelled(true);
					damager.sendMessage(Utils.mm("<grey>Ten gracz posiada ochronę spawnu."));
					return;
				}
			}
		}
	}
}
