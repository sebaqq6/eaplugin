package pl.eadventure.plugin.Events;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

public class playerInteractEntityEvent implements Listener {
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(player);
		if (e.getRightClicked() instanceof ItemFrame) {
			if (pd.creativeMode && !player.hasPermission("eadventureplugin.creative.bypass")) {
				e.setCancelled(true);
				player.sendMessage(Utils.mm("<#888888>Nie możesz modyfikować ItemFrame w <b>trybie kreatywnym</b>!</#888888>"));
			}
		}
	}
}
