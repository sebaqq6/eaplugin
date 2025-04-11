package pl.eadventure.plugin;

import me.NoChance.PvPManager.PvPlayer;
import me.NoChance.PvPManager.Tasks.NewbieTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.API.GlowAPI;
import pl.eadventure.plugin.API.PvpManagerAPI;
import pl.eadventure.plugin.Utils.print;

import java.lang.reflect.Field;

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

	}


	public static void testPlayer(Player player) {
		//glowTest(player);
		pvpManagerTest(player);
	}

	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------
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
		Entity specTarget = Bukkit.getPlayer("JrRequeim");
		if (specTarget instanceof Player playerTarget) {
			player.sendMessage("Target name: " + playerTarget.getName());
			Location playerLocation = player.getLocation();
			Location targetLocation = playerTarget.getLocation();
			//player.setSpectatorTarget(playerTarget);
			if (playerLocation.getWorld() != targetLocation.getWorld() || targetLocation.distance(playerLocation) > 10) {
				targetLocation.setY(targetLocation.getY() + 256);
				player.teleport(targetLocation);
				//player.setGameMode(GameMode.SPECTATOR);
				//player.setSpectatorTarget(playerTarget);
				player.setInvisible(true);
				Bukkit.getScheduler().runTaskLater(EternalAdventurePlugin.getInstance(), () -> {
					player.setInvisible(false);
					player.setGameMode(GameMode.SPECTATOR);
					player.setSpectatorTarget(playerTarget);
				}, 20L);
			}
		}
	}
}
