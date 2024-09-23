package pl.eadventure.plugin.Events;

import ct.ajneb97.api.TurretPlaceEvent;
import dev.espi.protectionstones.PSRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.eadventure.plugin.Modules.HomesInterface;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.print;

public class playerPlaceTurret implements Listener {
	@EventHandler
	public void onPlayerPlaceTurret(TurretPlaceEvent e) {
		Player p = e.getPlayer();
		print.debug(String.format("onPlayerPlaceTurret: %s", p.getName()));
		PlayerData pd = PlayerData.get(p);
		HomesInterface hi = pd.homesInterface;
		//PlayerTurret turret = ComplexTurretsAPI.getTurretFromLocation(e.getLocation());
		//UUID uid = UUID.fromString(turret.getOwnerUUID());
		if (hi == null) {
			hi = new HomesInterface();
			hi.loadFromPlayer(p);
			pd.homesInterface = hi;
		} else {
			hi.loadFromPlayer(p);
		}
		Location location = e.getLocation();
		boolean onCuboid = false;
		for (PSRegion cuboid : hi.getAllCuboids()) {
			if (cuboid.getWGRegion().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
				onCuboid = true;
				break;
			}
		}
		if (!onCuboid && !p.isOp() && !p.hasPermission("complexturrets.admin")) {
			PlayerUtils.sendColorMessage(p, "&7Nie możesz tutaj postawić wieżyczki.");
			print.okRed(String.format("Gracz %s próbował postawić wieżyczkę w niedozwolonym miejscu.", e.getPlayer().getName()));
			e.setCancelled(true);
		}
	}
}
