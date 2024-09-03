package pl.eadventure.plugin.Utils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permission;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static String shuffleString(String input) {

		// Convert String to a list of Characters
		List<Character> characters = new ArrayList<>();
		for (char c : input.toCharArray()) {
			characters.add(c);
		}

		// Shuffle the list
		Collections.shuffle(characters);

		// Convert the list back to String
		StringBuilder shuffledString = new StringBuilder();
		for (char c : characters) {
			shuffledString.append(c);
		}

		return shuffledString.toString();
	}

	public static String getRoundOffValue(double value) {
		NumberFormat df = NumberFormat.getNumberInstance(Locale.UK);
		df.setMaximumFractionDigits(3);
		return df.format(value);
	}

	public static long getUnixTimestamp() {
		return Instant.now().getEpochSecond();
	}

	public static String getCurrentDate() {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return date.format(timestamp);
	}

	public static int[] convertSecondsToTime(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int remainingSeconds = totalSeconds % 3600;
		int minutes = remainingSeconds / 60;
		int seconds = remainingSeconds % 60;

		return new int[]{hours, minutes, seconds};
	}

	public static int[] convertSecondsToTimeWithDays(int totalSeconds) {
		int days = totalSeconds / (3600 * 24);
		int remainingSeconds = totalSeconds % (3600 * 24);

		int hours = remainingSeconds / 3600;
		remainingSeconds %= 3600;

		int minutes = remainingSeconds / 60;
		int seconds = remainingSeconds % 60;

		return new int[]{days, hours, minutes, seconds};
	}

	public static int[] convertTicksToTime(int ticks) {
		int totalSeconds = ticks / 20; // 20 ticków to sekunda
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		return new int[]{hours, minutes, seconds};
	}

	public static void commandUsageMessage(CommandSender sender, String commandAndParams) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Użycie: %s", commandAndParams)));
	}

	static HashMap<String, ItemStack> headsCache = new HashMap<>();

	public static ItemStack getPlayerHead(String playerName, String displayName, ArrayList<String> lore) {
		if (headsCache.containsKey(playerName)) {
			ItemStack cachePlayerHead = headsCache.get(playerName);
			SkullMeta playerHeadMeta = (SkullMeta) cachePlayerHead.getItemMeta();
			if (lore != null) playerHeadMeta.setLore(lore);
			playerHeadMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.format("&r%s", displayName)));
			cachePlayerHead.setItemMeta(playerHeadMeta);//set new SkullMeta
			return cachePlayerHead;
		}
		ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD); //create item
		SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();//get actual skull meta from playerHead
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(Bukkit.getOfflinePlayer(playerName).getUniqueId());//get player uuid from name
		if (offlinePlayer == null) return null;
		playerHeadMeta.setOwningPlayer(offlinePlayer);//set SkullMeta to player getted from uuid
		if (lore != null) playerHeadMeta.setLore(lore);
		playerHeadMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.format("&r%s", displayName)));
		playerHead.setItemMeta(playerHeadMeta);//set new SkullMeta
		headsCache.put(playerName, playerHead);
		return playerHead;
	}

	public static ItemStack itemWithDisplayName(ItemStack item, String displayName, ArrayList<String> lore) {
		if (displayName.length() > 29) {
			displayName = displayName.substring(0, 29);
			displayName += "...";
		}
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(displayName);
		if (lore != null) meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static void saveItemStackToFile(ItemStack itemStack, String fileName) {
		File folder = new File("plugins/EternalAdventurePlugin/customitems");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		File file = new File(folder, fileName + ".yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		config.set("item", itemStack);
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static HashMap<String, ItemStack> loadItemsFromFiles() {
		File folder = new File("plugins/EternalAdventurePlugin/customitems");
		if (!folder.exists()) {
			folder.mkdirs();
			return new HashMap<>();
		}
		HashMap<String, ItemStack> itemStackMap = new HashMap<>();
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".yml")) {
					YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
					ItemStack itemStack = (ItemStack) config.get("item");
					if (itemStack != null) {
						String itemName = file.getName().replace(".yml", "");
						itemStackMap.put(itemName, itemStack);
						print.debug(String.format("Wczytywanie CustomItem: %s.yml", itemName));
					}
				}
			}
		}
		return itemStackMap;
	}

	public static String color(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public static boolean isAdminPermissionHigherThan(String permission, String thanPermission) {
		String[] permissions = {"eadventureplugin.apanel", "eadventureplugin.mpanel", "eadventureplugin.gpanel", "eadventureplugin.spanel"};
		int indexPermission = -1;
		int indexThanPermission = -1;
		for (int i = 0; i < permissions.length; i++) {
			if (permission.equals(permissions[i])) {
				indexPermission = i;
			}
			if (thanPermission.equals(permissions[i])) {
				indexThanPermission = i;
			}
		}
		if (indexPermission == -1 || indexThanPermission == -1) {
			throw new IllegalArgumentException("One or both permissions are invalid.");
		}
		return indexPermission > indexThanPermission;
	}

	public static int parseTimeToMinutes(String timeParam) {//example: 1d,12h,30m
		int timeExpireBanMinutes = -1;
		String[] time = timeParam.split(",");
		int timeDays = 0, timeHours = 0, timeMinutes = 0;

		switch (time.length) {
			case 1:
				// Obsługa jednej części
				if (time[0].contains("d")) {
					timeDays = Integer.parseInt(time[0].replaceAll("\\D", ""));
				} else if (time[0].contains("h")) {
					timeHours = Integer.parseInt(time[0].replaceAll("\\D", ""));
				} else if (time[0].contains("m")) {
					timeMinutes = Integer.parseInt(time[0].replaceAll("\\D", ""));
				}
				break;
			case 2:
				// Obsługa dwóch części
				for (String part : time) {
					if (part.contains("d")) {
						timeDays = Integer.parseInt(part.replaceAll("\\D", ""));
					} else if (part.contains("h")) {
						timeHours = Integer.parseInt(part.replaceAll("\\D", ""));
					} else if (part.contains("m")) {
						timeMinutes = Integer.parseInt(part.replaceAll("\\D", ""));
					}
				}
				break;
			case 3:
				// Obsługa wszystkich trzech części
				timeDays = Integer.parseInt(time[0].replaceAll("\\D", ""));
				timeHours = Integer.parseInt(time[1].replaceAll("\\D", ""));
				timeMinutes = Integer.parseInt(time[2].replaceAll("\\D", ""));
				break;
			default:
				return -1;
		}

		if (timeDays == 0 && timeHours == 0 && timeMinutes == 0) {
			return -1;
		}

		int timeDaysToMinutes = timeDays * 60 * 24;
		int timeHoursToMinutes = timeHours * 60;
		timeExpireBanMinutes = timeDaysToMinutes + timeHoursToMinutes + timeMinutes;

		return timeExpireBanMinutes;
	}

	public static List<Permission> getAllServerPerms() {
		return new ArrayList<>(EternalAdventurePlugin.getInstance().getServer().getPluginManager().getPermissions());
	}

	public static boolean isWood(Material type) {
		//LOG
		return type == Material.MANGROVE_LOG ||
				type == Material.DARK_OAK_LOG ||
				type == Material.OAK_LOG ||
				type == Material.ACACIA_LOG ||
				type == Material.CHERRY_LOG ||
				type == Material.BIRCH_LOG ||
				type == Material.JUNGLE_LOG ||
				type == Material.SPRUCE_LOG ||
				//WOOD
				type == Material.MANGROVE_WOOD ||
				type == Material.DARK_OAK_WOOD ||
				type == Material.OAK_WOOD ||
				type == Material.ACACIA_WOOD ||
				type == Material.CHERRY_WOOD ||
				type == Material.BIRCH_WOOD ||
				type == Material.JUNGLE_WOOD ||
				type == Material.SPRUCE_WOOD ||
				//STRIPPED WOOD
				type == Material.STRIPPED_MANGROVE_WOOD ||
				type == Material.STRIPPED_DARK_OAK_WOOD ||
				type == Material.STRIPPED_OAK_WOOD ||
				type == Material.STRIPPED_ACACIA_WOOD ||
				type == Material.STRIPPED_CHERRY_WOOD ||
				type == Material.STRIPPED_BIRCH_WOOD ||
				type == Material.STRIPPED_JUNGLE_WOOD ||
				type == Material.STRIPPED_SPRUCE_WOOD ||
				//STRIPPED LOG
				type == Material.STRIPPED_MANGROVE_LOG ||
				type == Material.STRIPPED_DARK_OAK_LOG ||
				type == Material.STRIPPED_OAK_LOG ||
				type == Material.STRIPPED_ACACIA_LOG ||
				type == Material.STRIPPED_CHERRY_LOG ||
				type == Material.STRIPPED_BIRCH_LOG ||
				type == Material.STRIPPED_JUNGLE_LOG ||
				type == Material.STRIPPED_SPRUCE_LOG ||
				//CRIMSON
				type == Material.CRIMSON_STEM ||
				type == Material.CRIMSON_HYPHAE ||
				type == Material.STRIPPED_CRIMSON_STEM ||
				type == Material.STRIPPED_CRIMSON_HYPHAE ||
				//WARPED
				type == Material.WARPED_STEM ||
				type == Material.WARPED_HYPHAE ||
				type == Material.STRIPPED_WARPED_STEM ||
				type == Material.STRIPPED_WARPED_HYPHAE;
	}

	public static boolean isLeaves(Material type) {
		//LEAVES
		return type == Material.AZALEA_LEAVES ||
				type == Material.ACACIA_LEAVES ||
				type == Material.BIRCH_LEAVES ||
				type == Material.CHERRY_LEAVES ||
				type == Material.JUNGLE_LEAVES ||
				type == Material.MANGROVE_LEAVES ||
				type == Material.OAK_LEAVES ||
				type == Material.SPRUCE_LEAVES ||
				type == Material.DARK_OAK_LEAVES ||
				type == Material.FLOWERING_AZALEA_LEAVES;
		//OTHER
		//type == Material.VINE ||
		//type == Material.TWISTING_VINES ||
		//type == Material.GLOW_LICHEN;
	}

	public static int countActiveThreads(ExecutorService executor) {
		// Sprawdzamy, czy executor jest instancją ThreadPoolExecutor
		if (executor instanceof ThreadPoolExecutor) {
			ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
			// Pobieramy liczbę aktywnych wątków
			return threadPoolExecutor.getActiveCount();
		} else {
			// Jeśli executor nie jest instancją ThreadPoolExecutor, zwracamy -1 jako wartość niepoprawną
			return -1;
		}
	}

	public static long benchmarkStart() {
		return System.nanoTime();
	}

	public static void benchmarkEnd(long benchmarkStartValue, String title) {
		long endTime = System.nanoTime();
		long durationInMilliseconds = (endTime - benchmarkStartValue) / 1000000;
		print.debug("[BENCHMARK] Czas wykonania[" + title + "]: " + durationInMilliseconds + " ms");
	}

	public static Location findFreeAirBlockAbove(Location startLocation) {
		World world = startLocation.getWorld();
		int x = startLocation.getBlockX();
		int y = startLocation.getBlockY();
		int z = startLocation.getBlockZ();

		// Przeszukiwanie w górę w osi Y
		for (int i = y + 1; i < world.getMaxHeight(); i++) {
			Block block = world.getBlockAt(x, i, z);
			if (block.getType().isAir()) {
				// Sprawdź, czy następny blok również jest powietrzem
				Block blockBelow = world.getBlockAt(x, i - 1, z);
				if (blockBelow.getType().isAir()) {
					return blockBelow.getState().getLocation();
				}
			}
		}

		// Nie znaleziono wolnego bloku powietrza
		return null;
	}

	public static String translateWorldName(World world) {
		String originalWorldName = world.getName();
		String translation = "Inny";
		switch (originalWorldName) {
			case "world" -> translation = "Overworld";
			case "world_nether" -> translation = "Nether";
			case "world_the_end" -> translation = "End";
		}
		return translation;
	}

	public static String generateRandomString(int length) {
		// Definiujemy znaki, z których będziemy losować
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder result = new StringBuilder();
		Random random = new Random();

		// Losujemy 'length' znaków i dodajemy do wyniku
		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(characters.length());
			result.append(characters.charAt(randomIndex));
		}

		return result.toString();
	}

	//IP FINDER
	// Regex dla IPv4
	private static final String IPv4_PATTERN =
			"\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
					"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b" +
					"(?::\\d{1,5})?";

	// Regex dla IPv6
	private static final String IPv6_PATTERN =
			"\\b(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|" +
					"([0-9a-fA-F]{1,4}:){1,7}:|" +
					"([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +
					"([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +
					"([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
					"([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +
					"([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
					"[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +
					":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +
					"fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|" +
					"::(ffff(:0{1,4}){0,1}:){0,1}" +
					"((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}" +
					"(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])|" +
					"([0-9a-fA-F]{1,4}:){1,4}:" +
					"((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}" +
					"(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9]))\\b" +
					"(?::\\d{1,5})?";

	public static boolean containsIPAddress(String input) {
		// Połącz regexy dla IPv4 i IPv6
		String combinedPattern = IPv4_PATTERN + "|" + IPv6_PATTERN;
		Pattern pattern = Pattern.compile(combinedPattern);
		Matcher matcher = pattern.matcher(input);

		// Zwróć true, jeśli znaleziono adres IP, w przeciwnym razie false
		return matcher.find();
	}
	//-------------------------------------------

	public static int isNumber(String number) {
		int result = -1;
		try {
			result = Integer.parseInt(number);
			return result;
		} catch (NumberFormatException e) {
			return -999999999;
		}
	}

	public static void saveConfig(File file, YamlConfiguration config) {
		try {
			config.save(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}