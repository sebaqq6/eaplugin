package pl.eadventure.plugin.Modules;

import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.yaml.snakeyaml.Yaml;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class GearScoreCalculator {
	static File fileConfig = new File("plugins/EternalAdventurePlugin/gearscore/config.yml");
	static File fileConfigEnchants = new File("plugins/EternalAdventurePlugin/gearscore/enchants.yml");
	static File fileConfigAttr = new File("plugins/EternalAdventurePlugin/gearscore/attributes.yml");
	static File fileConfigStockItems = new File("plugins/EternalAdventurePlugin/gearscore/stockItems.yml");
	static File fileConfigCustomItems = new File("plugins/EternalAdventurePlugin/gearscore/customItems.yml");
	static File fileItemsDefault = new File("plugins/EternalAdventurePlugin/gearscore/itemsDefault.yml");

	static Map<String, Integer> mpEnchants = new HashMap<>();
	static Map<String, Integer> mpAttr = new HashMap<>();
	static Map<String, Integer> mpStockItem = new HashMap<>();
	static Map<String, Integer> mpCustomItem = new HashMap<>();
	static Map<String, String> typeItems = new HashMap<>();
	static String gsTitleForStocks = null;
	static String gsValueColorStart = null;
	static String gsValueColorEnd = null;
	static int gsValueMax = 1000;
	static int gsValueMaxPlayer = 5000;

	public static int disableGs = 0;
	public static boolean enabledCacheV1 = false;

	static Map<String, Integer> cacheGsValues = new HashMap<>();
	static Map<Integer, Component> cacheFormatedStock = new HashMap<>();
	static Map<Integer, String> cacheColoredGs = new HashMap<>();

	ItemStack item;
	int gearScore = 0;

	public GearScoreCalculator(ItemStack item) {
		this.item = item;
	}

	public static void loadConfig() {
		cacheGsValues.clear();
		cacheFormatedStock.clear();
		cacheColoredGs.clear();
		cachePlayerGs.clear();
		loadMultipliers(fileConfigEnchants, mpEnchants);// Load enchants multipliers
		loadMultipliers(fileConfigAttr, mpAttr);// Load attributes multipliers
		loadMultipliers(fileConfigStockItems, mpStockItem);// Load stock items multipliers
		loadMultipliers(fileConfigCustomItems, mpCustomItem);// Load custom items multipliers
		// Global config
		YamlConfiguration config = YamlConfiguration.loadConfiguration(fileConfig);
		if (!fileConfig.exists()) {
			config.set("gsTitleForStocks", "<#888888>Punkty GS:</#888888> <b>{gs}</b>");
			config.set("gsValueColorStart", "#99FF00");
			config.set("gsValueColorEnd", "#FF0000");
			config.set("gsValueMax", 1000);
			config.set("gsValueMaxPlayer", 5000);
			Utils.saveConfig(fileConfig, config);
		}
		gsTitleForStocks = config.getString("gsTitleForStocks");
		gsValueColorStart = config.getString("gsValueColorStart");
		gsValueColorEnd = config.getString("gsValueColorEnd");
		gsValueMax = config.getInt("gsValueMax");
		gsValueMaxPlayer = config.getInt("gsValueMaxPlayer");
		print.ok("Wczytano config GearScoreCalculator!");
	}

	public static void convertConfig() throws IOException {
		print.ok("Konwertowanie...");

		File from = new File("plugins/EternalAdventurePlugin/gearscore/customItems.yml");
		File convert = new File("plugins/EternalAdventurePlugin/gearscore/customItems_convert.yml");

		if (!convert.exists()) {
			List<String> lines = Files.readAllLines(from.toPath());
			BufferedWriter writer = Files.newBufferedWriter(convert.toPath());

			String lastKey = null;
			StringBuilder currentComments = new StringBuilder();

			for (String line : lines) {
				line = line.trim();
				if (line.startsWith("#")) {
					// Dodajemy komentarze do bufora
					currentComments.append(line).append("\n");
				} else if (!line.isEmpty() && line.contains(":")) {
					// Odczytujemy klucz i wartość
					String[] parts = line.split(":");
					lastKey = parts[0].trim();
					String value = parts[1].trim();

					// Zapisujemy komentarze, jeśli istnieją
					if (currentComments.length() > 0) {
						writer.write(currentComments.toString());
						currentComments.setLength(0); // Resetujemy komentarze
					}

					// Zapisujemy klucz w nowym formacie
					writer.write(lastKey + ":");
					writer.newLine();

					// Zapisujemy wartość 'gs'
					writer.write("  gs: " + value);
					writer.newLine();

					// Zapisujemy wartość 'type'
					writer.write("  type: default");
					writer.newLine();
				}
			}

			writer.close();
			print.ok("Konwersja zakończona");
		}
	}

	static private void loadMultipliers(File file, Map<String, Integer> map) {
		map.clear(); //clear data when new data is load
		if (!file.exists()) {
			print.error("Nie znaleziono pliku: " + file.getPath());
			return;
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		Set<String> keys = config.getKeys(false);
		for (String key : keys) {
			if (file.getName().equalsIgnoreCase("customItems.yml") || file.getName().equalsIgnoreCase("stockItems.yml")) {
				int multiplier = config.getInt(key + ".gs");
				typeItems.put(key, config.getString(key + ".type"));
				map.put(key, multiplier);
				//print.okRed(key + " == " + multiplier);
			} else {
				int multiplier = config.getInt(key);
				map.put(key, multiplier);
			}


			//print.debug(String.format("load %s mp: %d", key, multiplier));
		}
	}

	public int calcGearScore() {
		if (disableGs >= 1) {
			this.gearScore = 0;
			return this.gearScore;
		}
		// Check cache
		String itemString = null;
		if (enabledCacheV1) {
			itemString = item.toString();
			if (cacheGsValues.containsKey(itemString)) {
				//print.debug("[GET FROM CACHE] gs: " + cacheGsValues.get(item.toString()));
				if (cacheGsValues.get(itemString) != null) {
					this.gearScore = cacheGsValues.get(itemString);
					return this.gearScore;
				}
			}
		}
		int gearScore = 0;
		ItemMeta itemMeta = item.getItemMeta();
		boolean isCustom = false;
		//detect custom items
		if (itemMeta != null) {
			PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
			NamespacedKey eiKey = NamespacedKey.fromString("executableitems:ei-id");
			if (pdc != null && eiKey != null) {
				String eiItemID = pdc.get(eiKey, PersistentDataType.STRING);
				if (eiItemID != null) {
					if (mpCustomItem.containsKey(eiItemID)) {
						isCustom = true;
						gearScore += mpCustomItem.get(eiItemID);
						//print.debug("CustomDetect: " + eiItemID);
						//print.debug(itemString);
					}
				}
			}
		}
		//detect stock items
		boolean isStock = false;
		if (!isCustom) {
			String typeString = item.getType().toString();
			if (mpStockItem.containsKey(typeString)) {
				Set<ItemFlag> itemFlags = item.getItemFlags();
				if (!itemFlags.contains(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
						&& !itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)
						&& !itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
					//print.debug("StockDetect: " + item.getType());
					gearScore += mpStockItem.get(typeString);
					isStock = true;
				}

			}
		}

		if (isCustom || isStock) {
			//enchants
			Map<Enchantment, Integer> enchants = item.getEnchantments();

			for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
				Enchantment enchant = entry.getKey();
				int level = entry.getValue();
				//print.debug(enchant.getKey().getKey() + " (lvl: " + level + ")");
				gearScore += level * getScoreForEnchantment(enchant);
			}

			//attributes
			Multimap<Attribute, AttributeModifier> attributes = itemMeta.getAttributeModifiers();
			if (attributes != null) {
				for (Map.Entry<Attribute, AttributeModifier> attribute : attributes.entries()) {
					double level = attribute.getValue().getAmount();
					gearScore += (int) (Math.round(level) * getScoreForAttribute(attribute));
					//print.debug(attribute.getValue().getKey().value());
					//()

					/*YamlConfiguration config = YamlConfiguration.loadConfiguration(fileConfigAttr);
					config.set(finalAttribute, 10);
					Utils.saveConfig(fileConfigAttr, config);*/
				}
			}
			//default attributes
			/*if (isStock) {
				print.debug(item.getType().toString().toLowerCase());
				Map<String, Object> defaultAttributes = getDefaultAttributes(item.getType().toString().toLowerCase());
				for (Map.Entry<String, Object> attribute : defaultAttributes.entrySet()) {
					print.debug(attribute.getKey() + ": " + attribute.getValue());
				}
			}*/
		}
		//add to cache
		this.gearScore = gearScore;

		if (itemString != null) {
			cacheGsValues.put(itemString, gearScore);
		}
//		print.debug("PUT TO CACHE");
//		print.debug("---------------" + cacheGsValues.size() + "-----------------");
//		int cacheCounter = 0;
//		for (String i : cacheGsValues.keySet()) {
//			cacheCounter++;
//			print.debug("*" + cacheCounter + "* " + i.toString());
//		}
//		print.debug("--------------------------------");
		return this.gearScore;
	}


	private int getScoreForEnchantment(Enchantment enchantment) {
		String enchantmentKey = enchantment.getKey().getKey();
		if (mpEnchants.containsKey(enchantmentKey)) {
			return mpEnchants.get(enchantmentKey);
		}
		return 0;
	}

	private int getScoreForAttribute(Map.Entry<Attribute, AttributeModifier> attribute) {
		String finalAttribute = attribute.getKey().getKey().getKey().replaceAll("generic.", "");
		//print.debug("finalAttribute: " + finalAttribute);
		if (attribute.getValue().getKey().value().contains("base")) return 0;
		if (mpAttr.containsKey(finalAttribute)) {
			int level = mpAttr.get(finalAttribute);
			//print.debug(finalAttribute + " (lvl: " + level + ")");
			return level;
		}
		return 0;
	}

	public Component getFormatedGsStock() {
		if (cacheFormatedStock.containsKey(this.gearScore)) {
			return cacheFormatedStock.get(this.gearScore);
		}
		String coloredGs = getGsValueColored(this.gearScore);
		Component component = Utils.mm("<!i>" + gsTitleForStocks.replaceAll("\\{gs\\}", coloredGs));
		cacheFormatedStock.put(this.gearScore, component);
		return component;
	}

	/*public String getGsValueColored(int gs) {
		if (cacheColoredGs.containsKey(gs)) {
			return cacheColoredGs.get(gs);
		}
		gs = Math.max(0, Math.min(gs, gsValueMax));

		int startR = Integer.parseInt(gsValueColorStart.substring(1, 3), 16);
		int startG = Integer.parseInt(gsValueColorStart.substring(3, 5), 16);
		int startB = Integer.parseInt(gsValueColorStart.substring(5, 7), 16);

		int endR = Integer.parseInt(gsValueColorEnd.substring(1, 3), 16);
		int endG = Integer.parseInt(gsValueColorEnd.substring(3, 5), 16);
		int endB = Integer.parseInt(gsValueColorEnd.substring(5, 7), 16);

		double ratio = (double) gs / gsValueMax;

		int finalR = (int) (startR + (endR - startR) * ratio);
		int finalG = (int) (startG + (endG - startG) * ratio);
		int finalB = (int) (startB + (endB - startB) * ratio);

		String finalColor = String.format("%02X%02X%02X", finalR, finalG, finalB);

		String result = "<!i><#" + finalColor + ">" + gs + "</#" + finalColor + ">";
		cacheColoredGs.put(gs, result);
		return result;
	}*/
	public String getGsValueColored(int gs) {
		return getGsValueColored(gs, gsValueMax);
	}

	public String getGsValueColored(int gs, int maxGs) {
		if (cacheColoredGs.containsKey(gs) && maxGs == gsValueMax) {
			return cacheColoredGs.get(gs);
		}

		gs = Math.max(0, Math.min(gs, maxGs));

		// Podział na 7 przedziałów
		double ratio = (double) gs / maxGs;
		int segment = (int) (ratio * 8);  // 7 segmentów
		double segmentRatio = (ratio * 8) - segment;  // Stosunek wewnątrz segmentu

		int startR = 0, startG = 0, startB = 0;
		int endR = 0, endG = 0, endB = 0;

		// Definiujemy kolory startowe i końcowe dla każdego segmentu
		switch (segment) {
			case 0: // Szary -> Biały
				startR = Integer.parseInt("D3D3D3".substring(0, 2), 16); // szary
				startG = Integer.parseInt("D3D3D3".substring(2, 4), 16);
				startB = Integer.parseInt("D3D3D3".substring(4, 6), 16);

				endR = Integer.parseInt("FFFFFF".substring(0, 2), 16); // biały
				endG = Integer.parseInt("FFFFFF".substring(2, 4), 16);
				endB = Integer.parseInt("FFFFFF".substring(4, 6), 16);
				break;
			case 1: // Biały -> Zielony
				startR = Integer.parseInt("FFFFFF".substring(0, 2), 16); // biały
				startG = Integer.parseInt("FFFFFF".substring(2, 4), 16);
				startB = Integer.parseInt("FFFFFF".substring(4, 6), 16);

				endR = Integer.parseInt("00FF00".substring(0, 2), 16); // zielony
				endG = Integer.parseInt("00FF00".substring(2, 4), 16);
				endB = Integer.parseInt("00FF00".substring(4, 6), 16);
				break;
			case 2: // Zielony -> Błękitny
				startR = Integer.parseInt("00FF00".substring(0, 2), 16); // zielony
				startG = Integer.parseInt("00FF00".substring(2, 4), 16);
				startB = Integer.parseInt("00FF00".substring(4, 6), 16);

				endR = Integer.parseInt("00FFFF".substring(0, 2), 16); // błękitny
				endG = Integer.parseInt("00FFFF".substring(2, 4), 16);
				endB = Integer.parseInt("00FFFF".substring(4, 6), 16);
				break;
			case 3: // Błękitny -> Żółty
				startR = Integer.parseInt("00FFFF".substring(0, 2), 16); // błękitny
				startG = Integer.parseInt("00FFFF".substring(2, 4), 16);
				startB = Integer.parseInt("00FFFF".substring(4, 6), 16);

				endR = Integer.parseInt("FFFF00".substring(0, 2), 16); // żółty
				endG = Integer.parseInt("FFFF00".substring(2, 4), 16);
				endB = Integer.parseInt("FFFF00".substring(4, 6), 16);
				break;
			case 4: // Żółty -> Pomarańczowy
				startR = Integer.parseInt("FFFF00".substring(0, 2), 16); // żółty
				startG = Integer.parseInt("FFFF00".substring(2, 4), 16);
				startB = Integer.parseInt("FFFF00".substring(4, 6), 16);

				endR = Integer.parseInt("FFA500".substring(0, 2), 16); // pomarańczowy
				endG = Integer.parseInt("FFA500".substring(2, 4), 16);
				endB = Integer.parseInt("FFA500".substring(4, 6), 16);
				break;
			case 5: // Pomarańczowy -> Czerwony
				startR = Integer.parseInt("FFA500".substring(0, 2), 16); // pomarańczowy
				startG = Integer.parseInt("FFA500".substring(2, 4), 16);
				startB = Integer.parseInt("FFA500".substring(4, 6), 16);

				endR = Integer.parseInt("FF0000".substring(0, 2), 16); // czerwony
				endG = Integer.parseInt("FF0000".substring(2, 4), 16);
				endB = Integer.parseInt("FF0000".substring(4, 6), 16);
				break;
			case 6: // Pomarańczowy -> Czerwony
				startR = Integer.parseInt("FF0000".substring(0, 2), 16); // czerwony
				startG = Integer.parseInt("FF0000".substring(2, 4), 16);
				startB = Integer.parseInt("FF0000".substring(4, 6), 16);

				endR = Integer.parseInt("ab00eb".substring(0, 2), 16); // fioletowy
				endG = Integer.parseInt("ab00eb".substring(2, 4), 16);
				endB = Integer.parseInt("ab00eb".substring(4, 6), 16);
				break;
			default: // Fioletowy - Jasno fioletowy
				startR = Integer.parseInt("ab00eb".substring(0, 2), 16); // czerwony
				startG = Integer.parseInt("ab00eb".substring(2, 4), 16);
				startB = Integer.parseInt("ab00eb".substring(4, 6), 16);

				endR = Integer.parseInt("800080".substring(0, 2), 16); // fioletowy
				endG = Integer.parseInt("800080".substring(2, 4), 16);
				endB = Integer.parseInt("800080".substring(4, 6), 16);
				break;
		}

		// Interpolacja w wybranym segmencie
		int finalR = (int) (startR + (endR - startR) * segmentRatio);
		int finalG = (int) (startG + (endG - startG) * segmentRatio);
		int finalB = (int) (startB + (endB - startB) * segmentRatio);

		// Upewniamy się, że wartości nie wychodzą poza zakres (0-255)
		finalR = Math.min(255, Math.max(0, finalR));
		finalG = Math.min(255, Math.max(0, finalG));
		finalB = Math.min(255, Math.max(0, finalB));

		String finalColor = String.format("%02X%02X%02X", finalR, finalG, finalB);

		String result = "<#" + finalColor + ">" + gs + "</#" + finalColor + ">";
		if (maxGs == gsValueMax) {
			cacheColoredGs.put(gs, result);
		}
		return result;
	}

	//--------------------------------------------------------------------------------------------------
	private Map<String, Object> getDefaultAttributes(String itemName) {
		Map<String, Object> attributes = new HashMap<>();
		YamlConfiguration config = YamlConfiguration.loadConfiguration(fileItemsDefault);

		if (config.contains(itemName)) {
			ConfigurationSection components = config.getConfigurationSection(itemName + ".components");
			if (components != null) {
				ConfigurationSection attributeModifiers = components.getConfigurationSection("attribute_modifiers");
				if (attributeModifiers != null) {
					List<Map<?, ?>> modifiers = attributeModifiers.getMapList("modifiers");
					if (modifiers != null) {
						for (Map<?, ?> modifier : modifiers) {
							String type = (String) modifier.get("type");
							Object amount = modifier.get("amount");
							if (type != null && amount != null) {
								attributes.put(type, amount);
							}
						}
					}
				}
			}
		}
		return attributes;
	}

	//------------------------------------------------------------------PLAYER GS CALCULATOR
	static HashMap<Integer, String> cachePlayerGs = new HashMap<>();

	public static String getPlayerGearScore(Player player) {
		//Scan used slots armor
		ItemStack[] armorContents = player.getInventory().getArmorContents();
		int armorGs = 0;
		//player.sendMessage("Start: ");
		GearScoreCalculator gsc = new GearScoreCalculator(null);
		for (ItemStack armor : armorContents) {
			if (armor == null) continue;
			//armor.getType().is
			//player.sendMessage("armor: " + armor);
			gsc = new GearScoreCalculator(armor);
			armorGs += gsc.calcGearScore();
		}
		//main off hands gs
		int mainhandGs = 0;
		int offhandGs = 0;
		//Scan offhand and mainhand custom items:
		ItemStack[] inventory = player.getInventory().getContents();
		for (ItemStack weapon : inventory) {
			if (weapon == null) continue;
			if (weapon.getType().equals(Material.AIR)) continue;
			Set<ItemFlag> itemFlags = weapon.getItemFlags();

			ItemMeta itemMeta = weapon.getItemMeta();
			if (itemMeta != null) {
				boolean customDetect = false;
				PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
				NamespacedKey eiKey = NamespacedKey.fromString("executableitems:ei-id");
				if (pdc != null && eiKey != null) {
					String eiItemID = pdc.get(eiKey, PersistentDataType.STRING);
					if (eiItemID != null) {
						if (typeItems.containsKey(eiItemID)) {
							String getType = typeItems.get(eiItemID);
							//print.okRed("detect: " + getType);
							//offhand
							if (getType.equalsIgnoreCase("offhand")) {
								gsc = new GearScoreCalculator(weapon);
								int itemGs = gsc.calcGearScore();
								if (itemGs > offhandGs) {
									offhandGs = itemGs;
									customDetect = true;
								}
							} else if (getType.equalsIgnoreCase("mainhand")) { //mainhand
								gsc = new GearScoreCalculator(weapon);
								int itemGs = gsc.calcGearScore();
								if (itemGs > mainhandGs) {
									mainhandGs = itemGs;
									customDetect = true;
								}
							}
						}
					}
				}
				if (!customDetect) {//stock items
					String itemId = weapon.getType().toString();
					//print.ok("detect: " + getType);
					if (typeItems.containsKey(itemId)) {
						if (!itemFlags.contains(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
								&& !itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)
								&& !itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
							String getType = typeItems.get(itemId);
							//offhand
							if (getType.equalsIgnoreCase("offhand")) {
								gsc = new GearScoreCalculator(weapon);
								int itemGs = gsc.calcGearScore();
								if (itemGs > offhandGs) {
									offhandGs = itemGs;
								}
							} else if (getType.equalsIgnoreCase("mainhand")) { //mainhand
								gsc = new GearScoreCalculator(weapon);
								int itemGs = gsc.calcGearScore();
								if (itemGs > mainhandGs) {
									mainhandGs = itemGs;
								}
							}
						}
					}
				}
			}
		}
		//()
		//SUM
		int totalGs = armorGs + offhandGs + mainhandGs;
		// Update GS into database
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		PlayerData pd = PlayerData.get(player);
		String sql = "UPDATE players SET lastgs=? WHERE id=?;";
		pd.gsRaw = String.valueOf(totalGs);
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(totalGs);
		parameters.add(pd.dbid);
		storage.executeSafe(sql, parameters);
		//Cache
		if (cachePlayerGs.containsKey(totalGs)) {
			//print.ok("cachePlayerGs.containsKey(totalGs)");
			if (cachePlayerGs.get(totalGs) != null) return cachePlayerGs.get(totalGs);
		}
		//print.okRed("cachePlayerGs.containsKey(totalGs)");
		String result = String.valueOf(gsc.getGsValueColored(totalGs, gsValueMaxPlayer));
		//print.ok(result);
		cachePlayerGs.put(totalGs, result);
		return result;
	}

	public String getItemType(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType().isAir()) return null;
		String result = null;
		Set<ItemFlag> itemFlags = itemStack.getItemFlags();
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta != null) {
			boolean customDetect = false;
			PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
			NamespacedKey eiKey = NamespacedKey.fromString("executableitems:ei-id");
			if (pdc != null && eiKey != null) {
				String eiItemID = pdc.get(eiKey, PersistentDataType.STRING);
				if (eiItemID != null) {
					if (typeItems.containsKey(eiItemID)) {
						result = typeItems.get(eiItemID);
						//print.okRed("detect: " + getType);

					}
				}
			}
			if (!customDetect) {//stock items
				String itemId = itemStack.getType().toString();
				//print.ok("detect: " + getType);
				if (typeItems.containsKey(itemId)) {
					if (!itemFlags.contains(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
							&& !itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)
							&& !itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
						result = typeItems.get(itemId);
					}
				}
			}
		}
		return result;
	}
}
