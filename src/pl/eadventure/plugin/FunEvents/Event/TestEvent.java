package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.Utils.print;

import java.util.HashMap;

public class TestEvent extends FunEvent {

	public TestEvent(String eventName, int minPlayers, int maxPlayers) {
		super(eventName, minPlayers, maxPlayers);

	}

	Location eventLocation = new Location(Bukkit.getWorld("world_utility"), -132, 72, -136);

	@Override
	public void start() {
		clearPlayersVariables();
		tpAll(eventLocation);
		for (Player player : getPlayers()) {
			getEvPlayer(player).setTeam(1);
		}
	}

	@Override
	public void playerQuit(Player player) {

	}

	@Override
	public void playerDeath(PlayerDeathEvent e) {//dont use finishEvent(); here
		print.error("playerDeath");
		finishEvent();
	}

	@Override
	public void playerRespawn(PlayerRespawnEvent e) {
		print.error("playerRespawn");
		e.getPlayer().teleport(eventLocation);
		e.getPlayer().sendMessage("Tp?");
	}
}
