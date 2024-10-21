package pl.eadventure.plugin.Modules;

import com.comphenix.protocol.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;

import java.sql.Timestamp;
import java.time.Instant;

public class TimersForAllPlayers {
	static MySQLStorage storage = EternalAdventurePlugin.getMySQL();

	public static void startTimers(EternalAdventurePlugin plugin) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			Bukkit.getOnlinePlayers().forEach(TimersForAllPlayers::oneSecondTimerForAllPlayersAsync);
		}, 0L, 20L);
	}

	//1 second
	private static void oneSecondTimerForAllPlayersAsync(Player player) {//async thread
		triggerTimePlayed(player);
		calcGearScore(player);
		//Sync section--------Sync section--------Sync section--------Sync section--------Sync section--------Sync section--------
		Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> {
			fixSpectatorTeleport(player);
		});
	}

	private static void triggerTimePlayed(Player player) {
		PlayerData pd = PlayerData.get(player);
		pd.onlineSeconds++;
		pd.sessionOnlineSeconds++;
		if (pd.maxSessionOnlineSeconds < pd.sessionOnlineSeconds) pd.maxSessionOnlineSeconds = pd.sessionOnlineSeconds;
		if (pd.onlineSeconds > 59) {
			pd.onlineSeconds = 0;
			pd.onlineMinutes++;
			if (pd.onlineMinutes > 59) {
				pd.onlineMinutes = 0;
				pd.onlineHours++;
			}
			if (pd.dbid != 0)
				storage.execute(String.format("UPDATE `players` SET `onlineHours`='%d', `onlineMinutes`='%d', `onlineSeconds`='%d', `maxSessionOnlineSeconds`='%d' WHERE `id`='%d';", pd.onlineHours, pd.onlineMinutes, pd.onlineSeconds, pd.maxSessionOnlineSeconds, pd.dbid));
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
				//player.setSpectatorTarget(playerTarget);
				if (playerLocation.getWorld() != targetLocation.getWorld() || targetLocation.distance(playerLocation) > 10) {
					targetLocation.setY(targetLocation.getY() + 256);
					player.teleport(targetLocation);
					//player.setGameMode(GameMode.SPECTATOR);
					//player.setSpectatorTarget(playerTarget);
					player.setInvisible(true);
					Bukkit.getScheduler().runTaskLater(EternalAdventurePlugin.getInstance(), () -> {
						player.setInvisible(false);
						player.setGameMode(GameMode.SPECTATOR);
						player.setSpectatorTarget(playerTarget);
					}, 20L);
				}
			}
		}
	}
}
