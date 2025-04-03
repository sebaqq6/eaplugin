package pl.eadventure.plugin.FunEvents;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.FunEvents.Event.TestEvent;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class FunEvent {
	private String eventName;
	private Set<Player> players = new HashSet<>();//Lista graczy uczestniczących na evencie
	private HashMap<Player, EvPlayer> playersVariables = new HashMap<>();
	private int status;
	private int minPlayers;
	private int maxPlayers;
	private Listener listener;

	//FePlayer
	protected class EvPlayer {
		private Player player;
		private int team = 0;
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
	}

	public EvPlayer getEvPlayer(Player player) {
		return playersVariables.computeIfAbsent(player, EvPlayer::new);
	}

	public void clearPlayersVariables() {
		playersVariables.clear();
	}

	//STATUS
	public interface Status {
		int FREE = 0;
		int RECORDS = 1;
		int IN_PROGRESS = 2;
	}

	public FunEvent(String eventName, int minPlayers, int maxPlayers) {
		this.eventName = eventName;
		this.status = Status.FREE;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(listener, getPlugin());
	}

	public Plugin getPlugin() {
		return EternalAdventurePlugin.getInstance();
	}

	public String getEventName() {
		return eventName;
	}

	public boolean addPlayer(Player player) {
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

	public void finishEvent() {
		for (Player player : players) {
			player.teleport(FunEventsManager.spawnLocation);
			player.saveData();
		}
		setStatus(Status.FREE);
	}

	public void msgAll(String msg) {
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
					damager.sendMessage("Nie możesz atakować członków swojej drużyny!");
				}
			}
		}
	}
}
