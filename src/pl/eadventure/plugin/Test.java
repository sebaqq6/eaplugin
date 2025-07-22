package pl.eadventure.plugin;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.object.GStopReason;
import me.NoChance.PvPManager.PvPlayer;
import me.frep.vulcan.api.VulcanAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.API.PvpManagerAPI;
import pl.eadventure.plugin.FunEvents.Event.StarcieEternal;
import pl.eadventure.plugin.FunEvents.FunEvent;
import pl.eadventure.plugin.Utils.print;

import java.lang.reflect.Field;
import java.sql.Timestamp;

/*
DEVELOPER PLAYGROUND CLASS
*/
public class Test {
	public static Plugin plugin = EternalAdventurePlugin.getInstance();

	public static void run(CommandSender sender) {
		if (sender instanceof Player player) {
			testPlayer(player);
		} else {
			test(sender);
		}

	}

	public static void test(CommandSender sender) {
		VulcanApiTest();
	}


	public static void testPlayer(Player player) {
		glowTest(player);
		//pvpManagerTest(player);
		//messageTest(player);
		//gateTest();
		//gSitApiTest(player);
		//specTest(player);
	}

	public static Player testDoll() {
		Player player = Bukkit.getPlayer("Test_doll2");
		return player;
	}

	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	public static void gSitApiTest(Player player) {
		player.sendMessage("gSitApiTest");
		GSitAPI.stopPlayerSit(testDoll(), GStopReason.PLUGIN);
	}

	//------------------------------------------------------------------------------------------------------------------
	public static void gateTest() {
		FunEvent funEvent = gVar.funEventsManager.getEvent("starcieeternal");
		if (funEvent instanceof StarcieEternal starcieEternal) {
			starcieEternal.setOpenBlueGate(true);
			starcieEternal.setOpenRedGate(true);
			new BukkitRunnable() {
				@Override
				public void run() {
					starcieEternal.setOpenBlueGate(false);
					starcieEternal.setOpenRedGate(false);
				}
			}.runTaskLater(plugin, 20 * 5);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	public static void VulcanApiTest() {
		print.info("VulcanApiTest...");
		print.info("Vulcan version detected: " + VulcanAPI.Factory.getApi().getVulcanVersion());
		print.info("VulcanApiTest... END");
	}

	//------------------------------------------------------------------------------------------------------------------
	public static void messageTest(Player player) {
		String message = String.format("&8&l[&4&lALERT&8&l]");
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		message = String.format("&8&l[&4&lAC&8&l]");
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	//------------------------------------------------------------------------------------------------------------------
	static long pvpNewbieRemainTime = -1;

	public static void pvpManagerTest(Player player) {
		PvPlayer pvplayer = PvPlayer.get(player);
		for (Field field : pvplayer.getClass().getDeclaredFields()) {
			System.out.println("Field: " + field.getName() + " (Type: " + field.getType().getSimpleName() + ")");
		}
		player.sendMessage("PvpManagerAPI.getTimeLeftNewbie(player): " + PvpManagerAPI.getTimeLeftNewbie(player));
		player.sendMessage("PvpManagerAPI.isNewbie(player): " + PvpManagerAPI.isNewbie(player));
		player.sendMessage("PvpManagerAPI.takeNewbie(player)......");
		PvpManagerAPI.takeNewbie(player);
		player.sendMessage("PvpManagerAPI.getTimeLeftNewbie(player): " + PvpManagerAPI.getTimeLeftNewbie(player));
		player.sendMessage("PvpManagerAPI.isNewbie(player): " + PvpManagerAPI.isNewbie(player));
/*
		pvpNewbieRemainTime = PvpManagerAPI.disableNewbie(player);
		player.sendMessage("Twój czas newbie: " + pvpNewbieRemainTime);
		PvpManagerAPI.giveNewbie(player, pvpNewbieRemainTime);
		player.sendMessage("Twój NOWY czas newbie: " + PvpManagerAPI.getTimeLeftNewbie(player));

 */
	}

	//------------------------------------------------------------------------------------------------------------------
	public static void glowTest(Player player) {
		for (Player receiver : Bukkit.getOnlinePlayers()) {
			GlowAPI.glowPlayer(player, receiver, ChatColor.BLUE, 60);
		}

		Entity entity = player.getTargetEntity(10);
		if (entity != null) {
			if (entity instanceof Player player1) {
				GlowAPI.glowPlayer(player1, player, ChatColor.YELLOW, 60);
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	public static void specTest(Player player) {
		//Entity specTarget = player.getSpectatorTarget();
		Entity specTarget = Bukkit.getPlayer("Test_doll3");
		if (specTarget instanceof Player playerTarget) {
			player.sendMessage("Target name: " + playerTarget.getName());
			Location playerLocation = player.getLocation();
			Location targetLocation = playerTarget.getLocation();
			Timestamp playerLastTeleport = PlayerData.get(player).lastTeleport;
			Timestamp targetLastTeleport = PlayerData.get(playerTarget).lastTeleport;
			//player.setSpectatorTarget(playerTarget);
			print.info("pd.before(targetPd) " + playerLastTeleport.before(targetLastTeleport));//BUG
			if (playerLocation.getWorld() != targetLocation.getWorld() || playerLastTeleport.before(targetLastTeleport)) {
				print.error("FIX SPEC???");
				targetLocation.setY(targetLocation.getY() + 256);
				player.teleport(targetLocation);
				//player.setGameMode(GameMode.SPECTATOR);
				//player.setSpectatorTarget(playerTarget);
				player.setInvisible(true);
				player.setGameMode(GameMode.SURVIVAL);
				Bukkit.getScheduler().runTaskLater(EternalAdventurePlugin.getInstance(), () -> {
					player.setInvisible(false);
					player.setGameMode(GameMode.SPECTATOR);
					player.setSpectatorTarget(playerTarget);
				}, 20L);
			}
		}
	}
}
