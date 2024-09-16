package pl.eadventure.plugin.Events;

import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.framework.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.PunishmentSystem;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

public class playerCommandPreprocessEvent implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();
		String command = e.getMessage();
		PlayerData pd = PlayerData.get(player);
		String[] args = command.split(" ");
		//print.debug("[PreProcessCmd]Gracz: " + player.getName() + " - CMD: " + command);
		//mute command if player muted
		if (PunishmentSystem.isMuted(player)) {
			if (args[0].equalsIgnoreCase("/czatglobalny")
					|| args[0].equalsIgnoreCase("/czatlokalny")) {
				if (args.length > 1) {// /commandchat text
					PunishmentSystem.sendPlayerMutedInfo(player);
					e.setCancelled(true);
				}
			}
		}

		if (pd.creativeMode) {
			if (!args[0].equalsIgnoreCase("/creative") && !player.isOp()) {
				player.sendMessage(Utils.mm("<#888888>Nie możesz używać komend w <b>trybie kreatywnym</b>!</#888888>"));
				e.setCancelled(true);
				return;
			}
		}
		/*if (args[0].equalsIgnoreCase("/creative")) {
			if (player.getGameMode() != GameMode.CREATIVE) {
				ItemStack[] contents = player.getInventory().getContents();
				ItemStack[] armorContents = player.getInventory().getArmorContents();
				ItemStack[] extraContents = player.getInventory().getExtraContents();
				player.getInventory().clear();
				new BukkitRunnable() {
					@Override
					public void run() {

						player.getInventory().setContents(contents);
						player.getInventory().setArmorContents(armorContents);
						player.getInventory().setExtraContents(extraContents);
						print.debug("update invent");
					}
				}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L);
			}
		}*/
		//Mob arena ticket system
		if (command.equalsIgnoreCase("/ma join") || command.equalsIgnoreCase("/ma join eternal")) {
			ItemStack mobArenaTicket = gVar.customItems.get("mobArenaTicket");
			if (PlayerUtils.getItemCount(player, mobArenaTicket) > 0) {//has mob arena ticket more than 0
				MobArena ma = EternalAdventurePlugin.getMobArena();
				Arena mainArena = ma.getArenaMaster().getArenaWithName("Eternal");
				if (mainArena.canJoin(player)) {//can join to arena
					//player.getInventory().removeItem(mobArenaTicket);
					player.getInventory().removeItemAnySlot(mobArenaTicket);
				} else {//cant join to arena - arena busy
					player.sendMessage(Utils.color("&7Nie możesz zrealizować tej operacji - arena zajęta. Spróbuj później."));
					e.setCancelled(true);
				}
			} else {//dont have mob arena ticket
				player.sendMessage(Utils.color("&7Nie możesz zrealizować tej operacji - brak biletu."));
				e.setCancelled(true);
			}
		}
		EternalAdventurePlugin.getPrivateChatEvent().onPlayerCommandProxy(e);
		/*if (player.isOp() == true) {
			if (!player.getName().equals("JrDesmond")) {
				print.error("Gracz: " + player.getName() + " ma OP - leci ban! - Anulowanie komendy: " + command);
				e.setCancelled(true);
				player.setOp(false);
				pVar.isBanned.put(player.getAddress().getAddress(), true);
				player.kickPlayer("Nieautoryzowany dostęp!");
			}
			if (args[0].equalsIgnoreCase("/lp") || args[0].equalsIgnoreCase("/luckperms")
					|| args[0].equalsIgnoreCase("/permissions") || args[0].equalsIgnoreCase("/perms")
					|| args[0].equalsIgnoreCase("/perm")) {
				print.error(
						"Gracz: " + player.getName() + " próbował zmienić LuckPerms! - Anulowanie komendy: " + command);
				e.setCancelled(true);
			}
		}*/
	}
}
