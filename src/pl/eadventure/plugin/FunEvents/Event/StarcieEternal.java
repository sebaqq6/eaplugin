package pl.eadventure.plugin.FunEvents.Event;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
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

import java.util.ArrayList;
import java.util.List;
/*TODO:
- Podsumowanie końca walki.[done]
- Scoreboard.[done]
- Zmienić ilość broni z 2 na 1.[done]
- Wprowadzenie (info na czacie).[done]
- Spawn protection[done]
- Wyłączać efekt glow przy niewidzialności.[done]
- Zmienić fragi na punkty.[done]
- Naprawić tab instance. [done]
- Poprawić teleport aby teleportowało ustawionym do wyjścia.[done]
- Naprawić projectile.[done]
- Czasem nie telportuje wszystkich.[nie moge powtórzyć błędu]
- MVP - nagradzanie najlepszych graczy (najwyższe ratio)[jeszcze nie zrobione]
 */

public class StarcieEternal extends FunEvent {
	Location teamRedSpawn;
	Location teamBlueSpawn;

	final int MAX_TIME_SECONDS = 60 * 5;
	int fragsTeamRed;
	int fragsTeamBlue;
	BossBar bossBar;
	int bossBarStep;
	List<Location> blueGateCoords = new ArrayList<>();
	List<Location> redGateCoords = new ArrayList<>();
	ScoreboardManager scoreboardManager;
	Scoreboard scoreboard;
	List<Player> topPlayers = new ArrayList<>();
	List<Player> mvPlayers = new ArrayList<>();

	public StarcieEternal(String eventName, int minPlayers, int maxPlayers, boolean ownSet) {
		super(eventName, minPlayers, maxPlayers, ownSet);
		teamRedSpawn = new Location(world_utility, 250, 112, 366, 0.0F, 0.0F);
		teamBlueSpawn = new Location(world_utility, 250, 111, 477, 180.0F, 0.0F);
		this.setArenaPos(teamRedSpawn);
		bossBar = Bukkit.createBossBar(eventName, BarColor.PURPLE, BarStyle.SOLID);
		Bukkit.getScheduler().runTaskTimer(getPlugin(), this::oneSecondTimer, 20L, 20L);
	}

	@Override
	public void setStatus(int status) {
		if (status == Status.RECORDS) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.sendMessage(Utils.mm("<green><bold>Zapisy na <dark_purple>Starcie Eternal <green>właśnie wystartowały! By wziąć udział, wyposaż się w <white>pancerz <green>i dowolną <red>broń <green>- następnie użyj polecenia <yellow>/dolacz<green>. Podczas bitwy <red>nie stracisz <green>swojego ekwipunku!"));
			}
		}
		super.setStatus(status);
	}

	@Override
	public void start() {
		scoreboardManager = TabAPI.getInstance().getScoreboardManager();//update scoreboardManager (fix for reload)
		scoreboard = null;
		updateScoreboard();
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
				tp(player, teamRedSpawn);
				title(player, "<#FF0000>Drużyna czerwona", "Należysz do drużyny czerwonej.");
				teamSelector = TEAM_BLUE;
			} else if (teamSelector == TEAM_BLUE) {
				tp(player, teamBlueSpawn);
				title(player, "<#0000FF>Drużyna niebieska", "Należysz do drużyny niebieskiej.");
				teamSelector = TEAM_RED;
			}
			//show scoreboard
			TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
			scoreboardManager.showScoreboard(tabPlayer, scoreboard);
		}
		updateScoreboard();
		bossBar.setVisible(true);
		//Bukkit.getScheduler().runTaskLater(getPlugin(), () -> updateGlowTeam(GlowTeamType.AllForAll), 20L * 5);
		endTimeSeconds = MAX_TIME_SECONDS;
		bossBarStep = 0;
		setOpenRedGate(false);
		setOpenBlueGate(false);
		startCountdown(10, () -> {
			setOpenRedGate(true);
			setOpenBlueGate(true);
		});
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
		//results
		int lp = 1;
		List<String> result = new ArrayList<>();
		result.add("<gradient:#BF00FF:#7d00a7><bold>Wyniki:</bold>");
		updateScoreboard();
		for (Player oP : topPlayers) {
			EvPlayer eoP = getEvPlayer(oP);
			int kills = eoP.getInt("kills");
			int deaths = eoP.getInt("deaths");
			int ratio = kills - deaths;
			String color = eoP.getTeam() == TEAM_RED ? "red" : "blue";
			boolean isMvp = mvPlayers.contains(oP);
			String mvpMark = "<gold>✫";
			result.add(String.format("<grey><bold>%d</bold>. <%s>%s <grey>(<red>\uD83D\uDDE1 %s<grey> | <white>\uD83D\uDC80 %d<grey> | <green>\uD83C\uDF00 %d<gray>) %s", lp, color, oP.getName(), kills, deaths, ratio, isMvp ? mvpMark : ""));
			lp++;
		}
		for (String msg : result) {
			Bukkit.getConsoleSender().sendMessage(Utils.mm(msg));
		}
		//loop for all event players
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
				tp(player, teamRedSpawn);
			} else if (getEvPlayer(player).getTeam() == TEAM_BLUE) {
				tp(player, teamBlueSpawn);
			}

			PlayerData.get(player).freeze = true;//FREEZE ALL
			for (Player otherPlayer : getPlayers()) {
				GlowAPI.unGlowPlayer(player, otherPlayer);
			}
			//show result
			for (String msg : result) {
				player.sendMessage(Utils.mm(msg));
			}
		}
		mostValuablePlayers.clear();
		mostValuablePlayers.addAll(mvPlayers);
		bossBar.setVisible(false);
		bossBar.removeAll();
		Bukkit.getScheduler().runTaskLater(getPlugin(), super::finishEvent, 20L * 10);
		endTimeSeconds = -1;//fix for forcefinish
		return true;
	}

	public void setOpenBlueGate(boolean isOpen) {
		//Initgate
		if (blueGateCoords.isEmpty()) {
			for (int x = 247; x <= 253; x++) {
				for (int y = 112; y <= 115; y++) {
					blueGateCoords.add(new Location(world_utility, x, y, 473));
				}
			}
		}
		blueGateCoords.add(new Location(world_utility, 248, 116, 473));
		blueGateCoords.add(new Location(world_utility, 249, 116, 473));
		blueGateCoords.add(new Location(world_utility, 250, 116, 473));
		blueGateCoords.add(new Location(world_utility, 251, 116, 473));
		blueGateCoords.add(new Location(world_utility, 252, 116, 473));
		blueGateCoords.add(new Location(world_utility, 249, 117, 473));
		blueGateCoords.add(new Location(world_utility, 251, 117, 473));
		//Manage
		BlockData blockData = Material.IRON_BARS.createBlockData();
		if (blockData instanceof MultipleFacing ironBars) {
			ironBars.setFace(BlockFace.EAST, true);
			ironBars.setFace(BlockFace.WEST, true);
			if (isOpen) {
				for (Location location : blueGateCoords) {
					world_utility.setBlockData(location, Material.AIR.createBlockData());
					world_utility.spawnParticle(
							Particle.BLOCK,
							location.clone().add(0.5, 0.5, 0.5), // środek bloku
							10, // ilość cząsteczek
							0.2, 0.2, 0.2, // rozrzut
							Material.IRON_BARS.createBlockData());
					world_utility.playSound(
							location,
							Sound.BLOCK_IRON_DOOR_CLOSE,
							1.0f,
							1.0f
					);
				}
			} else {
				for (Location location : blueGateCoords) {
					world_utility.setBlockData(location, ironBars);
				}
			}
		}
	}

	public void setOpenRedGate(boolean isOpen) {
		if (redGateCoords.isEmpty()) {
			for (int x = 247; x <= 253; x++) {
				for (int y = 112; y <= 115; y++) {
					redGateCoords.add(new Location(world_utility, x, y, 370));
				}
			}
		}
		redGateCoords.add(new Location(world_utility, 248, 116, 370));
		redGateCoords.add(new Location(world_utility, 249, 116, 370));
		redGateCoords.add(new Location(world_utility, 250, 116, 370));
		redGateCoords.add(new Location(world_utility, 251, 116, 370));
		redGateCoords.add(new Location(world_utility, 252, 116, 370));
		redGateCoords.add(new Location(world_utility, 249, 117, 370));
		redGateCoords.add(new Location(world_utility, 250, 117, 370));
		redGateCoords.add(new Location(world_utility, 251, 117, 370));
		redGateCoords.add(new Location(world_utility, 250, 118, 370));
		//Manage
		BlockData blockData = Material.IRON_BARS.createBlockData();
		if (blockData instanceof MultipleFacing ironBars) {
			ironBars.setFace(BlockFace.EAST, true);
			ironBars.setFace(BlockFace.WEST, true);
			if (isOpen) {
				for (Location location : redGateCoords) {
					world_utility.setBlockData(location, Material.AIR.createBlockData());
					world_utility.spawnParticle(
							Particle.BLOCK,
							location.clone().add(0.5, 0.5, 0.5), // środek bloku
							10, // ilość cząsteczek
							0.2, 0.2, 0.2, // rozrzut
							Material.IRON_BARS.createBlockData());
					world_utility.playSound(
							location,
							Sound.BLOCK_IRON_DOOR_CLOSE,
							1.0f,
							1.0f
					);
				}
			} else {
				for (Location location : redGateCoords) {
					world_utility.setBlockData(location, ironBars);
				}
			}
		}
	}

	private void updateTopPlayers() {
		List<Player> sortedPlayers = new ArrayList<>(getPlayers());
		sortedPlayers.sort((p1, p2) -> {
			EvPlayer ep1 = getEvPlayer(p1);
			EvPlayer ep2 = getEvPlayer(p2);
			int ratio1 = ep1.getInt("kills") - ep1.getInt("deaths");
			int ratio2 = ep2.getInt("kills") - ep2.getInt("deaths");
			return Integer.compare(ratio2, ratio1); // descending
		});
		topPlayers.clear();
		topPlayers.addAll(sortedPlayers);
		//find mvp players
		mvPlayers.clear();
		boolean findMvpInRedTeam = false;
		boolean findMvpInBlueTeam = false;
		for (Player p : topPlayers) {
			EvPlayer evPlayer = getEvPlayer(p);
			int ratio = evPlayer.getInt("kills") - evPlayer.getInt("deaths");
			if (!findMvpInRedTeam && evPlayer.getTeam() == TEAM_RED && ratio > 0) {
				findMvpInRedTeam = true;
				mvPlayers.add(p);
			} else if (!findMvpInBlueTeam && evPlayer.getTeam() == TEAM_BLUE && ratio > 0) {
				findMvpInBlueTeam = true;
				mvPlayers.add(p);
			}
			if (findMvpInRedTeam && findMvpInBlueTeam) {
				break;
			}
		}
	}

	private void updateScoreboard() {
		if (scoreboardManager != null && scoreboard == null) {
			List<String> defaultList = new ArrayList<>();
			for (int i = 1; i <= 10; i++) {
				defaultList.add(String.format("<grey><bold>%d</bold>. ---", i));
			}

			scoreboard = scoreboardManager.createScoreboard("Starcie Eternal", "<gradient:#BF00FF:#7d00a7><bold>Starcie Eternal</bold>", defaultList);
		}
		updateTopPlayers();
		if (scoreboard != null) {
			int lp = 1;
			for (Player oP : topPlayers) {
				if (lp > 10) break;
				EvPlayer eoP = getEvPlayer(oP);
				int kills = eoP.getInt("kills");
				int deaths = eoP.getInt("deaths");
				int ratio = kills - deaths;
				String color = eoP.getTeam() == TEAM_RED ? "red" : "blue";
				boolean isMvp = mvPlayers.contains(oP);
				String mvpMark = "<gold>✫";
				scoreboard.getLines().get(lp - 1).setText(String.format("<grey><bold>%d</bold>. <%s>%s <grey>(<green>\uD83C\uDF00 %d<gray>) %s", lp, color, oP.getName(), ratio, isMvp ? mvpMark : ""));
				lp++;
			}
		}
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
		EvPlayer evPlayer = getEvPlayer(player);
		int points = evPlayer.getInt("kills") - evPlayer.getInt("deaths");
		if (points < 1) points = 1;
		else if (points > 5) points = 5;
		evPlayer.addInt("deaths", 1);
		e.setKeepInventory(true);
		e.getDrops().clear();
		if (evPlayer.getTeam() == TEAM_RED) {
			fragsTeamBlue += points;
		} else if (evPlayer.getTeam() == TEAM_BLUE) {
			fragsTeamRed += points;
		}
		Player killer = Utils.getPlayerKiller(e);
		if (killer != null && getPlayers().contains(killer)) {
			EvPlayer evKillerPlayer = getEvPlayer(killer);
			evKillerPlayer.addInt("kills", 1);
			title(player, " ", "<white>☠</white> <gray>Zabił/a Cię <white>" + killer.getName() + "</white>");
			title(killer, " ", "<red>\uD83D\uDDE1</red> <gray>Pokonałeś/aś <white>" + player.getName() + "</white> <grey>(+</grey><green>" + points + "<grey> pkt)");
		}
		updateScoreboard();
	}

	@Override
	public void playerRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		EvPlayer evPlayer = getEvPlayer(player);
		if (evPlayer.getTeam() == TEAM_RED) {
			tp(player, teamRedSpawn);
		} else if (evPlayer.getTeam() == TEAM_BLUE) {
			tp(player, teamBlueSpawn);
		}
		evPlayer.setSpawnProtection(6);
	}
}