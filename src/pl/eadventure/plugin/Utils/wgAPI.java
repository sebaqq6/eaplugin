package pl.eadventure.plugin.Utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.API.GlowAPI;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class wgAPI {

	public static boolean isOnRegion(Entity p, String region) {
		RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(p.getWorld()));
		for (ProtectedRegion r : regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(p.getLocation()))) {
			if (r.getId().equalsIgnoreCase(region)) {
				return true;
			}
		}
		return false;
	}

	public static boolean leafDecayFlagDeny(Block block) {
		//long benchmark = Utils.benchmarkStart();
		RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(block.getWorld()));

		for (ProtectedRegion r : regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()))) {
			if (r.getFlag(Flags.LEAF_DECAY) != null && r.getFlag(Flags.LEAF_DECAY) == StateFlag.State.DENY) {
				//Utils.benchmarkEnd(benchmark, "leafDecayFlagDeny1");
				return true;
			}
		}
		//special for global, because getApplicableRegions as block doesn't work in global
		for (ProtectedRegion r : regionManager.getRegions().values()) {
			if (r.getId().equalsIgnoreCase("__global__")) {
				if (r.getFlag(Flags.LEAF_DECAY) != null && r.getFlag(Flags.LEAF_DECAY) == StateFlag.State.DENY) {
					//Utils.benchmarkEnd(benchmark, "leafDecayFlagDeny2");
					return true;
				}
			}
		}
		//Utils.benchmarkEnd(benchmark, "leafDecayFlagDeny3");
		return false;
	}

	public static void showWorldGuardBorder(Player p, String regionName) {
		World world = p.getWorld();
		RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
		ProtectedRegion region = regionManager.getRegion(regionName);
		if (region != null) {
			print.debug("maszyna ruszyła!");
			int playerY = p.getLocation().getBlockY(), minY = region.getMinimumPoint().getBlockY(), maxY = region.getMaximumPoint().getBlockY();
			AtomicInteger modU = new AtomicInteger(0);
			RegionTraverse.traverseRegionEdge(new HashSet<>(region.getPoints()), Collections.singletonList(region), tr -> {
				if (tr.isVertex) {
					//handleBlueParticle(p, new Location(p.getWorld(), 0.5+tr.point.getX(), 0.5+playerY, 0.5+tr.point.getZ()));
					/*Location loc = new Location(world, 0.5+tr.point.getX(), 0.5+playerY, 0.5+tr.point.getZ());
					PlayerUtils.glowBlock(p.getWorld().getBlockAt(loc), p, ChatColor.RED, 300L);
					for (int y = minY; y <= maxY; y += 5) {
						loc = new Location(world, 0.5+tr.point.getX(), 0.5+y, 0.5+tr.point.getZ());
						PlayerUtils.glowBlock(p.getWorld().getBlockAt(loc), p, ChatColor.RED, 300L);
						//handleBlueParticle(p, new Location(p.getWorld(), 0.5+tr.point.getX(), 0.5+y, 0.5+tr.point.getZ()));
					}*/
				} else {
					if (modU.get() % 2 == 0) {
						Location loc1 = new Location(world, 0.5 + tr.point.getX(), 0.5 + playerY, 0.5 + tr.point.getZ());
						Location loc2 = new Location(world, 0.5 + tr.point.getX(), 0.5 + minY, 0.5 + tr.point.getZ());
						Location loc3 = new Location(world, 0.5 + tr.point.getX(), 0.5 + maxY, 0.5 + tr.point.getZ());
						//world.strikeLightning(loc1);
						GlowAPI.glowBlock(p.getWorld().getBlockAt(loc1), p, ChatColor.RED, 300L);
						//PlayerUtils.glowBlock(p.getWorld().getBlockAt(loc2), p, ChatColor.RED, 300L);
						//PlayerUtils.glowBlock(p.getWorld().getBlockAt(loc3), p, ChatColor.RED, 300L);
						//handlePinkParticle(p, new Location(p.getWorld(), 0.5+tr.point.getX(), 0.5+playerY, 0.5+tr.point.getZ()));
						//handlePinkParticle(p, new Location(p.getWorld(), 0.5+tr.point.getX(), 0.5+minY, 0.5+tr.point.getZ()));
						//handlePinkParticle(p, new Location(p.getWorld(), 0.5+tr.point.getX(), 0.5+maxY, 0.5+tr.point.getZ()));
					}
					modU.set((modU.get() + 1) % 2);
				}
			});
		}
	}
}


