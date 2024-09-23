package pl.eadventure.plugin.Utils;

import com.nickuc.login.api.nLoginAPI;
import dev.espi.protectionstones.PSRegion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.objectweb.asm.*;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.HomesInterface;
import pl.eadventure.plugin.PlayerData;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class CrackComplexTurret {

	public static boolean disabledBypass = false;

	public static void runBypassForCracked(Plugin plugin) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (disabledBypass) return;
				for (Player p : Bukkit.getOnlinePlayers()) {
					//print.debug(p.getName()+ " nLogin is logged: " + nLoginAPI.getApi().isAuthenticated(p.getName()));
					boolean hideForTurrets = false;
					if (PlaceholderAPI.setPlaceholders(p, "%supervanish_isvanished%").equalsIgnoreCase("Yes")) {//vanish detect
						hideForTurrets = true;
					} else if (p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) {//creative spec detect - oryginalne działanie isSurvival :D
						hideForTurrets = true;
					} else if (PlaceholderAPI.setPlaceholders(p, "%essentials_godmode%").equalsIgnoreCase("yes")) {//godmode detect
						hideForTurrets = true;
					} else if (!nLoginAPI.getApi().isAuthenticated(p.getName())) {//nlogin detect
						hideForTurrets = true;
					} else {//cuboid members auto detect
						PlayerData pd = PlayerData.get(p);
						HomesInterface hi = pd.homesInterface;
						if (hi == null) {
							hi = new HomesInterface();
							hi.loadFromPlayer(p);
							pd.homesInterface = hi;
						} else {
							hi.loadFromPlayer(p);
						}

						for (PSRegion cuboid : hi.getAllCuboids()) {
							if (wgAPI.isOnRegion(p, cuboid.getId())) {
								hideForTurrets = true;
								break;
							}
						}
						//if (wgAPI.isOnRegion(p, cuboid.getId()))
					}

					//print.debug(p.getName() + " hideForTurrets: " + hideForTurrets);
					boolean finalHideForTurrets = hideForTurrets;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (finalHideForTurrets) {
								disableTurrets(p);
							} else {
								enableTurrets(p);
							}
						}
					}.runTask(plugin);

				}
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L);
	}

	private static void enableTurrets(Player p) {
		if (!p.hasMetadata("eapenabledturrets")) {
			p.setMetadata("eapenabledturrets", new FixedMetadataValue(EternalAdventurePlugin.getInstance(), true));
			print.debug("enableTurrets: " + p.getName());
		}
	}

	public static void disableTurrets(Player p) {
		if (p.hasMetadata("eapenabledturrets")) {
			Iterator<MetadataValue> metadataValueIterator = p.getMetadata("eapenabledturrets").iterator();

			while (metadataValueIterator.hasNext()) {
				MetadataValue mdv = metadataValueIterator.next();
				Plugin ownPlugin = mdv.getOwningPlugin();
				print.debug(ownPlugin.toString());
				p.removeMetadata("eapenabledturrets", ownPlugin);
			}
			print.debug("disableTurrets: " + p.getName());
		}
	}

	//--------------------------------------------------------------------crack class ComplexTurret for bypass work fine
	public static void crack() throws IOException {
		// Ścieżka do pliku .class
		String classFilePath = "plugins/EternalAdventurePlugin/UtilsPlayers.class";
		FileInputStream fis = new FileInputStream(classFilePath);

		// Odczytanie istniejącej klasy
		ClassReader classReader = new ClassReader(fis);
		//-------------------------------------------------------------------------------------------------------------STEP TWO
		ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

		// Modyfikacja klasy
		ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				// Zamień nazwę metody `isSurvival` na `isPlayerInSurvival`
				if (name.equals("isSurvival")) {
					name = "hackdisabled";
				} else if (name.equals("isIgnoreTurret")) {
					name = "isSurvival";
				}

				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
		};


		//--------------------------------------------------------------------------------------STEP ONE
		/*ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

		// Modyfikacja klasy
		ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
			@Override
			public void visitEnd() {
				// Dodanie nowej metody
				MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "isIgnoreTurret", "(Lorg/bukkit/entity/Player;)Z", null, null);
				if (mv != null) {
					mv.visitCode();

					// Wczytanie argumentu (Player)
					mv.visitVarInsn(Opcodes.ALOAD, 0);

					// Wywołanie metody hasMetadata
					mv.visitLdcInsn("eapenabledturrets");
					mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/bukkit/entity/Player", "hasMetadata", "(Ljava/lang/String;)Z", true);

					// Zwrócenie wyniku metody hasMetadata
					mv.visitInsn(Opcodes.IRETURN);

					// Określenie maksymalnej liczby slotów stosu i lokalnych zmiennych
					mv.visitMaxs(2, 1);
					mv.visitEnd();
				}
				super.visitEnd();
			}
		};*/

		classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
		byte[] modifiedClass = classWriter.toByteArray();

		// Zapisz zmodyfikowaną klasę do pliku
		FileOutputStream fos = new FileOutputStream("plugins/EternalAdventurePlugin/cracked/UtilsPlayers.class");
		fos.write(modifiedClass);
		fos.close();
		fis.close();
	}
}
