package pl.eadventure.plugin.FunEvents;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.FunEvents.Event.TestEvent;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.awt.*;
import java.awt.List;
import java.util.*;

public abstract class FunEvent {
	private String eventName;
	private Set<Player> players = new HashSet<>();//Lista graczy uczestniczących na evencie
	private HashMap<Player, EvPlayer> playersVariables = new HashMap<>();
	private HashMap<Player, ItemStack[]> ownSets = new HashMap<>();
	private int status;
	private int minPlayers;
	private int maxPlayers;
	private Listener listener;
	private boolean ownSet;
	protected static World world_utility = Bukkit.getWorld("world_utility");

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
		Bukkit.getPluginManager().registerEvents(listener, getPlugin());
		Bukkit.getScheduler().runTaskTimerAsynchronously(getPlugin(), () -> oneSecondTimer(), 20L, 20L);
	}

	//FePlayer
	protected class EvPlayer {
		private Player player;
		private int team = 0;
		private ItemStack[] beforeJoinEq = null;
		private boolean beforeJoinEqSaved = false;
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

		//Integer
		public void setInt(String key, int val) {
			integers.put(key, val);
		}

		public int getInt(String key) {
			return integers.getOrDefault(key, 0);
		}

		//Float
		public void setFloat(String key, Float val) {
			floatvar.put(key, val);
		}

		public Float getFloat(String key) {
			return floatvar.getOrDefault(key, 0.0F);
		}

		//Others
		public void setTeam(int team) {
			this.team = team;
		}

		public int getTeam() {
			return this.team;
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
					if (player.isDead()) {
						print.error("Dead? " + player.getName());
					}
					getEvPlayer(player).restoreEqBeforeJoin();
					player.teleport(FunEventsManager.spawnLocation);
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
		print.debug("[msgAll-Event] " + msg);
		for (Player player : players) {
			if (player.isOnline()) {
				player.sendMessage(Utils.mm(msg));
			}
		}
	}

	public void tpAll(Location location) {
		for (Player player : players) {
			if (player.isOnline()) {
				player.teleport(location);
			}
		}
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

	protected void updateGlowTeam() {
		for (Player player : getPlayers()) {
			EvPlayer ep = getEvPlayer(player);
			for (Player otherPlayer : getPlayers()) {
				EvPlayer eop = getEvPlayer(otherPlayer);
				if (ep.getTeam() != 0 & ep.getTeam() == eop.getTeam()) {
					GlowAPI.glowPlayer(ep.getPlayer(), eop.getPlayer(), ChatColor.GREEN, 0);
				}//WIP
			}
		}
	}

	//******************************************************************************************************************
	//Timers
	//******************************************************************************************************************
	private void oneSecondTimer() {//ASYNC
		for (Player player : getPlayers()) {
			EvPlayer ep = getEvPlayer(player);

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
					damager.sendMessage("<grey>Nie możesz atakować członków swojej drużyny!");
				}
			}
		}
	}
}
