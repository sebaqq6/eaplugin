package pl.eadventure.plugin.Events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import pl.eadventure.plugin.PlayerData;

public class playerInventoryOpen implements Listener {
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		LivingEntity livingEntity = e.getPlayer();
		if (livingEntity instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.creativeMode && !player.hasPermission("eadventureplugin.creative.bypass")) {
				e.setCancelled(true);
			}
		}
	}
}
