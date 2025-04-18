package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.Utils.print;

public class TestEvent extends FunEvent {
	Location eventLocation = new Location(Bukkit.getWorld("world_utility"), 250, 153, 420);

	public TestEvent(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
		super(eventName, minPlayers, maxPlayers, ownSet);
		this.setArenaPos(eventLocation);//ustawia pozycje areny (do debugowania)
	}


	/*Przed start wykonywane jest:
	actualFunEvent.setStatus(FunEvent.Status.IN_PROGRESS);//ustawienie statusu na IN PROGRESS
	actualFunEvent.clearPlayersVariables();//czyszczenie zmiennych dla graczy na evencie
	actualFunEvent.saveEqBeforeJoinForAll();//zapisywanie EQ przed dołączeniem (czyli póżniej niż setOwnSet - te jest zapisywane OSOBNO)
	actualFunEvent.start();//no i start
	 */
	@Override
	public void start() {
		tpAll(eventLocation);//teleportuje wszystkich na arene
		for (Player player : getPlayers()) {
			getEvPlayer(player).setTeam(1);//ustawia team
			clearPlayerInventory(player);//czyści wszystkim graczom eq
			setOwnSet(player);//ustawia set który gracz miał podczas zapisów (inventoryHasOnlySet)
			EvPlayer ep = getEvPlayer(player);//get player variables
		}
	}

	@Override
	public boolean finishEvent() {
		//przed zakończeniem eventu
		return super.finishEvent();
	}

	@Override
	public void playerQuit(Player player) {

	}

	@Override
	public void playerDeath(PlayerDeathEvent e) {//try dont use finishEvent(); here, but its work
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
