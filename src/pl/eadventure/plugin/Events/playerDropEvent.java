package pl.eadventure.plugin.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import pl.eadventure.plugin.PlayerData;

public class playerDropEvent implements Listener {
	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent e) {//gdy grazc próbuje wyrzuć przez Q
		//print.debug("Gracz: " + e.getPlayer().getName() + " wyrzuca przedmiot.");
		PlayerData pd = PlayerData.get(e.getPlayer());
		if (pd.creativeMode) {
			e.setCancelled(true);
		}
	}
}
