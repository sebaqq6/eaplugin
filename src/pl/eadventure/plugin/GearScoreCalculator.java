package pl.eadventure.plugin;

import com.google.common.collect.Multimap;
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

	static Map<String, Integer> cache = new HashMap<>();

	public static void loadConfig() {
		cache.clear();
		loadMultipliers(fileConfigEnchants, mpEnchants);// Load enchants multipliers
		loadMultipliers(fileConfigAttr, mpAttr);// Load attributes multipliers
		loadMultipliers(fileConfigStockItems, mpStockItem);// Load stock items multipliers
		loadMultipliers(fileConfigCustomItems, mpCustomItem);// Load custom items multipliers
	}

	static private void loadMultipliers(File file, Map<String, Integer> map) {
		map.clear(); //clear cache when new data is load
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

	public int calcGearScore(ItemStack item) {
		// Check cache
		if (cache.containsKey(item.toString())) {
			print.debug("[GET FROM CACHE] gs: " + cache.get(item.toString()));
			return cache.get(item.toString());
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
		cache.put(item.toString(), gearScore);
		print.debug("PUT TO CACHE");
		print.debug("---------------" + cache.size() + "-----------------");
		int cacheCounter = 0;
		for (String i : cache.keySet()) {
			cacheCounter++;
			print.debug("*" + cacheCounter + "* " + i.toString());
		}
		print.debug("--------------------------------");
		return gearScore;
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

}
