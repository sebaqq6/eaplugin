package pl.eadventure.plugin.FunEvents.Event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.API.PvpManagerAPI;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

public class StarcieEternal extends FunEvent {
	Location teamRedSpawn;
	Location teamBlueSpawn;

	final int MAX_TIME_SECONDS = 60 * 5;
	int fragsTeamRed;
	int fragsTeamBlue;
	BossBar bossBar;
	int bossBarStep;
	int endTimeSeconds;

	public StarcieEternal(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
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
			//remove newbie
			PvpManagerAPI.takeNewbie(player);
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
				title(player, "<#FF0000>Drużyna czerwona", "Należysz do drużyny czerwonej.");
			} else if (teamSelector == TEAM_BLUE) {
				player.teleport(teamBlueSpawn);
				title(player, "<#0000FF>Drużyna niebieska", "Należysz do drużyny niebieskiej.");
			}
			teamSelector++;
			if (teamSelector >= 3) {
				teamSelector = TEAM_RED;
			}
		}
		bossBar.setVisible(true);
		//Bukkit.getScheduler().runTaskLater(getPlugin(), () -> updateGlowTeam(GlowTeamType.AllForAll), 20L * 5);
		endTimeSeconds = MAX_TIME_SECONDS;
		bossBarStep = 0;
	}

	public void oneSecondTimer() {
		if (getStatus() == Status.IN_PROGRESS) {
			if (endTimeSeconds > 0) {
				endTimeSeconds--;
				bossBarUpdate();
				updateGlowTeam(GlowTeamType.AllForAll);
			} else if (endTimeSeconds == 0) {
				finishEvent();
			}
		}
	}

	public void bossBarUpdate() {
		double progressValue = (double) endTimeSeconds / (double) MAX_TIME_SECONDS;
		bossBar.setProgress(progressValue);
		int[] time = Utils.convertSecondsToTime(endTimeSeconds);
		String barTitle = String.format("&c&l%d &f- &9&l%d &7&l[&5&l%02d:%02d&7&l]", fragsTeamRed, fragsTeamBlue, time[1], time[2]);
		bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', barTitle));
	}

	@Override
	public boolean finishEvent() {
		int winTeam = 0;
		if (fragsTeamRed > fragsTeamBlue) {//team red win?
			msgAll(String.format("<bold><grey>[<dark_purple>%s<grey>]</bold><grey> Wygrała drużyna <red><bold>czerwonych</bold><grey>, wynik: <bold><red>%d <grey>- <blue>%d", getEventName(), fragsTeamRed, fragsTeamBlue));
			winTeam = TEAM_RED;
		} else if (fragsTeamBlue > fragsTeamRed) { //blue win?
			winTeam = TEAM_BLUE;
			msgAll(String.format("<bold><grey>[<dark_purple>%s<grey>]</bold><grey> Wygrała drużyna <blue><bold>niebieskich</bold><grey>, wynik: <bold><red>%d <grey>- <blue>%d", getEventName(), fragsTeamRed, fragsTeamBlue));
		} else {//draw
			msgAll(String.format("<bold><grey>[<dark_purple>%s<grey>]</bold><grey> Starcie zakończyło się <gradient:red:blue><bold>remisem</bold></gradient><grey>, wynik: <bold><red>%d <grey>- <blue>%d", getEventName(), fragsTeamRed, fragsTeamBlue));
		}
		for (Player player : getPlayers()) {
			if (getEvPlayer(player).getTeam() == winTeam) {//win
				winPlayers.add(player);
				title(player, "<#00FF00><bold>Zwycięstwo!</bold>", " ");
				player.playSound(player.getLocation(), "minecraft:ui.toast.challenge_complete",
						SoundCategory.MASTER, 1.0f, 1.0f);
			} else if (winTeam == 0) {//draw
				title(player, "<gradient:red:green><bold>Remis!</bold></gradient>", " ");
				player.playSound(player.getLocation(), "my_sounds:sounds.boss.clear",
						SoundCategory.MASTER, 1.0f, 1.0f);
			} else {//lose
				title(player, "<#FF0000><bold>Porażka!</bold>", " ");
				player.playSound(player.getLocation(), "my_sounds:sounds.boss.clear",
						SoundCategory.MASTER, 1.0f, 1.0f);
			}
			//back to spawns
			if (getEvPlayer(player).getTeam() == TEAM_RED) {
				player.teleport(teamRedSpawn);
			} else if (getEvPlayer(player).getTeam() == TEAM_BLUE) {
				player.teleport(teamBlueSpawn);
			}

			PlayerData.get(player).freeze = true;//FREEZE ALL
			for (Player otherPlayer : getPlayers()) {
				GlowAPI.unGlowPlayer(player, otherPlayer);
			}
		}
		bossBar.setVisible(false);
		bossBar.removeAll();
		Bukkit.getScheduler().runTaskLater(getPlugin(), super::finishEvent, 20L * 10);
		endTimeSeconds = -1;//fix for forcefinish
		return true;
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
		e.setKeepInventory(true);
		e.getDrops().clear();
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