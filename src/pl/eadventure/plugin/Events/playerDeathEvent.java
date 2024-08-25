package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.Utils.wgAPI;

public class playerDeathEvent implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		e.setDeathMessage(null);
		PlayerData pd = PlayerData.get(player);
		print.debug("Gracz: " + player.getName() + " zdech.");
		pd.itemsWhileDeath = player.getInventory().getContents();
		pd.armorWhileDeath = player.getInventory().getArmorContents();
		pd.itemsCountWhileDeath = PlayerUtils.getInventoryItemsCount(player);
		pd.armorCountWhileDeath = PlayerUtils.getArmorSetCount(player);
		if (wgAPI.isOnRegion(e.getEntity(), "_arena_") == true) {
			print.info(String.format("Gracz: %s byl na arenie PvP - drop anulowany (EQ: %d Armor: %d)!", player.getName(), pd.itemsCountWhileDeath, pd.armorCountWhileDeath));
			pd.deathOnPVPArena = true;
			e.setKeepInventory(true);
			e.getDrops().clear();
		} else pd.deathOnPVPArena = false;
		if(e.getKeepInventory() == false && pd.deathOnPVPArena)
			print.error("Gracz: " + player.getName() + " - setKeepInventory(false); - Na arenie PvP!");
	}
}
