package pl.eadventure.plugin.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permission;
import pl.eadventure.plugin.API.Placeholders;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
		if (lore != null) {
			meta.setLore(lore);
		} else {
			ArrayList<Component> loreNull = new ArrayList<>();
			meta.lore(loreNull);
		}
		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack itemWithDisplayName(ItemStack item, Component displayName, ArrayList<Component> lore) {
		//String plainText = PlainTextComponentSerializer.plainText().serialize(displayName);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(displayName);
		if (lore != null) {
			meta.lore(lore);
		} else {
			ArrayList<Component> loreNull = new ArrayList<>();
			meta.lore(loreNull);
		}
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

	public static TextComponent color(int color, String string) {
		return Component.text(string).color(TextColor.color(color));
	}

	public static Component mm(String text) { // mm, short for MiniMessage
		return MiniMessage.miniMessage().deserialize(text);
	}

	public static boolean isAdminPermissionHigherThan(String permission, String thanPermission) {
		String[] permissions = {"plhide.group.admin", "plhide.group.moderator", "plhide.group.gamemaster", "plhide.group.support"};
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

	// Split lines with minimessage tags
	public static List<String> breakLinesWithTags(String text, int maxLineLength) {
		List<String> lines = new ArrayList<>();
		Stack<String> openTags = new Stack<>();  // Przechowuje otwarte tagi

		while (text.length() > 0) {
			int breakPoint = findBreakPoint(text, maxLineLength);

			// Znajdź fragment tekstu do nowej linii
			String lineFragment = text.substring(0, breakPoint).trim();
			text = text.substring(breakPoint).trim();

			// Dodaj otwarte tagi do nowej linii
			lineFragment = rebuildLineWithTags(lineFragment, openTags);

			// Dodaj linię do listy
			lines.add(lineFragment);

			// Zaktualizuj otwarte tagi na podstawie przetworzonego fragmentu
			updateOpenTags(lineFragment, openTags);
		}

		return lines;
	}

	// Funkcja do znalezienia odpowiedniego miejsca do przerwania linii
	private static int findBreakPoint(String text, int maxLineLength) {
		int visibleLength = 0;
		int lastSpace = -1;  // Pozycja ostatniej spacji
		int breakPoint = text.length();  // Ustawiamy jako długość tekstu na początek

		boolean insideTag = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (c == '<') {
				insideTag = true;  // Znacznik otwarcia tagu
			} else if (c == '>') {
				insideTag = false; // Znacznik zamknięcia tagu
				continue;
			}

			// Jeśli nie jesteśmy wewnątrz tagu, zliczamy widoczne znaki
			if (!insideTag) {
				visibleLength++;

				// Jeśli napotkamy spację, zapisujemy jej pozycję
				if (c == ' ') {
					lastSpace = i;
				}
			}

			// Przerwij, jeśli osiągniemy maksymalną długość widocznego tekstu
			if (visibleLength >= maxLineLength) {
				// Ustaw breakPoint na ostatnią spację, jeśli jest, w przeciwnym razie podzielimy na maksymalną długość
				breakPoint = (lastSpace != -1) ? lastSpace : i + 1;
				break;
			}
		}

		// Jeśli nie znaleźliśmy miejsca do przerwania, łamiemy na maksymalnej długości
		if (breakPoint == 0) {
			breakPoint = Math.min(text.length(), maxLineLength);
		}

		return breakPoint;
	}

	// Funkcja do przebudowania linii z otwartymi tagami
	private static String rebuildLineWithTags(String lineFragment, Stack<String> openTags) {
		StringBuilder rebuiltLine = new StringBuilder();

		// Dodaj otwarte tagi na początku linii
		for (String tag : openTags) {
			rebuiltLine.append(tag);
		}

		// Dodaj rzeczywisty fragment linii
		rebuiltLine.append(lineFragment);

		return rebuiltLine.toString();
	}

	// Funkcja do aktualizacji otwartych tagów
	private static void updateOpenTags(String line, Stack<String> openTags) {
		String[] parts = line.split("(?=<)|(?<=>)"); // Dzielenie tekstu na fragmenty zawierające tagi

		for (String part : parts) {
			if (part.startsWith("<") && !part.startsWith("</")) {
				// Jeśli zaczyna się od otwarcia tagu, dodaj do stosu
				openTags.push(part);
			} else if (part.startsWith("</")) {
				// Jeśli to tag zamykający, znajdź odpowiadający otwarty tag i usuń go
				String matchingOpenTag = findMatchingOpenTag(part, openTags);
				if (matchingOpenTag != null) {
					openTags.remove(matchingOpenTag);
				}
			}
		}
	}

	// Funkcja do znajdowania pasującego otwartego tagu na podstawie zamkniętego tagu
	private static String findMatchingOpenTag(String closeTag, Stack<String> openTags) {
		// Usuwamy znacznik końcowy </ i szukamy odpowiadającego otwartego tagu
		String tagName = closeTag.replace("</", "").replace(">", "").trim();

		// Przeszukiwanie stosu od końca, aby znaleźć odpowiadający otwarty tag
		for (int i = openTags.size() - 1; i >= 0; i--) {
			String openTag = openTags.get(i);
			if (openTag.startsWith("<" + tagName)) {
				return openTag;
			}
		}
		return null;
	}
	// Split lines with minimessage tags - END

	/**
	 * Wysyła asynchroniczne zapytanie HTTPS do danego URL.
	 *
	 * @param urlString Adres URL API.
	 * @param method    Metoda HTTP (np. "GET", "POST").
	 * @param body      Treść zapytania (dla POST/PUT), lub null dla GET.
	 * @return CompletableFuture z odpowiedzią serwera.
	 */
	public static CompletableFuture<String> sendHttpsRequest(String urlString, String method, String body) {
		return CompletableFuture.supplyAsync(() -> {
			HttpURLConnection connection = null;
			try {
				// Tworzenie obiektu URL
				URL url = new URL(urlString);
				connection = (HttpURLConnection) url.openConnection();

				// Konfiguracja połączenia
				connection.setRequestMethod(method);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setDoOutput(true);

				// Jeśli jest treść zapytania (dla POST/PUT)
				if (body != null && !body.isEmpty()) {
					try (OutputStream os = connection.getOutputStream()) {
						os.write(body.getBytes());
						os.flush();
					}
				}

				// Odczyt odpowiedzi
				int responseCode = connection.getResponseCode();
				if (responseCode >= 200 && responseCode < 300) { // Kod 2xx oznacza sukces
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
						StringBuilder response = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							response.append(line);
						}
						return response.toString();
					}
				} else { // Obsługa błędów
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
						StringBuilder errorResponse = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							errorResponse.append(line);
						}
						throw new RuntimeException("Błąd HTTP " + responseCode + ": " + errorResponse.toString());
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Błąd podczas wysyłania zapytania HTTPS: " + e.getMessage(), e);
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		});
	}

	//get killer
	public static Player getPlayerKiller(PlayerDeathEvent e) {
		Player player = e.getPlayer();
		Player killer = null;
		EntityDamageEvent lastDamage = player.getLastDamageCause();
		//who kill
		if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
			Entity damager = entityEvent.getDamager();
			if (damager instanceof Player playerKiller) {
				killer = playerKiller;
			} else if (damager instanceof ThrownPotion thrownPotion) {
				if (thrownPotion.getShooter() instanceof Player shooter) {
					killer = shooter;
				}
			}
		}
		return killer;
	}
}
