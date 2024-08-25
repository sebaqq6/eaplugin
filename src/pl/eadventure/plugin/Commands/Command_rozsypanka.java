package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Command_rozsypanka implements TabExecutor {
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) {
			List<String> cmdlist = new ArrayList<>();
			cmdlist = Arrays.asList("500", "1000", "5000");
			return StringUtil.copyPartialMatches(args[0], cmdlist, new ArrayList<>());
		}
		else if (args.length == 2) {
			List<String> cmdlist = new ArrayList<>();
			cmdlist = Arrays.asList("MASŁO", "HASŁO", "DRZEWO");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		} else
			return Collections.emptyList();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(String.format("Użyj /%s [kwota] [hasło do rozwiązania]", label));
			return true;
		}
		if (args.length == 1) {
			sender.sendMessage(String.format("Użyj /%s %s [hasło do rozwiązania]", label, args[0]));
			return true;
		}
		//kwota
		double money = 0.0;
		try {
			money = Double.parseDouble(args[0]);
		} catch(NumberFormatException e) {
			sender.sendMessage("Nieprawidłowa kwota (wartość nie jest cyfrą).");
			return true;
		}
		if(money > 50000 || money < 100) {
			sender.sendMessage("Nieprawidłowa kwota (od 100$ do 50000$).");
			return true;
		}
		//hasło
		if(args[1].length() < 3) {
			sender.sendMessage("Hasło jest za krótkie.");
			return true;
		}
		if(args[1].length() > 10) {
			sender.sendMessage("Hasło jest za długie.");
			return true;
		}
		print.debug("Rozsypanka: " + args[1]);
		String shuffledPassword = Utils.shuffleString(args[1].toUpperCase());
		int errorShuffleCount = 0;
		while(args[1].equals(shuffledPassword))
		{
			shuffledPassword = Utils.shuffleString(args[1].toUpperCase());
			errorShuffleCount++;
			if(errorShuffleCount > 10) {
				sender.sendMessage("Nie można ułożyć hasła z podanych liter: " + shuffledPassword);
				return true;
			}
		}
		PlayerUtils.sendColorMessageToAll(String.format("&f&l[&6&lROZSYPANKA&f&l] &7Ułóż hasło z liter: &c&l%s &7(&a&l+ &e&l$%s&7)", shuffledPassword, Utils.getRoundOffValue(money)));
		gVar.scatteredResult = args[1];
		gVar.scatteredBounty = money;
		return true;
	}

	public static void autoInit() {
		//load data from YML
		File file = new File("plugins/EternalAdventurePlugin/rozsypanka.yml");
		if(!file.exists()) {
			print.error("Nie znaleziono rozsypanka.yml");
			return;
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		List<String> commandParam = new ArrayList<>();
		commandParam =  config.getStringList("params");
		print.ok("Wczytano rozsypanka.yml");
		/*for (String entry : commandList) {
			print.debug(entry);
		}*/
		Collections.shuffle(commandParam);
		List<String> finalCommandParam = commandParam;
		new BukkitRunnable() {
			final int endIndex = finalCommandParam.size();
			int actualIndex = 0;
			@Override
			public void run() {
				String commandLine = "rozsypanka " + finalCommandParam.get(actualIndex);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
				//print.debug(commandLine);
				actualIndex++;
				if(actualIndex >= endIndex) actualIndex = 0;
			}
		//}.runTaskTimer(EternalAdventurePlugin.getInstance(), 20L*60L, 20*60L);
		}.runTaskTimer(EternalAdventurePlugin.getInstance(), 20L*60L*30L, 20L*60L*30L);
	}
}
