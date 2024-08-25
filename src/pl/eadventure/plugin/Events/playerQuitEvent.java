package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;

public class playerQuitEvent implements Listener {
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		Player player = e.getPlayer();
		print.debug("Gracz: " + player.getName() + " opuścił na serwer.");
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		if(player.isGlowing()) {
			player.setGlowing(false);
			print.debug("Dezaktywacja podświetlenia gracza... (opuścił serwer)");
		}
		PlayerData pd = PlayerData.get(player);
		 if(pd.dbid != 0) storage.execute(String.format("UPDATE `players` SET `onlineHours`='%d', `onlineMinutes`='%d', `onlineSeconds`='%d', `maxSessionOnlineSeconds`='%d' WHERE `id`='%d';", pd.onlineHours, pd.onlineMinutes, pd.onlineSeconds, pd.maxSessionOnlineSeconds, pd.dbid));
		PlayerData.free(player);
	}
}
