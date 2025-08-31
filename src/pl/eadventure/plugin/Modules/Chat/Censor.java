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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Censor {

	private final Map<String, String> filters = new HashMap<>();
	private final File file;
	private FileConfiguration config;

	private static final Set<String> ALLOWED_DOMAINS = Set.of(
			"play.eadventure.pl",
			"eadventure.pl",
			"sklep.eadventure.pl",
			"discord.eadventure.pl"
	);
	private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");
	private static final Pattern DOMAIN_PATTERN = Pattern.compile("(?i)(https?:\\/\\/)?(www\\.)?([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

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

		// --- 1. Filtry z pliku (np. wulgaryzmy) ---
		for (Map.Entry<String, String> entry : filters.entrySet()) {
			TextReplacementConfig config = TextReplacementConfig.builder()
					.match(Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE))
					.replacement(entry.getValue())
					.build();
			updated = updated.replaceText(config);
		}

		// --- 2. Cenzura adresów IP ---
		updated = updated.replaceText(builder -> builder
				.match(IP_PATTERN)
				.replacement("play.eadventure.pl")
		);

		// --- 3. Cenzura domen (z http/https + www) ---
		updated = updated.replaceText(builder -> builder
				.match(DOMAIN_PATTERN)
				.replacement((mr, b) -> {
					Matcher matcher = DOMAIN_PATTERN.matcher(mr.group());
					if (matcher.find()) {
						String domain = matcher.group(3).toLowerCase();
						if (ALLOWED_DOMAINS.contains(domain)) {
							return Component.text(mr.group());
						}
					}
					return Component.text("https://eadventure.pl");
				})
		);


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
