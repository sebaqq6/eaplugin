package pl.eadventure.plugin.Events;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.List;

public class playerInventoryOpen implements Listener {
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		LivingEntity livingEntity = e.getPlayer();
		if (livingEntity instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.creativeMode) {
				e.setCancelled(true);
			}
		}
	}
}
