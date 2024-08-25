package pl.eadventure.plugin.FunEvents;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public abstract class FunEvent {
	protected Set<Player> players = new HashSet<>();

	public boolean addPlayer(Player player) {
		return players.add(player);
	}

	public boolean removePlayer(Player player) {
		return players.remove(player);
	}

	public abstract void start();
}
