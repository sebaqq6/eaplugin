package pl.eadventure.plugin.Events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Modules.ServerLogManager;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.awt.*;
import java.time.Duration;

public class playerChatEvent implements Listener {
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Player player = e.getPlayer();
		//print.debug("Gracz: " + player.getName() + " wpisał: " + e.getMessage());
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
			/*if (isPlayerInEvent(player)) {
				for (Player players : Bukkit.getOnlinePlayers()) {
					if (isPlayerInEvent(players)) {
						players.showTitle(Title.title(Utils.mm(""),
								Utils.mm("<#FF0000>" + e.getMessage()),
								Title.Times.times(Duration.ofMillis(500),
										Duration.ofMillis(7000),
										Duration.ofMillis(500))));
						players.playSound(
								players.getLocation(),                     // Lokalizacja odtworzenia dźwięku
								"my_sounds:sounds.warning",              // Ścieżka do dźwięku (namespace:sound)
								SoundCategory.MASTER,                    // Kategoria dźwięku
								0.15f,                                    // Głośność
								1.0f                                     // Ton (1.0 = standardowy ton)
						);
					}
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
			}*/
			for (Player players : Bukkit.getOnlinePlayers()) {
				if (player.getWorld().equals(players.getWorld())) {
					if (player.getLocation().distance(players.getLocation()) < 300)
						players.showTitle(Title.title(Utils.mm(""),
								Utils.mm("<#FF0000>" + e.getMessage()),
								Title.Times.times(Duration.ofMillis(500),
										Duration.ofMillis(7000),
										Duration.ofMillis(500))));
					players.playSound(
							players.getLocation(),                     // Lokalizacja odtworzenia dźwięku
							"my_sounds:sounds.warning",              // Ścieżka do dźwięku (namespace:sound)
							SoundCategory.MASTER,                    // Kategoria dźwięku
							0.15f,                                    // Głośność
							1.0f                                     // Ton (1.0 = standardowy ton)
					);
				}
			}
			isAnnMessage = true;
			e.setCancelled(true);
		}
		//logging:
		if (isAnnMessage) {
			String strLog = String.format("[ANN(Event)] %s: %s", player.getName(), e.getMessage());
			ServerLogManager.log(strLog, ServerLogManager.LogType.Chat);
		}
		//
		EternalAdventurePlugin.getPrivateChatEvent().onPlayerChatProxy(e);
		if (e.getMessage().contains("[item]")) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack != null && !itemStack.getType().isAir()) {
				Component itemName = itemStack.displayName();
				String formattedItemName = LegacyComponentSerializer.legacySection().serialize(itemName);
				e.setMessage(e.getMessage().replace("[item]", formattedItemName));
			}
		}
		/*if (player.isOp()) {
			if (!player.getName().equals("DevDesmond")) {
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
