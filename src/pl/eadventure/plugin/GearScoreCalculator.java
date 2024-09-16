package pl.eadventure.plugin;

import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.yaml.snakeyaml.Yaml;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

public class GearScoreCalculator {
	static File fileConfig = new File("plugins/EternalAdventurePlugin/gearscore/config.yml");
	static File fileConfigEnchants = new File("plugins/EternalAdventurePlugin/gearscore/enchants.yml");
	static File fileConfigAttr = new File("plugins/EternalAdventurePlugin/gearscore/attributes.yml");
	static File fileConfigStockItems = new File("plugins/EternalAdventurePlugin/gearscore/stockItems.yml");
	static File fileConfigCustomItems = new File("plugins/EternalAdventurePlugin/gearscore/customItems.yml");

	static Map<String, Integer> mpEnchants = new HashMap<>();
	static Map<String, Integer> mpAttr = new HashMap<>();
	static Map<String, Integer> mpStockItem = new HashMap<>();
	static Map<String, Integer> mpCustomItem = new HashMap<>();
	static String gsTitleForStocks = null;
	static String gsValueColorStart = null;
	static String gsValueColorEnd = null;
	static int gsValueMax = 1000;

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
			Utils.saveConfig(fileConfig, config);
		}
		gsTitleForStocks = config.getString("gsTitleForStocks");
		gsValueColorStart = config.getString("gsValueColorStart");
		gsValueColorEnd = config.getString("gsValueColorEnd");
		gsValueMax = config.getInt("gsValueMax");
		print.ok("Wczytano config GearScoreCalculator!");
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
			int multiplier = config.getInt(key);
			map.put(key, multiplier);
			//print.debug(String.format("load %s mp: %d", key, multiplier));
		}
	}

	public int calcGearScore() {
		// Check cache
		if (cacheGsValues.containsKey(item.toString())) {
			print.debug("[GET FROM CACHE] gs: " + cacheGsValues.get(item.toString()));
			this.gearScore = cacheGsValues.get(item.toString());
			return this.gearScore;
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
						print.debug("CustomDetect: " + eiItemID);
					}
				}
			}
		}
		//detect stock items
		boolean isStock = false;
		if (!isCustom) {
			if (mpStockItem.containsKey(item.getType().toString())) {
				Set<ItemFlag> itemFlags = item.getItemFlags();
				if (!itemFlags.contains(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
						&& !itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)
						&& !itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
					print.debug("StockDetect: " + item.getType());
					gearScore += mpStockItem.get(item.getType().toString());
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

					//print.debug(finalAttribute + " (lvl: " + level + ")");
					/*YamlConfiguration config = YamlConfiguration.loadConfiguration(fileConfigAttr);
					config.set(finalAttribute, 10);
					Utils.saveConfig(fileConfigAttr, config);*/
				}
			}
		}

		//add to cache
		this.gearScore = gearScore;
		cacheGsValues.put(item.toString(), gearScore);
		print.debug("PUT TO CACHE");
		print.debug("---------------" + cacheGsValues.size() + "-----------------");
		int cacheCounter = 0;
		for (String i : cacheGsValues.keySet()) {
			cacheCounter++;
			print.debug("*" + cacheCounter + "* " + i.toString());
		}
		print.debug("--------------------------------");
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
		if (attribute.getValue().getKey().value().contains("base")) return 0;
		if (mpAttr.containsKey(finalAttribute)) {
			return mpAttr.get(finalAttribute);
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

	public String getGsValueColored(int gs) {
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
	}
}
