package pl.eadventure.plugin.Modules;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;

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
}
