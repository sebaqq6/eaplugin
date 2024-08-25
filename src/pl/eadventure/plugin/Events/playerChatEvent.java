package pl.eadventure.plugin.Events;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.*;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

public class playerChatEvent implements Listener {
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Player player = e.getPlayer();
		print.debug("Gracz: " + player.getName() + " wpisał: "+ e.getMessage());
		PlayerData pd = PlayerData.get(player);
		//-----------------------------------------------------------------------
		boolean isAnnMessage = false;
		//mute event
		if (PunishmentSystem.isMuted(player)) {
			PunishmentSystem.sendPlayerMutedInfo(player);
			e.setCancelled(true);
			return;
		}
		//rozsypanka
		if (gVar.scatteredResult != null) {//if scattered in progress...
			new BukkitRunnable() {//delay
				public void run() {
					if (gVar.scatteredResult == null) return;//check in main thread scattered is null
					if (gVar.scatteredResult.equalsIgnoreCase(e.getMessage())) {
						EternalAdventurePlugin.getEconomy().depositPlayer(player, gVar.scatteredBounty);
						String msg = String.format("&f&l[&6&lROZSYPANKA&f&l] &a&l%s &7odgadł/a hasło: &d&l%s&7. " +
								"Wygrywa: &e&l%s$&7.", player.getName(), gVar.scatteredResult, Utils.getRoundOffValue(gVar.scatteredBounty));
						PlayerUtils.sendColorMessageToAll(msg);
						gVar.scatteredResult = null;
						gVar.scatteredBounty = 0.0;
					}
				}
			}.runTaskLater(EternalAdventurePlugin.getInstance(), 5L);
		}
		//event ann chat
		if (pd.eventAnnChat) {
			if (isPlayerInEvent(player)) {
				for (Player players : Bukkit.getOnlinePlayers()) {
					if (isPlayerInEvent(players))
						players.sendTitle("", ChatColor.translateAlternateColorCodes('&', "&e" + e.getMessage()), 10,
								140, 10);
				}
				isAnnMessage = true;
				e.setCancelled(true);
			} else {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&7Wiadomości na środku ekranu: &c&lwyłączone&7. Jesteś poza eventem!"));
				player.sendMessage(
						ChatColor.translateAlternateColorCodes('&', "&7Od teraz wiadomości będą wysyłane na czat."));
				pd.eventAnnChat = false;
				e.setCancelled(true);
			}
		}
		//logging:
		if (isAnnMessage) {
			String strLog = String.format("[ANN(Event)] %s: %s", player.getName(), e.getMessage());
			ServerLogManager.log(strLog, ServerLogManager.LogType.Chat);
		}
		//
		EternalAdventurePlugin.getPrivateChatEvent().onPlayerChatProxy(e);
		/*if (player.isOp()) {
			if (!player.getName().equals("JrDesmond")) {
				print.error("Gracz: " + player.getName() + " ma OP - Zabieram i banuje!! - Anulowanie wiadomosci: "+ e.getMessage());
				player.setOp(false);
				pVar.isBanned.put(player.getAddress().getAddress(), true);
				e.setCancelled(true);
				Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> {// Run on the next minecraft
																							// tick (because is
																							// AsyncThread)
					player.kickPlayer("Nieautoryzowany dostęp!");
				});
			}
		}*/
	}

	// Czy gracz jest na evencie
	private boolean isPlayerInEvent(Player player) {
		if (player.getWorld().getName().equalsIgnoreCase("world_event"))// world_event
			return true;
		return false;
	}
}
