package pl.eadventure.plugin.FunEvents;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class FunEvent {
	protected String eventName;
	protected Set<Player> players = new HashSet<>();//Lista graczy uczestniczących na evencie
	protected int status;
	protected int minPlayers;
	protected Listener listener;

	//STATUS
	public interface Status {
		int FREE = 0;
		int RECORDS = 1;
		int IN_PROGRESS = 2;
	}

	public FunEvent(String eventName, int minPlayers) {
		this.eventName = eventName;
		this.status = Status.FREE;
		this.minPlayers = minPlayers;
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

	public boolean isPlayerOnEvent(Player player) {
		return players.contains(player);
	}

	public int getPlayersCount() {
		return players.size();
	}

	public int getMinPlayers() {
		return minPlayers;
	}

	public abstract void start();

	public abstract void playerQuit(Player player);

	public abstract void playerDeath(PlayerDeathEvent e);

	public abstract void playerRespawn(PlayerRespawnEvent e);


	public void setStatus(int status) {
		if (status == Status.FREE) {
			players.clear();
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

}
