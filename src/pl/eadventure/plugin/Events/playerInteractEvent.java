package pl.eadventure.plugin.Events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.LeavesDecay;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.wgAPI;

public class playerInteractEvent implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Block clickedBlock = e.getClickedBlock();
		PlayerData pd = PlayerData.get(player);
		//LeavesDecay DEBUG
		if(pd.decayDebug) {
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) {
				if (clickedBlock != null) {
					new BukkitRunnable() {
						@Override
						public void run() {
							if (Utils.isWood(clickedBlock.getType())) {
								player.sendMessage(Utils.color(String.format("&6%s jest drewnem.", clickedBlock.getType())));
							} else if (!Utils.isLeaves(clickedBlock.getType())) {
								player.sendMessage(Utils.color(String.format("&7%s nie jest lisciem.", clickedBlock.getType())));
							} else if (LeavesDecay.isLeavesConnectedToWood(clickedBlock, player)) {
								player.sendMessage(Utils.color(String.format("&2%s jest połączone z drewnem. Liść bezpieczny.", clickedBlock.getType())));
							} else {
								player.sendMessage(Utils.color(String.format("&c%s nie jest połączone z drewnem.", clickedBlock.getType())));
								if(wgAPI.leafDecayFlagDeny(clickedBlock)) {
									player.sendMessage(Utils.color("&aLiść nie zniknie, gdyż obowiązuje go flaga LEAF_DECAY:DENY."));
								}
								else {
									player.sendMessage(Utils.color("&dLiść może zniknąć. Brak flagi LEAF_DECAY:DENY."));
								}
							}
						}
					}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());

				}
			}
		}
	}
}