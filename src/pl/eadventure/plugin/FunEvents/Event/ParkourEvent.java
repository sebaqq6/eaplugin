package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.entity.Player;
import pl.eadventure.plugin.FunEvents.FunEvent;

public class ParkourEvent extends FunEvent {
	public ParkourEvent(String eventName) {
		super(eventName);
	}

	@Override
	public void start() {
		for (Player player : players) {
			player.sendMessage("Parkour event has started!");
		}
		finishEvent();
	}
}