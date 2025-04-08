package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.Utils.print;

public class WarGangs extends FunEvent {
	Location teamRedSpawn;
	Location teamBlueSpawn;
	final int TEAM_RED = 1;
	final int TEAM_BLUE = 2;
	int fragsTeamRed;
	int fragsTeamBlue;
	BossBar bossBar;

	public WarGangs(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
		super(eventName, minPlayers, maxPlayers, ownSet);
		teamRedSpawn = new Location(world_utility, 250, 111, 477);
		teamBlueSpawn = new Location(world_utility, 250, 112, 366);
		bossBar = Bukkit.createBossBar(eventName, BarColor.BLUE, BarStyle.SOLID);
	}

	@Override
	public void start() {
		//reset points
		fragsTeamRed = 0;
		fragsTeamBlue = 0;
		//do teams and teleport to the spawns
		int teamSelector = TEAM_RED;
		for (Player player : getPlayers()) {
			bossBar.addPlayer(player);
			//clear invent
			clearPlayerInventory(player);
			//set own set
			setOwnSet(player);
			//get player variables to ep
			EvPlayer ep = getEvPlayer(player);
			//set team
			ep.setTeam(teamSelector);
			//teleport player to the team spawn
			if (teamSelector == TEAM_RED) {
				player.teleport(teamRedSpawn);
			} else if (teamSelector == TEAM_BLUE) {
				player.teleport(teamBlueSpawn);
			}
			teamSelector++;
			if (teamSelector >= 3) {
				teamSelector = TEAM_RED;
			}
		}
		bossBar.setVisible(true);
		updateGlowTeam();//show glows for friendly players
	}

	@Override
	public boolean finishEvent() {
		bossBar.setVisible(false);
		bossBar.removeAll();
		return super.finishEvent();
	}

	@Override
	public void playerQuit(Player player) {

	}

	@Override
	public void playerDeath(PlayerDeathEvent e) {
		Player player = e.getPlayer();
		if (getEvPlayer(player).getTeam() == TEAM_RED) {
			fragsTeamBlue++;
		} else if (getEvPlayer(player).getTeam() == TEAM_BLUE) {
			fragsTeamRed++;
		}
	}

	@Override
	public void playerRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		if (getEvPlayer(player).getTeam() == TEAM_RED) {
			player.teleport(teamRedSpawn);
		} else if (getEvPlayer(player).getTeam() == TEAM_BLUE) {
			player.teleport(teamBlueSpawn);
		}
	}
}