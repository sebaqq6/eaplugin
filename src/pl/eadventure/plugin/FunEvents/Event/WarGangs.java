package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.eadventure.plugin.FunEvents.FunEvent;

public class WarGangs extends FunEvent {
	Location teamRedSpawn;
	Location teamBlueSpawn;

	public WarGangs(String eventName, int minPlayers, int maxPlayers) {
		super(eventName, minPlayers, maxPlayers);
		teamRedSpawn = new Location(world_utility, 250, 111, 477);
		teamBlueSpawn = new Location(world_utility, 250, 112, 366);
	}

	@Override
	public void start() {
		msgAll("wg wystartowało :D");
		finishEvent();
	}

	@Override
	public void playerQuit(Player player) {

	}

	@Override
	public void playerDeath(PlayerDeathEvent e) {

	}

	@Override
	public void playerRespawn(PlayerRespawnEvent e) {

	}
}