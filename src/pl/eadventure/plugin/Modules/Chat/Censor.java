package pl.eadventure.plugin.Modules.Chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Censor {

	private final Map<String, String> filters = new HashMap<>();
	private final File file;
	private FileConfiguration config;

	public Censor(String filePath) {
		this.file = new File(filePath);
		loadFile();
	}

	private void loadFile() {
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		config = YamlConfiguration.loadConfiguration(file);
	}

	public void loadFilters() {
		filters.clear();

		List<String> list = config.getStringList("filters");
		for (String entry : list) {
			String[] parts = entry.split(",", 2);
			if (parts.length == 2) {
				filters.put(parts[0], parts[1]);
			}
		}
	}

	public Component filterMessage(Component message) {
		Component updated = message;

		for (Map.Entry<String, String> entry : filters.entrySet()) {
			TextReplacementConfig config = TextReplacementConfig.builder()
					.matchLiteral(entry.getKey())
					.replacement(entry.getValue())
					.build();

			updated = updated.replaceText(config);
		}

		return updated;
	}

	public String filterMessage(String message) {
		String result = message;
		for (Map.Entry<String, String> entry : filters.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}


	public Map<String, String> getFilters() {
		return filters;
	}

	public void reload() {
		loadFile();
		loadFilters();
	}
}
