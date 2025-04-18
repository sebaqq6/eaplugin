package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import pl.eadventure.plugin.Commands.Command_creative;
import pl.eadventure.plugin.PlayerData;

public class playerChangeWorldEvent implements Listener {
	@EventHandler
	public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(player);
		if (pd.creativeMode && !player.hasPermission("eadventureplugin.creative.bypass")) {
			Command_creative.toggleCreative(player);
		}
	}
}
