package pl.eadventure.plugin.Modules;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.LinkedList;
import java.util.Queue;

//TODO flagi leaf decay sprawdzane tylko raz
//TODO dynamiczne ustawianie thread sleep
public class LeavesDecay {
	private static boolean enabled = false;
	private static final int MAX_DISTANCE = 8; // Maksymalna odległość do sprawdzenia

	public static boolean enabled() {
		return enabled;
	}

	public static void active(boolean enable) {
		enabled = enable;
		if (enable) {
			print.ok("Aktywowano system automatycznego usuwania liści z drzew.");
		} else {
			print.okRed("Dezaktywowano system automatycznego usuwania liści z drzew.");
		}
	}

	// Metoda sprawdzająca, czy dany blok liścia jest połączony z drewnem
	public static boolean isLeavesConnectedToWood(Block block, Player debugPlayer) {
		//long benchmark = Utils.benchmarkStart();
		if (!Utils.isLeaves(block.getType())) {
			//Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(0-false)");
			return false;
		}
		Queue<Block> queue = new LinkedList<>();
		boolean[][][] visited = new boolean[MAX_DISTANCE * 2 + 1][MAX_DISTANCE * 2 + 1][MAX_DISTANCE * 2 + 1];
		int blockX = block.getX();
		int blockY = block.getY();
		int blockZ = block.getZ();

		queue.offer(block); // Dodanie startowego bloku do kolejki
		visited[MAX_DISTANCE][MAX_DISTANCE][MAX_DISTANCE] = true; // Oznaczenie startowego bloku jako odwiedzony

		// Algorytm BFS - przeszukiwanie wszerz
		while (!queue.isEmpty()) {
			Block currentBlock = queue.poll(); // Pobranie bloku z kolejki
			//currentBlock.breakNaturally();
			if (Utils.isWood(currentBlock.getType())) { // Jeśli znaleziono drewno, zwróć true
				//Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(true)");
				return true;
			}

			// Sprawdzanie sąsiadów
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						Block neighborBlock = currentBlock.getRelative(x, y, z); // Pobranie sąsiada
						Material neighborType = neighborBlock.getType();
						Location neighborLocation = neighborBlock.getLocation();
						int dx = neighborLocation.getBlockX() - blockX + MAX_DISTANCE; // Obliczenie współrzędnej x w tablicy odwiedzonych
						int dy = neighborLocation.getBlockY() - blockY + MAX_DISTANCE; // Obliczenie współrzędnej y w tablicy odwiedzonych
						int dz = neighborLocation.getBlockZ() - blockZ + MAX_DISTANCE; // Obliczenie współrzędnej z w tablicy odwiedzonych
						if (debugPlayer != null) {
							GlowAPI.glowBlock(currentBlock, debugPlayer, ChatColor.GREEN, 20L * 2L);
						}
						// Dodanie do kolejki również sąsiednich bloków będących blokami drzewa
						if (dx >= 0 && dx < visited.length && dy >= 0 && dy < visited[0].length && dz >= 0 && dz < visited[0][0].length) {
							if (!visited[dx][dy][dz] && Utils.isWood(neighborType)) {
								queue.offer(neighborBlock); // Dodanie sąsiada do kolejki
								visited[dx][dy][dz] = true; // Oznaczenie sąsiada jako odwiedzony
								if (debugPlayer != null) {
									GlowAPI.glowBlock(neighborBlock, debugPlayer, ChatColor.RED, 20L * 4L);
								}
								//Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(true)");
								return true;
							}
							// Sprawdzanie czy sąsiedni blok jest blokiem liścia i nie był jeszcze odwiedzony
							if (!visited[dx][dy][dz] && Utils.isLeaves(neighborType)) {
								queue.offer(neighborBlock); // Dodanie sąsiada do kolejki
								visited[dx][dy][dz] = true; // Oznaczenie sąsiada jako odwiedzony
							}
						}
					}
				}
			}
		}
		//Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(false)");
		return false; // Jeśli nie znaleziono połączenia z drewnem, zwróć false
	}

	public static boolean isBlockInContactWithOnlyAir(Block block) {
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0) continue;
					Block relativeBlock = block.getRelative(x, y, z);
					if (!relativeBlock.getType().equals(Material.AIR)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/*public static boolean isLeavesConnectedToWood(Block block, Player debugPlayer) {
		long benchmark = Utils.benchmarkStart();
		if (!Utils.isLeaves(block.getType())) {
			Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(0-false)");
			return false;
		}

		Queue<Block> queue = new LinkedList<>();
		Map<Block, Boolean> visited = new HashMap<>();

		queue.offer(block);
		visited.put(block, true);
		if (debugPlayer != null) {
			PlayerUtils.glowBlock(block, debugPlayer, Utils.isWood(block.getType()) ? ChatColor.RED : ChatColor.GREEN, Utils.isWood(block.getType()) ? 20L * 4L : 20L * 2L);
		}
		int hardLimit = 0;
		while (!queue.isEmpty()) {
			hardLimit++;
			if(hardLimit > 256) {
				Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(hardLimit)");
				return false;
			}
			Block currentBlock = queue.poll();
			if (Utils.isWood(currentBlock.getType())) {
				Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(true2)");
				return true;
			}

			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						if (x == 0 && y == 0 && z == 0) continue; // Skip the current block
						Block neighborBlock = currentBlock.getRelative(x, y, z);
						if (!visited.containsKey(neighborBlock)) {
							Material neighborType = neighborBlock.getType();
							if (Utils.isWood(neighborType) || Utils.isLeaves(neighborType)) {
								queue.offer(neighborBlock);
								visited.put(neighborBlock, true);
								if (debugPlayer != null) {
									PlayerUtils.glowBlock(neighborBlock, debugPlayer, Utils.isWood(neighborType) ? ChatColor.RED : ChatColor.GREEN, Utils.isWood(neighborType) ? 20L * 4L : 20L * 2L);
								}
								if(Utils.isWood(neighborType)) {
									Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(true)");
									return true;
								}
							}
						}
					}
				}
			}
		}

		Utils.benchmarkEnd(benchmark, "isLeavesConnectedToWood(false)");
		return false;
	}*/
}
