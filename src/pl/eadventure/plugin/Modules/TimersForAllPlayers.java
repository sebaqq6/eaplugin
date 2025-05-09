package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.Commands.Command_blueflag;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

public class TimersForAllPlayers {
	static MySQLStorage storage = EternalAdventurePlugin.getMySQL();

	public static void startTimers(EternalAdventurePlugin plugin) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			Bukkit.getOnlinePlayers().forEach(TimersForAllPlayers::oneSecondTimerForAllPlayersAsync);
		}, 0L, 20L);
	}

	//1 second
	private static void oneSecondTimerForAllPlayersAsync(Player player) {//async thread
		banCheck(player);
		triggerTimePlayed(player);
		calcGearScore(player);
		updatePlayersOnlineVanish(player);
		saveBreakBlocks(player);
		//--------------------------------------------------------------------------
		Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> {
			oneSecondTimerForAllPlayersSync(player);
		});
	}

	//Sync section--------Sync section--------Sync section--------Sync section--------Sync section--------Sync section--------
	private static void oneSecondTimerForAllPlayersSync(Player player) {//sync thread
		fixSpectatorTeleport(player);
		MobFixer.timerFixTargetMob(player);
	}

	private static void triggerTimePlayed(Player player) {
		PlayerData pd = PlayerData.get(player);
		pd.onlineSeconds++;
		pd.sessionOnlineSeconds++;
		if (PlayerUtils.isAfk(player)) {
			pd.afkTime++;
		}
		if (pd.maxSessionOnlineSeconds < pd.sessionOnlineSeconds) pd.maxSessionOnlineSeconds = pd.sessionOnlineSeconds;
		if (pd.onlineSeconds > 59) {
			pd.onlineSeconds = 0;
			pd.onlineMinutes++;
			if (pd.onlineMinutes > 59) {
				pd.onlineMinutes = 0;
				pd.onlineHours++;
			}
			if (pd.dbid != 0) {
				storage.execute(String.format("UPDATE `players` SET `onlineHours`='%d', `onlineMinutes`='%d', `onlineSeconds`='%d', `maxSessionOnlineSeconds`='%d' WHERE `id`='%d';", pd.onlineHours, pd.onlineMinutes, pd.onlineSeconds, pd.maxSessionOnlineSeconds, pd.dbid));
				pd.updateSession();
			}
		}
	}

	private static void calcGearScore(Player player) {
		PlayerData pd = PlayerData.get(player);
		pd.gearScore = GearScoreCalculator.getPlayerGearScore(player);
	}

	private static void fixSpectatorTeleport(Player player) {
		if (player.getGameMode() == GameMode.SPECTATOR) {
			Entity specTarget = player.getSpectatorTarget();
			PlayerData pd = PlayerData.get(player);
			pd.lastSpec = Timestamp.from(Instant.now());
			if (specTarget instanceof Player playerTarget) {
				Location playerLocation = player.getLocation();
				Location targetLocation = playerTarget.getLocation();
				Timestamp playerLastTeleport = PlayerData.get(player).lastTeleport;
				Timestamp targetLastTeleport = PlayerData.get(playerTarget).lastTeleport;
				//player.setSpectatorTarget(playerTarget);
				if (playerLocation.getWorld() != targetLocation.getWorld() || playerLastTeleport.before(targetLastTeleport)) {
					if (playerLocation.getWorld() != targetLocation.getWorld()) {
						targetLocation.setY(targetLocation.getY() + 256);
						player.teleport(targetLocation);
					} else {
						PlayerData.get(player).lastTeleport = Timestamp.from(Instant.now());
					}
					player.setInvisible(true);
					//player.setGameMode(GameMode.SPECTATOR);
					player.setSpectatorTarget(null);
					if (playerLocation.getWorld() != targetLocation.getWorld()) {
						player.setGameMode(GameMode.SURVIVAL);
					}
					Bukkit.getScheduler().runTaskLater(EternalAdventurePlugin.getInstance(), () -> {
						player.setInvisible(false);
						player.setGameMode(GameMode.SPECTATOR);
						player.setSpectatorTarget(playerTarget);
					}, 20L);
				}
			}
		}
	}

	private static void updatePlayersOnlineVanish(Player player) {
		//Update players online
		ArrayList<Object> parameters = new ArrayList<>();
		int visibleValue = PlayerUtils.isVanished(player) ? 0 : 1;
		parameters.add(visibleValue);
		parameters.add(player.getName());
		storage.executeSafe("UPDATE playersonline SET visible=? WHERE nick=?", parameters);
	}

	private static void saveBreakBlocks(Player player) {
		PlayerData pd = PlayerData.get(player);
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(pd.breakBlocksCount);
		parameters.add(pd.dbid);
		storage.executeSafe("UPDATE players SET breakBlocks=? WHERE id=?;", parameters);
	}

	private static void banCheck(Player player) {
		String ip = player.getAddress().getAddress().getHostAddress();
		PunishmentSystem.BanData bd = PunishmentSystem.isBanned(player.getName(), ip, player.getUniqueId());
		if (bd != null) {
			String bannedMessage = PunishmentSystem.getBannedMessage(bd.nick(), bd.bannedByNick(), bd.reason(), bd.expiresTimestamp(), bd.bannedDate());
			Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> player.kickPlayer(bannedMessage));
		} else {
			if (gVar.whiteList.isEmpty()) return;
			if (gVar.whiteList.contains(player.getName())) return;
			String kickMessage = Command_blueflag.kickMessage.replace("<player>", player.getName());
			Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> player.kickPlayer(Utils.color(kickMessage)));
		}
	}
}
