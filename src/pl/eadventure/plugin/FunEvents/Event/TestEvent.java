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

	private HashMap<Player, Pvar> pVars = new HashMap<>();

	record Pvar(int team) {
	}

	public TestEvent(String eventName, int minPlayers) {
		super(eventName, minPlayers);
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(listener, getPlugin());
	}

	Location location = new Location(Bukkit.getWorld("world_utility"), -132, 72, -136);

	@Override
	public void start() {
		tpAll(location);
		pVars.clear();
		for (Player player : players) {
			pVars.put(player, new Pvar(0));
		}
	}

	@Override
	public void playerQuit(Player player) {

	}

	@Override
	public void playerDeath(PlayerDeathEvent e) {
		print.error("playerDeath");
	}

	@Override
	public void playerRespawn(PlayerRespawnEvent e) {
		print.error("playerRespawn");
		e.getPlayer().teleport(location);
		e.getPlayer().sendMessage("Tp?");
	}

	public class Listeners implements Listener {
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
			if (status != Status.IN_PROGRESS) return;
			if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player victim) {
				if (!isPlayerOnEvent(damager)) return;
				if (!isPlayerOnEvent(victim)) return;
				int damagerTeam = pVars.get(damager).team();
				int victimTeam = pVars.get(victim).team();

				if (damagerTeam != 0 && damagerTeam == victimTeam) {
					event.setCancelled(true);
					damager.sendMessage("Nie możesz atakować członków swojej drużyny!");
				}
			}
		}
	}

}
