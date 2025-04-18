package pl.eadventure.plugin.Events;

import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.framework.Arena;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.FunEvents.FunEventsManager;
import pl.eadventure.plugin.Modules.GearScoreCalculator;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.util.Set;

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

		if (pd.creativeMode && !player.hasPermission("eadventureplugin.creative.bypass")) {
			Set<String> allowedCommands = Set.of(
					"/creative",
					"/czatlokalny",
					"/czatglobalny",
					"/adminczat",
					"/cg",
					"/cl",
					"/ca",
					"/r",
					"/msg",
					"/kick",
					"/mute",
					"/unmute",
					"/ban",
					"/unban",
					"/banlist",
					"/warn",
					"/warnlist",
					"/viewlog",
					"/kartoteka"
			);

			if (!allowedCommands.contains(args[0].toLowerCase())) {
				player.sendMessage(Utils.mm("<#888888>Nie możesz używać komend w <b>trybie kreatywnym</b>!</#888888>"));
				e.setCancelled(true);
				return;
			}
		}

		if (pd.creativeMode) {
			if (args[0].equalsIgnoreCase("/survival")) {
				player.performCommand("creative");
				e.setCancelled(true);
				return;
			}
		}
		if (command.equalsIgnoreCase("/gamemode creative") || args[0].equalsIgnoreCase("/creativemode")) {
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
					}
				}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L);
			}
		}
		//Mob arena ticket system
		if (command.equalsIgnoreCase("/ma join") || command.equalsIgnoreCase("/ma join eternal")) {
			ItemStack mobArenaTicket = gVar.customItems.get("mobArenaTicket");
			if (PlayerUtils.getItemCount(player, mobArenaTicket) > 0) {//has mob arena ticket more than 0
				MobArena ma = EternalAdventurePlugin.getMobArena();
				Arena mainArena = ma.getArenaMaster().getArenaWithName("Eternal");

				if (mainArena.canJoin(player)) {//can join to arena
					//check valid inventory
					if (!FunEventsManager.inventoryHasOnlySet(player)) {
						e.setCancelled(true);
					} else {
						player.getInventory().removeItemAnySlot(mobArenaTicket);
					}
					//player.getInventory().removeItemAnySlot(mobArenaTicket);
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
			if (!player.getName().equals("DevDesmond")) {
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
