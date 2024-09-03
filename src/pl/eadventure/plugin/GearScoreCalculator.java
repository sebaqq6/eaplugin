package pl.eadventure.plugin;

import com.google.common.collect.Multimap;
import org.bukkit.Keyed;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.io.File;
import java.util.Map;
import java.util.jar.Attributes;

public class GearScoreCalculator {
	static File fileConfig = new File("plugins/EternalAdventurePlugin/gearscore/config.yml");
	static File fileConfigEnchants = new File("plugins/EternalAdventurePlugin/gearscore/enchants.yml");
	static File fileConfigAttr = new File("plugins/EternalAdventurePlugin/gearscore/attributes.yml");
	static File fileConfigStockItems = new File("plugins/EternalAdventurePlugin/gearscore/stockItems.yml");
	static File fileConfigCustomItems = new File("plugins/EternalAdventurePlugin/gearscore/customItems.yml");

	public static void loadConfig() {


	}

	public int calcGearScore(ItemStack item) {
		//enchants
		Map<Enchantment, Integer> enchants = item.getEnchantments();

		int gearScore = 0;

		for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
			Enchantment enchant = entry.getKey();
			int level = entry.getValue();
			print.debug(enchant.getKey().getKey() + " (lvl: " + level + ")");
			gearScore += level * getScoreForEnchantment(enchant);
		}
		//attributes
		item.getItemMeta().getAttributeModifiers();
		Multimap<Attribute, AttributeModifier> attributes = item.getItemMeta().getAttributeModifiers();
		if (attributes != null) {
			for (Map.Entry<Attribute, AttributeModifier> attribute : attributes.entries()) {
				double level = attribute.getValue().getAmount();
				print.debug(attribute.getKey().getKey().getKey().replaceAll("generic.", "") + " (lvl: " + level + ")");
			}
		}

		return gearScore;
	}


	private int getScoreForEnchantment(Enchantment enchantment) {

		if (enchantment.equals(Enchantment.BANE_OF_ARTHROPODS)) {
			return 10;
		}
		return 0;
	}

}
