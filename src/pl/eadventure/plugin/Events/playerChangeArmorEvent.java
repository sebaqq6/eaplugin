package pl.eadventure.plugin.Events;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.eadventure.plugin.Utils.ColorIssueResolverIA;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

public class playerChangeArmorEvent implements Listener {
	@EventHandler
	public void onPlayerChangeArmor(PlayerArmorChangeEvent e) {
		Player player = e.getPlayer();
		ItemStack oldArmor = e.getOldItem();
		ItemStack newArmor = e.getNewItem();
		print.debug("Gracz "+player.getName()+" zmienił Armor z "+oldArmor.getType()+" na "+newArmor.getType() + " SLOT: " + e.getSlotType().name());
		//----------------------------------------------------------------------------------------Item Adder color fixer
		ItemMeta newArmorMeta = newArmor.getItemMeta();
		if(newArmorMeta != null) {
			PersistentDataContainer pdc = newArmorMeta.getPersistentDataContainer();
			NamespacedKey eiKey = NamespacedKey.fromString("executableitems:ei-id");
			if (newArmorMeta instanceof LeatherArmorMeta) {
				LeatherArmorMeta newArmorLeatherMeta = (LeatherArmorMeta) newArmorMeta;
				if (pdc != null && newArmorLeatherMeta != null && eiKey != null) {
					String eiItemID = pdc.get(eiKey, PersistentDataType.STRING);
					if (eiItemID != null) {
						print.debug("Wykryto executableItemID: " + eiItemID);
						ColorIssueResolverIA ciria = gVar.colorIssueResolverIA;
						Color validColor = ciria.getValidColor(eiItemID);
						if(validColor != null) {
							if(!newArmorLeatherMeta.getColor().equals(validColor)) {
								newArmorLeatherMeta.setColor(validColor);
								newArmor.setItemMeta(newArmorLeatherMeta);
								switch (e.getSlotType()) {
									case HEAD -> player.getInventory().setHelmet(newArmor);
									case CHEST -> player.getInventory().setChestplate(newArmor);
									case LEGS -> player.getInventory().setLeggings(newArmor);
									case FEET -> player.getInventory().setBoots(newArmor);
								}
								print.info(String.format("Naprawiono kolor zbroi %s dla %s (slot: %s).", eiItemID, player.getName(), e.getSlotType().name()));
							}
						}
					}
				}
			}
		}
		//--------------------------------------------------------------------------------------------------------------
	}
}
