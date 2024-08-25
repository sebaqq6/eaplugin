package pl.eadventure.plugin.Events;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class playerInventoryOpen implements Listener {
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		// Sprawdź, czy otwierany ekwipunek jest ekwipunkiem gracza
		/*if (event.getPlayer() != null) {
			for (int i = 0; i < event.getInventory().getSize(); i++) {
				ItemStack item = event.getInventory().getItem(i);

				if (item != null && item.getType() != Material.AIR) {
					ItemMeta meta = item.getItemMeta();

					if (meta != null) {
						List<String> lore = new ArrayList<>();
						lore.add("{gs}");
						meta.setLore(lore);
						item.setItemMeta(meta);
					}
				}
			}
		}*/
	}
}
