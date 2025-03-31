package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.eadventure.plugin.FunEvents.FunEvent;

public class WarGangs extends FunEvent {
	public WarGangs(String eventName, int minPlayers) {
		super(eventName, minPlayers);
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