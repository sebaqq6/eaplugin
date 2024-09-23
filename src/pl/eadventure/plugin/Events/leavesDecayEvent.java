package pl.eadventure.plugin.Events;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.LeavesDecay;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.Utils.wgAPI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class leavesDecayEvent implements Listener {
	private static final long CHECK_INTERVAL = 5 * 60 * 1000;
	private static final long CLEANUP_INTERVAL = 20L * 60L * 30L;//20 * 60 * minutes
	private static final int MAX_THREADS = 2;
	private static final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
	private static int statAllClearedCache = 0;
	private static final Map<Block, Long> lastCheckMap = new HashMap<>();
	private static final Map<Block, Boolean> decayIgnoredByFlag = new HashMap<>();
	private static long sleepDecayThread = 60L;

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent e) {
		e.setCancelled(true);
		if (LeavesDecay.enabled()) {
			Block block = e.getBlock();
			if (decayIgnoredByFlag.containsKey(block)) return;
			long currentTime = System.currentTimeMillis();

			if (!lastCheckMap.containsKey(block) || currentTime - lastCheckMap.get(block) >= CHECK_INTERVAL) {
				int activeThreads = Utils.countActiveThreads(executor);
				if (activeThreads != -1 && activeThreads < MAX_THREADS) {
					if (print.getDebug()) {
						//print.debug(String.format("Sprawdzam liścia: %d, %d, %d (%s)", block.getX(), block.getY(), block.getZ(), block.getWorld().getName()));
					}
					lastCheckMap.put(block, currentTime);
					executor.execute(() -> {
						if (!wgAPI.leafDecayFlagDeny(block)) {
							if (!LeavesDecay.isLeavesConnectedToWood(block, null)) {
								if (print.getDebug()) {
									//print.debug(String.format("Usuwam liścia: %d, %d, %d (%s)", block.getX(), block.getY(), block.getZ(), block.getWorld().getName()));
								}
								//remove more leaves per one tick
								final int radius = 1;
								for (int x = -radius; x <= radius; x++) {
									for (int y = -radius; y <= radius; y++) {
										for (int z = -radius; z <= radius; z++) {
											if (x == 0 && y == 0 && z == 0) continue;
											Block adjacentBlock = block.getRelative(x, y, z);
											if (Utils.isLeaves(adjacentBlock.getType())) {
												removeBlockAsynchronously(adjacentBlock);
											}
										}
									}
								}

								//single leaves clear
								//long startSingleLeaves = Utils.benchmarkStart();
								final int extendedRadius = 5;
								int maxRemoveSingleLeaves = 0;
								for (int x = -extendedRadius; x <= extendedRadius; x++) {
									for (int y = -extendedRadius; y <= extendedRadius; y++) {
										for (int z = -extendedRadius; z <= extendedRadius; z++) {
											if (maxRemoveSingleLeaves >= 8) continue;
											Block nearbyBlock = block.getRelative(x, y, z);
											if (nearbyBlock.getType().isAir()) continue; // Skip air blocks
											if (!Utils.isLeaves(nearbyBlock.getType())) continue;
											if (LeavesDecay.isLeavesConnectedToWood(nearbyBlock, null)) continue;
											maxRemoveSingleLeaves++;
											removeBlockAsynchronously(nearbyBlock);
										}
									}
								}
								//Utils.benchmarkEnd(startSingleLeaves, "SingleLeaves");
								removeBlockAsynchronously(block);
							}
						} else {
							decayIgnoredByFlag.put(block, true);//add if decay has decay-leaf:deny flag
						}
						try {
							Thread.sleep(sleepDecayThread);
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
					});
				}
			}
		}
	}

	private void removeBlockAsynchronously(Block block) {
		new BukkitRunnable() {
			@Override
			public void run() {
				block.breakNaturally();
			}
		}.runTask(EternalAdventurePlugin.getInstance());
	}

	/*public static void putLeafAsCheck(Block block) {
		long currentTime = System.currentTimeMillis();
		if (!lastCheckMap.containsKey(block)) {
			lastCheckMap.put(block, currentTime);
		}
	}*/

	private static boolean initCleanupBufferActive = false;

	public static void initCleanupBuffer() {
		if (initCleanupBufferActive) return;
		initCleanupBufferActive = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				cleanupLastCheckMap();
			}
		}.runTaskTimer(EternalAdventurePlugin.getInstance(), CLEANUP_INTERVAL, CLEANUP_INTERVAL);
	}

	private static void cleanupLastCheckMap() {
		int deletedCount = 0;
		long currentTime = System.currentTimeMillis();
		Iterator<Map.Entry<Block, Long>> iterator = lastCheckMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Block, Long> entry = iterator.next();
			if (currentTime - entry.getValue() >= CLEANUP_INTERVAL) {
				iterator.remove(); // Usuwamy wpis z mapy, jeśli minął czas od ostatniego sprawdzenia
				deletedCount++;
			}
		}
		statAllClearedCache += deletedCount;
		print.debug(String.format("Wyczyszczono cache liści, usunięto: %d wpisów.", deletedCount));
	}

	public static void getStats(CommandSender sender) {
		sender.sendMessage("Status systemu usuwania liści: " + LeavesDecay.enabled());
		sender.sendMessage(String.format("Ilość liści w pamięci cache: %d", lastCheckMap.size()));
		sender.sendMessage(String.format("Ilość wyczyszczonych pozycji z cache: %d", statAllClearedCache));
		sender.sendMessage(String.format("Ilość perm cache dla leafDecayFlag: %d", decayIgnoredByFlag.size()));
		sender.sendMessage(String.format("Aktywne wątki: %d/%d", Utils.countActiveThreads(executor), MAX_THREADS));
	}

	public static void setThreadSleep(long ms) {
		sleepDecayThread = ms;
	}
}
