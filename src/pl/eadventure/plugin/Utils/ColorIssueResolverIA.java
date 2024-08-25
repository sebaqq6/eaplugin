package pl.eadventure.plugin.Utils;

import org.bukkit.Color;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class ColorIssueResolverIA {
	private HashMap<String, Color> validColors = new HashMap<>();
	public void loadDataFromConfig() {
		File file = new File("plugins/EternalAdventurePlugin/ArmorFixColors.yml");
		if (!file.exists()) {
			print.error("Nie znaleziono ArmorFixColors.yml");
			return;
		}

		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

		// Pobierz wszystkie klucze (nazwy przedmiotów)
		Set<String> keys = config.getKeys(false);

		// Odczytaj kolory dla wszystkich przedmiotów
		for (String key : keys) {
			String hex = config.getString(key + ".color");
			if (hex != null && hex.startsWith("#") && hex.length() == 7) {
				try {
					Color color = Color.fromRGB(
							Integer.valueOf(hex.substring(1, 3), 16),
							Integer.valueOf(hex.substring(3, 5), 16),
							Integer.valueOf(hex.substring(5, 7), 16)
					);
					print.ok("Wczytywanie ArmorFixColors.yml -> "+key+" = "+hex);
					validColors.put(key, color);

				} catch (NumberFormatException e) {
					print.error("Nieprawidłowy kolor dla: "+key);
				}
			} else {
				print.error("Nieprawidłowy lub brak koloru dla: "+key);
			}
		}
	}

	public Color getValidColor(String itemName) {
		if(validColors.containsKey(itemName)) {
			return validColors.get(itemName);
		}
		return null;
	}

	public void reloadConfig() {
		print.info("Przeładowuje config obiektu ColorIssueResolverIA...");
		validColors.clear();
		loadDataFromConfig();
	}

}
