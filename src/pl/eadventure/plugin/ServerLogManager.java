package pl.eadventure.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.regex.Pattern;
/*
[14:50:51 INFO]: JrRequeim issued server command: /apanel
[14:50:52 INFO]: [Essentials] CONSOLE issued server command: /sudo JrRequeim admin-zgub-skrzynie
[14:50:52 INFO]: Forcing JrRequeim to run: /admin-zgub-skrzynie
[14:50:52 INFO]: JrRequeim issued server command: /admin-zgub-skrzynie
*/
public class ServerLogManager extends AbstractFilter {
	static Filter myFilter;
	static Logger rootLogger;
	static MySQLStorage storage;
	boolean disabled = false;
	boolean ignoreNextCommandRun = false;

	public static void enable(MySQLStorage mySQLStorage) {
		rootLogger = (Logger) LogManager.getRootLogger();
		myFilter = new ServerLogManager();
		myFilter.start();
		rootLogger.addFilter(myFilter);
		storage = mySQLStorage;
		print.debug("ServerLogManager - ENABLED! filterCount = "+rootLogger.filterCount());
	}

	public interface LogType {
		int Other = 0;
		int Command = 1;
		int Chat = 2;
		int JoinLeave = 3;
	}

	public static void disable() {
		if (myFilter instanceof ServerLogManager) {
			((ServerLogManager) myFilter).setDisabled(true);
		}
		print.debug("ServerLogManager - DISABLED!");
	}

	public void setDisabled(boolean value) {
		disabled = value;
	}

	@Override
	public Result filter(LogEvent event) {
		if(disabled) return Result.NEUTRAL;
		if(storage == null) return Result.NEUTRAL;
		String m = removeAnsiEscapeCodes(event.getMessage().getFormattedMessage());//message
		if(m.contains("terra:reimagend/reimagend/")) {//block console spam terra generator
			return Result.DENY;
		}
		//String sender = event.getLoggerName();
		/*if(m.contains("mfnalex")) {
			return Result.DENY; // mfnalex is a ninja
		}*/
		/*for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(sender +" -> "+ message);
		}*/
		//CHAT
		//[Lokalny] [Uczestnik] [Adminchat] [Gracz] [EVP] [SVP] [VIP] [A] [M] [GM] [S]
		if(m.contains("[Lokalny]") || m.contains("[Uczestnik]") || m.contains("[Adminchat]")
		|| m.contains("[Gracz]") || m.contains("[EVP]") || m.contains("[SVP]") || m.contains("[VIP]")
		|| m.contains("[A]") || m.contains("[M]") || m.contains("[GM]") || m.contains("[S]")) {
			log(m, LogType.Chat);
			return Result.NEUTRAL;
		}
		//COMMANDS
		//disable log forcing
		if (m.contains("Forcing") && m.contains("to run: /") || m.contains("issued server command: /sudo")) {
			ignoreNextCommandRun = true;
			return Result.NEUTRAL;
		}
		if (m.contains("issued server command: /") && !m.contains("CONSOLE")) {
			if(ignoreNextCommandRun) {
				ignoreNextCommandRun = false;
				return Result.NEUTRAL;
			}
			//IGNORE
			if ((m.contains("JrDesmond") || m.contains("JrRequeim") || m.contains("MsKarolsa")) && !m.contains("dbgxd")) {
				return Result.NEUTRAL;
			}
			//ignore private message /msg and /r (because is SPY) and other chats commands
			if(m.contains("/r") || m.contains("/msg") || m.contains("/adminczat")) {
				return Result.NEUTRAL;
			}
			String finalMessage = m.replaceAll("issued server command:", "wpisał/a komendę:");
			log(finalMessage, LogType.Command);
			return Result.NEUTRAL;
		}

		//DISCONNECT
		if (m.contains("lost connection: ") && !Utils.containsIPAddress((m))) {
			String finalMessage = m.replaceAll("lost connection:", "opuścił/a serwer. Powód: ");
			log(finalMessage, LogType.JoinLeave);
			return Result.NEUTRAL;
		}

		//OTHER
		if (m.contains("[DETAIL]") || m.contains("[ADMIN]")) {
			log(m, LogType.Other);
			return Result.NEUTRAL;
		}
		return Result.NEUTRAL;
	}

	public static void log(String text, int type) {
		new BukkitRunnable() {
			@Override
			public void run() {
				ArrayList<Object> sqlParams = new ArrayList<>();
				sqlParams.add(text);
				sqlParams.add(type);
				storage.executeSafe("INSERT INTO `logserver` (`text`, `type`) VALUES (?, ?);", sqlParams);
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m");

	public static String removeAnsiEscapeCodes(String input) {
		if (input == null) return null;
		return ANSI_ESCAPE_PATTERN.matcher(input).replaceAll("");
	}
}
