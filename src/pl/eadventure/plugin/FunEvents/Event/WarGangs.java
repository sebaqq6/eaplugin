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
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.Utils.Utils;

public class WarGangs extends FunEvent {
	Location teamRedSpawn;
	Location teamBlueSpawn;

	final int MAX_TIME_SECONDS = 60 * 3;
	int fragsTeamRed;
	int fragsTeamBlue;
	BossBar bossBar;
	int bossBarStep;
	int endTimeSeconds;

	public WarGangs(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
		super(eventName, minPlayers, maxPlayers, ownSet);
		teamRedSpawn = new Location(world_utility, 250, 111, 477);
		teamBlueSpawn = new Location(world_utility, 250, 112, 366);
		bossBar = Bukkit.createBossBar(eventName, BarColor.PURPLE, BarStyle.SOLID);
		Bukkit.getScheduler().runTaskTimer(getPlugin(), this::oneSecondTimer, 20L, 20L);
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
		endTimeSeconds = MAX_TIME_SECONDS;
		bossBarStep = 0;
	}

	public void oneSecondTimer() {
		if (getStatus() == Status.IN_PROGRESS) {
			endTimeSeconds--;
			if (endTimeSeconds > 0) {
				bossBarUpdate();
			} else if (endTimeSeconds == 0) {
				finishEvent();
			}
		}
	}

	public void bossBarUpdate() {
		double progressValue = (double) endTimeSeconds / (double) MAX_TIME_SECONDS;
		bossBar.setProgress(progressValue);
		int[] time = Utils.convertSecondsToTime(endTimeSeconds);
		String barTitle = String.format("&c&l%d &f- &9&l%d &7[&5%02d:%02d&7]", fragsTeamRed, fragsTeamBlue, time[1], time[2]);
		bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', barTitle));
	}

	@Override
	public boolean finishEvent() {
		if (fragsTeamRed > fragsTeamBlue) {//team red win?
			msgAll("Drużyna czerwona wygrała.");
		} else if (fragsTeamBlue > fragsTeamRed) { //blue win?
			msgAll("Drużyna niebieska wygrała.");
		} else {//draw
			msgAll("Remis.");
		}
		for (Player player : getPlayers()) {
			for (Player otherPlayer : getPlayers()) {
				GlowAPI.unGlowPlayer(player, otherPlayer);
			}
		}
		bossBar.setVisible(false);
		bossBar.removeAll();
		return super.finishEvent();
	}

	@Override
	public void playerQuit(Player player) {
		if (getPlayersFromTeam(TEAM_RED).isEmpty() || getPlayersFromTeam(TEAM_BLUE).isEmpty()) {
			finishEvent();
		}
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