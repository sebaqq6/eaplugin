package pl.eadventure.plugin.FunEvents;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public abstract class FunEvent {
	protected String eventName;
	protected Set<Player> players = new HashSet<>();//Lista graczy uczestniczących na evencie
	protected int status;

	//STATUS
	public interface Status {
		int FREE = 0;
		int RECORDS = 1;
		int IN_PROGRESS = 2;
	}

	public FunEvent(String eventName) {
		this.eventName = eventName;
		this.status = Status.FREE;
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

	public abstract void start();

	public void setStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public void finishEvent() {
		for (Player player : players) {
			player.teleport(FunEventsManager.spawnLocation);
		}
		
		players.clear();
		status = Status.FREE;
	}
}
