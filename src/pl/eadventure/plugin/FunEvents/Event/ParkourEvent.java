package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.entity.Player;
import pl.eadventure.plugin.FunEvents.FunEvent;

public class ParkourEvent extends FunEvent {
	@Override
	public void start() {
		// Placeholder: Implement event start logic
		for (Player player : players) {
			player.sendMessage("Parkour event has started!");
		}
	}
}