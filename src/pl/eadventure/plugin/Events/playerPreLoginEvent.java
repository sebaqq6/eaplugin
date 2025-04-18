package pl.eadventure.plugin.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import pl.eadventure.plugin.Commands.Command_blueflag;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.List;

public class playerPreLoginEvent implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
		if (gVar.isBanned.get(e.getAddress()) != null) {
			if (gVar.isBanned.get(e.getAddress())) {
				print.debug("Nieutoryzowany dostęp.");
				e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Nieautoryzowany dostęp!");
			}
		}
		PunishmentSystem.BanData bd = PunishmentSystem.isBanned(e.getName(), e.getAddress().getHostAddress(), e.getUniqueId());

		if (bd != null) {
			e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PunishmentSystem.getBannedMessage(bd.nick(), bd.bannedByNick(), bd.reason(), bd.expiresTimestamp(), bd.bannedDate()));
		}
		//Whitelist (blueflag)
		List<String> wl = gVar.whiteList;
		if (!wl.isEmpty()) {
			if (!wl.contains(e.getName())) {

				e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Utils.color(Command_blueflag.kickMessage.replace("<player>", e.getName())));
			}
		}
	}
}
