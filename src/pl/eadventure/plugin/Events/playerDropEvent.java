package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import pl.eadventure.plugin.PlayerData;

public class playerDropEvent implements Listener {
	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent e) {//gdy grazc próbuje wyrzuć przez Q
		//print.debug("Gracz: " + e.getPlayer().getName() + " wyrzuca przedmiot.");
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(player);
		if (pd.creativeMode && !player.isOp()) {
			e.setCancelled(true);
		}
	}
}
