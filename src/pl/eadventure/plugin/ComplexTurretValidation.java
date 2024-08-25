package pl.eadventure.plugin;

import com.nickuc.login.api.nLoginAPI;
import ct.ajneb97.api.EntityValidation;
import ct.ajneb97.model.PlayerTurret;
import dev.espi.protectionstones.PSRegion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ComplexTurretValidation extends EntityValidation {

	public ComplexTurretValidation() {
		super("eaplugin_validation");
	}

	@Override
	public boolean validate(LivingEntity livingEntity, PlayerTurret playerTurret) {
		if (livingEntity instanceof Player player) {
			//print.debug("Shoot detect for: " + player.getName());
			//Don't shoot player while login
			if (!nLoginAPI.getApi().isAuthenticated(player.getName())) {
				return false;
			}
			//Don't shoot player while vanished
			if (PlaceholderAPI.setPlaceholders(player, "%supervanish_isvanished%").equalsIgnoreCase("Yes")) {
				return false;
			}
			//Don't shoot player while god mode
			if (PlaceholderAPI.setPlaceholders(player, "%essentials_godmode%").equalsIgnoreCase("yes")) {
				return false;
			}
			UUID ownerUuid = UUID.fromString(playerTurret.getOwnerUUID());//get turret owner uuid
			//Don't shoot members/owners in cuboid
			HomesInterface ownerHi = new HomesInterface();
			ownerHi.loadFromUUID(ownerUuid);//load all cuboids owner turret
			for (PSRegion cuboid : ownerHi.getAllCuboids()) {
				int x = playerTurret.getLocation().getBlockX();
				int y = playerTurret.getLocation().getBlockY();
				int z = playerTurret.getLocation().getBlockZ();
				if (cuboid.getWGRegion().contains(x, y, z)) {//if turret in inside of cuboid
					//print.debug("Tower inside: " + cuboid.getId());
					if(cuboid.getMembers().contains(player.getUniqueId())) {
						//print.debug("Target is member - don't shoot!");
						return false;//if target is member - don't shoot
					}
					if(cuboid.getOwners().contains(player.getUniqueId())) {
						//print.debug("Target is owner - don't shoot!");
						return false;//if target is owner - don't shoot
					}
				}
			}
			//Don't shoot owner if member has been kicked from members
			HomesInterface targetHi = new HomesInterface();
			targetHi.loadFromPlayer(player);
			for (PSRegion cuboid : targetHi.getOwnerCuboids()) {
				int x = playerTurret.getLocation().getBlockX();
				int y = playerTurret.getLocation().getBlockY();
				int z = playerTurret.getLocation().getBlockZ();
				if (cuboid.getWGRegion().contains(x, y, z)) {
					//print.debug("Target is owner, turret is ex member - don't shoot!");
					return false;
				}
			}
		}
		return true;
	}
}
