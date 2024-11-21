package pl.eadventure.plugin.Events;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;

public class playerQuitEvent implements Listener {
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		print.debug("Gracz: " + player.getName() + " opuścił na serwer.");
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		if (player.isGlowing()) {
			player.setGlowing(false);
			print.debug("Dezaktywacja podświetlenia gracza... (opuścił serwer)");
		}
		PlayerData pd = PlayerData.get(player);
		if (pd.creativeMode) {//restore items before disconnect
			//TODO: BUG - server restart
			pd.creativeMode = false;
			player.getInventory().clear();
			player.getInventory().setHelmet(null);
			player.getInventory().setChestplate(null);
			player.getInventory().setLeggings(null);
			player.getInventory().setBoots(null);
			player.getInventory().setContents(pd.itemsBackupCreative);
			player.getInventory().setArmorContents(pd.armorBackupCreative);
			player.setGameMode(GameMode.SURVIVAL);
		}
		//Update players online
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(player.getName());
		storage.executeSafe("DELETE FROM playersonline WHERE nick=?", parameters);
		//Other
		if (pd.dbid != 0)
			storage.execute(String.format("UPDATE `players` SET `onlineHours`='%d', `onlineMinutes`='%d', `onlineSeconds`='%d', `maxSessionOnlineSeconds`='%d' WHERE `id`='%d';", pd.onlineHours, pd.onlineMinutes, pd.onlineSeconds, pd.maxSessionOnlineSeconds, pd.dbid));
		PlayerData.free(player);
	}
}
