package pl.eadventure.plugin.Modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
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
		print.debug("ServerLogManager - ENABLED! filterCount = " + rootLogger.filterCount());
	}

	public interface LogType {
		int Other = 0;
		int Command = 1;
		int Chat = 2;
		int JoinLeave = 3;
		int Inventory = 4;
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
		if (disabled) return Result.NEUTRAL;
		if (storage == null) return Result.NEUTRAL;
		String m = removeAnsiEscapeCodes(event.getMessage().getFormattedMessage());//message
		if (m.contains("EternalAntibot")) {//block console spam
			return Result.DENY;
		}
		//console spam from Pocket Games
		if (/*m.contains("at java.base/jdk.internal.reflect.FieldAccessorImpl.throwSetIllegalArgumentException(FieldAccessorImpl.java:228)")
				|| m.contains("at java.base/jdk.internal.reflect.FieldAccessorImpl.throwSetIllegalArgumentException(FieldAccessorImpl.java:232)")
				|| m.contains("at java.base/jdk.internal.reflect.MethodHandleObjectFieldAccessorImpl.set(MethodHandleObjectFieldAccessorImpl.java:115)")
				|| m.contains("at java.base/java.lang.reflect.Field.set(Field.java:836)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.utils.gameUtils.SkullUtil.getSkullFromPlayer(SkullUtil.java:41)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.games.Game.highscoreUpdateYML(Game.java:422)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.games.Game.access$300(Game.java:26)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.games.Game$2.run(Game.java:235)")
				|| m.contains("at org.bukkit.craftbukkit.scheduler.CraftTask.run(CraftTask.java:86)")
				|| m.contains("at org.bukkit.craftbukkit.scheduler.CraftScheduler.mainThreadHeartbeat(CraftScheduler.java:475)")
				|| m.contains("at net.minecraft.server.MinecraftServer.tickChildren(MinecraftServer.java:1770)")
				|| m.contains("at net.minecraft.server.dedicated.DedicatedServer.tickChildren(DedicatedServer.java:513)")
				|| m.contains("at net.minecraft.server.MinecraftServer.tickServer(MinecraftServer.java:1642)")
				|| m.contains("at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1342)")
				|| m.contains("at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:333)")
				|| m.contains("at java.base/java.lang.Thread.run(Thread.java:1583)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.utils.ItemUtil.getSkullFromURL(ItemUtil.java:219)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.utils.ItemUtil.getSkull(ItemUtil.java:191)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.utils.ItemUtil.getItemStack(ItemUtil.java:156)")
				|| m.contains("at PocketGames.jar//com.live.bemmamin.pocketgames.Events.onInventoryClick(Events.java:196)")
				|| m.contains("at com.destroystokyo.paper.event.executor.MethodHandleEventExecutor.execute(MethodHandleEventExecutor.java:44)")
				|| m.contains("at co.aikar.timings.TimedEventExecutor.execute(TimedEventExecutor.java:80)")
				|| m.contains("at org.bukkit.plugin.RegisteredListener.callEvent(RegisteredListener.java:70)")
				|| m.contains("at io.papermc.paper.plugin.manager.PaperEventManager.callEvent(PaperEventManager.java:54)")
				|| m.contains("at io.papermc.paper.plugin.manager.PaperPluginManagerImpl.callEvent(PaperPluginManagerImpl.java:131)")
				|| m.contains("at org.bukkit.plugin.SimplePluginManager.callEvent(SimplePluginManager.java:630)")
				|| m.contains("at net.minecraft.server.network.ServerGamePacketListenerImpl.handleContainerClick(ServerGamePacketListenerImpl.java:3306)")
				|| m.contains("at net.minecraft.network.protocol.game.ServerboundContainerClickPacket.handle(ServerboundContainerClickPacket.java:69)")
				|| m.contains("at net.minecraft.network.protocol.game.ServerboundContainerClickPacket.handle(ServerboundContainerClickPacket.java:33)")
				|| m.contains("at net.minecraft.network.protocol.PacketUtils.lambda$ensureRunningOnSameThread$0(PacketUtils.java:56)")
				|| m.contains("at net.minecraft.server.TickTask.run(TickTask.java:18)")
				|| m.contains("at net.minecraft.util.thread.BlockableEventLoop.doRunTask(BlockableEventLoop.java:151)")
				|| m.contains("at net.minecraft.util.thread.ReentrantBlockableEventLoop.doRunTask(ReentrantBlockableEventLoop.java:24)")
				|| m.contains("at net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:1581)")
				|| m.contains("at net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:201)")
				|| m.contains("at net.minecraft.util.thread.BlockableEventLoop.pollTask(BlockableEventLoop.java:125)")
				|| m.contains("at net.minecraft.server.MinecraftServer.pollTaskInternal(MinecraftServer.java:1558)")
				|| m.contains("at net.minecraft.server.MinecraftServer.pollTask(MinecraftServer.java:1551)")
				|| m.contains("at net.minecraft.util.thread.BlockableEventLoop.runAllTasks(BlockableEventLoop.java:114)")
				|| m.contains("at net.minecraft.util.thread.BlockableEventLoop.managedBlock(BlockableEventLoop.java:135)")
				|| m.contains("at net.minecraft.server.MinecraftServer.managedBlock(MinecraftServer.java:1510)")
				|| m.contains("at net.minecraft.server.MinecraftServer.waitUntilNextTick(MinecraftServer.java:1517)")
				|| m.contains("at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1362)")
				|| */m.contains("at PocketGames-3.25.0.jar/com.live.bemmamin.pocketgames.UpdateChecker.isLatestVersion")) {
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
		//[Lokalny] [Uczestnik] [PROWADZĄCY] [Adminchat] [Gracz] [EVP] [SVP] [VIP] [A] [M] [GM] [S]
		if (m.contains("[Lokalny]") || m.contains("[Uczestnik]") || m.contains("[PROWADZĄCY]") || m.contains("[Adminchat]")
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
			if (ignoreNextCommandRun) {
				ignoreNextCommandRun = false;
				return Result.NEUTRAL;
			}
			//IGNORE
			if ((m.contains("MgrDesmond") || m.contains("JrRequeim") || m.contains("MsKarolsa")) && !m.contains("dbgxd")) {
				return Result.NEUTRAL;
			}
			//ignore private message /msg and /r (because is SPY) and other chats commands
			if (m.contains("/r") || m.contains("/msg") || m.contains("/adminczat")) {
				return Result.NEUTRAL;
			}
			//ignore commands like playerhidden
			if (m.contains("/playerhidden")) {
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
