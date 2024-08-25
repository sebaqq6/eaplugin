package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.print;

public class playerRespawnEvent implements Listener {
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(player);
		print.debug("Gracz: " + player.getName() + " zrespawnowal sie.");
		if (pd.deathOnPVPArena == true) {
			pd.deathOnPVPArena = false;
			ItemStack[] contentsDeath = pd.itemsWhileDeath;
			ItemStack[] armorContentsDeath = pd.armorWhileDeath;
			int inventoryItemsCount = PlayerUtils.getInventoryItemsCount(player);
			int armorSetCount = PlayerUtils.getArmorSetCount(player);
			if (contentsDeath != null && inventoryItemsCount != pd.itemsCountWhileDeath) {
				player.getInventory().clear();
				player.getInventory().setContents(contentsDeath);
				print.info(String.format("Gracz: %s byl na arenie PvP - przywracanie straconego EQ (%d != %d) na arenie PvP...", player.getName(), inventoryItemsCount, pd.itemsCountWhileDeath));
			}
			if (armorContentsDeath != null && armorSetCount != pd.armorCountWhileDeath) {
				player.getInventory().setHelmet(null);
				player.getInventory().setChestplate(null);
				player.getInventory().setLeggings(null);
				player.getInventory().setBoots(null);
				player.getInventory().setArmorContents(armorContentsDeath);
				print.info(String.format("Gracz: %s byl na arenie PvP - przywracanie straconego Armora (%d != %d) na arenie PvP...", player.getName(), armorSetCount, pd.armorCountWhileDeath));
			}
		}
	}
}
