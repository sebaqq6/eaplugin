package pl.eadventure.plugin;

import dev.espi.protectionstones.PSPlayer;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HomesInterface {
	//---------------------------------------------------variables
	private static HomeInterfaceListener listener;
	private static Plugin staticPlugin;
	PSPlayer psPlayer;
	PSRegion actualCuboidSet = null;
	List<World> allWorlds;
	List<PSRegion> ownerCuboids = new ArrayList<>();
	List<PSRegion> memberCuboids = new ArrayList<>();
	List<PSRegion> allCuboids = new ArrayList<>();
	ArrayList<String> allCuboidsNames = new ArrayList<>();//owner cuboids and member
	ItemStack hRedHouse;
	ItemStack hGreenHouse;
	ItemStack hPurpleHouse;
	ItemStack homePane;
	ItemStack homePaneManage;
	ItemStack backgroundPane;
	ItemStack hBlackX;
	ItemStack hRedArrowLeft;
	ItemStack hRedMinus;
	ItemStack hGreenPlus;
	ItemStack hBorder;
	ItemStack hTeleport;
	MagicGUI mainMenuGUI;
	MagicGUI menageCuboidGUI;
	int cuboidsOwnerSize = 0;
	int cuboidsMemberSize = 0;
	int cuboidsLimitSize = 0;
	boolean typingCuboidName = false;
	boolean typingAddMemberCuboid = false;
	//-------------------------------------------------------------------use onEnable plugin
	public static void tryLoad(Plugin plugin) {
			if (listener == null) {
				staticPlugin = plugin;
				listener = new HomeInterfaceListener();
				Bukkit.getPluginManager().registerEvents(listener, plugin);
			}
	}
	//------------------------------------------------------------------use onDisable plugin
	public static void tryUnload() {
		if(listener != null) {
			HandlerList.unregisterAll(listener);
			staticPlugin = null;
		}
	}


	//------------------------------------------------------------init HouseInterface object
	public HomesInterface() {
		allWorlds = Bukkit.getWorlds();
		hRedHouse = gVar.customItems.get("hRedHouse");
		hGreenHouse = gVar.customItems.get("hGreenHouse");
		hPurpleHouse = gVar.customItems.get("hPurpleHouse");
		hRedArrowLeft = gVar.customItems.get("hRedArrowLeft");
		hBlackX = gVar.customItems.get("hBlackX");
		hRedMinus = gVar.customItems.get("hRedMinus");
		hGreenPlus = gVar.customItems.get("hGreenPlus");
		hBorder = gVar.customItems.get("hBorder");
		hTeleport = gVar.customItems.get("hTeleport");
		homePane = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
		homePaneManage = Utils.itemWithDisplayName(new ItemStack(Material.LIME_STAINED_GLASS_PANE), " ", null);
		backgroundPane = Utils.itemWithDisplayName(new ItemStack(Material.WHITE_STAINED_GLASS_PANE), " ", null);
	}
	//----------------------------------------------------------load data from player to HouseInterface
	public void loadFromUUID(UUID uuid) {
		this.psPlayer = PSPlayer.fromUUID(uuid);
		cuboidsLimitSize = psPlayer.getGlobalRegionLimits();
		load();
	}

	public void loadFromPlayer(Player player) {
		this.psPlayer = PSPlayer.fromPlayer(player);
		if (player.isOp()) {
			cuboidsLimitSize = -1;
		} else {
			cuboidsLimitSize = psPlayer.getGlobalRegionLimits();
		}
		load();
	}

	private void load() {
		ownerCuboids.clear();
		memberCuboids.clear();
		allCuboids.clear();
		for (World w : allWorlds) {
			ownerCuboids.addAll(psPlayer.getPSRegions(w, false));
			memberCuboids.addAll(psPlayer.getPSRegions(w, true));
		}
		//copy getPSRegions to all cuboids before isolate
		allCuboids.addAll(memberCuboids);
		//create all cuboids names list
		allCuboidsNames.clear();
		for(PSRegion cuboid : allCuboids) {
			String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
			allCuboidsNames.add(cuboidName);
		}
		//isolate member cuboids from owner
		Iterator<PSRegion> memberIterator = memberCuboids.iterator();
		while (memberIterator.hasNext()) {
			PSRegion mCuboid = memberIterator.next();
			//print.debug("dzialka: "+mCuboid);
			for (PSRegion pCuboid : ownerCuboids) {
				if (mCuboid.equals(pCuboid)) {
					//print.debug("Usuwana dzialka: "+mCuboid);
					memberIterator.remove();
					break;
				}
			}
		}
		cuboidsOwnerSize = ownerCuboids.size();
		cuboidsMemberSize = memberCuboids.size();
	}
	//---------------------------------------------------------------------get cuboid list
	public ArrayList<String> getAllCuboidsList() {
		return allCuboidsNames;
	}
	public List<PSRegion> getAllCuboids() {
		return allCuboids;
	}
	public List<PSRegion> getOwnerCuboids() {
		return ownerCuboids;
	}
	public List<PSRegion> getMemberCuboids() {
		return memberCuboids;
	}
	//---------------------------------------------------------------------------print debug data
	public void printDebugData() {
		print.debug("Dane gracza: " + psPlayer.getName());
		//only owner
		print.debug("[Owner] Ilość działek: " + ownerCuboids.size());
		print.debug("[Owner] Lista działek (nazwa - nazwa wg - świat- typ):");
		for (PSRegion cubOwner : ownerCuboids) {
			print.debug(cubOwner.getName() + " - " + cubOwner.getId() + " - " + cubOwner.getWorld().getName() + " - "
			+ cubOwner.getTypeOptions().alias);
		}
		// be member
		print.debug("[Member] Ilość działek: " + memberCuboids.size());
		print.debug("[Member] Lista działek (nazwa - nazwa wg - świat - typ):");
		for (PSRegion cubMember : memberCuboids) {
			print.debug(cubMember.getName() + " - " + cubMember.getId() + " - " + cubMember.getWorld().getName() + " - " +
			cubMember.getTypeOptions().alias);
		}
	}
	//---------------------------------------------------------------------------show first menu (all houses)
	public void renderMainMenuGUI(Player p) {
		int renderedInactiveCuboids = 0;
		mainMenuGUI = MagicGUI.create(Utils.color(String.format("&5&lDziałki %d/%d", cuboidsOwnerSize, cuboidsLimitSize)), 54);
		mainMenuGUI.setAutoRemove(true);
		for (int x = 0; x < 54; x++) {
			switch (x) {
				case 4, 12, 14, 20, 24, 33, 34, 28, 29, 42, 51, 38, 47, 48, 49, 50 -> {//home edge
					mainMenuGUI.setItem(x, homePane);
				}
				case 21, 22, 23, 40 -> {//houses
					if (x == 40) {
						String houseTitle = Utils.color(String.format("&r&5&lDziałki członkowskie"));
						//prepare description
						ArrayList<String> description = new ArrayList<>();
						String strFormat = String.format("&r&7Ilość działek: &3%d", memberCuboids.size());
						description.add(Utils.color(strFormat));
						description.add(" ");
						description.add(Utils.color("&r&7Kliknij &a&lLPM&7, aby wybrać działkę"));
						description.add(Utils.color("&r&7do której chcesz się teleportować"));
						description.add(Utils.color("&r&7będąc jej członkiem."));
						ItemStack houseItem = Utils.itemWithDisplayName(hPurpleHouse, houseTitle, description);
						mainMenuGUI.setItem(x, houseItem, ((player, gui, slot, type) -> {
							if (type == ClickType.LEFT) {
								renderMemberCuboids(player, 1);
								player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							}
						}));
					} else {
						if (renderedInactiveCuboids <= cuboidsLimitSize || cuboidsLimitSize == -1) {

							String houseTitle = Utils.color(String.format("&r&8&l-brak-"));
							//prepare description
							ArrayList<String> description = new ArrayList<>();
							description.add(Utils.color("&r&7Nieużywany slot działki."));
							description.add(" ");
							description.add(Utils.color("&r&7Po postawieniu bloku działki"));
							description.add(Utils.color("&r&7pojawi się ona tutaj automatycznie."));
							ItemStack houseItem = Utils.itemWithDisplayName(hRedHouse, houseTitle, description);
							if (cuboidsLimitSize == 2 && renderedInactiveCuboids == 1) {//limit 2 cuboids
								mainMenuGUI.setItem(x + 1, houseItem);
								mainMenuGUI.setItem(x, backgroundPane);
							} else if(cuboidsLimitSize == 1 && x == 21) {//limit one cuboids
								mainMenuGUI.setItem(x, backgroundPane);
							} else {//other (3 cuboids)
								mainMenuGUI.setItem(x, houseItem);
							}
							renderedInactiveCuboids++;
						} else {
							mainMenuGUI.setItem(x, backgroundPane);
						}

						/*String houseTitle = Utils.color(String.format("&r&8&l-brak-"));
						//prepare description
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color("&r&7Nieużywany slot działki."));
						description.add(" ");
						description.add(Utils.color("&r&7Po postawieniu bloku działki"));
						description.add(Utils.color("&r&7pojawi się ona tutaj automatycznie."));
						ItemStack houseItem = Utils.itemWithDisplayName(hRedHouse, houseTitle, description);
						mainMenuGUI.setItem(x, houseItem);*/
					}
				}
				case 45 -> {
					ItemStack exitButton = Utils.itemWithDisplayName(hBlackX, Utils.color("&r&7&lZamknij"), null);
					mainMenuGUI.setItem(x, exitButton, ((player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						mainMenuGUI.close(player);
						PlayerData.get(player).homesInterface = null;
					}));
				}
				default -> {//background
					mainMenuGUI.setItem(x, backgroundPane);
				}
			}
		}
		int cubOwnerRegionCounter = 0;
		for (PSRegion cubOwner : ownerCuboids) {
			//prepare title
			String cuboidName = cubOwner.getName() == null ? cubOwner.getId() : cubOwner.getName();
			String cuboidTitle = Utils.color(String.format("&r&5&l%s", cuboidName));
			//prepare description
			ArrayList<String> description = new ArrayList<>(getCuboidDescription(p, cubOwner));
			description.add(" ");
			description.add(Utils.color("&r&a&lLPM &7- aby się teleportować."));
			description.add(Utils.color("&r&a&lPPM &7- aby zarządzać."));
			//create final item
			ItemStack houseItem = Utils.itemWithDisplayName(hGreenHouse, cuboidTitle, description);

			if (cuboidsLimitSize == 2) {
				switch (cubOwnerRegionCounter) {
					case 0 -> mainMenuGUI.setItem(21, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
					case 1 -> mainMenuGUI.setItem(23, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
					case 2 -> mainMenuGUI.setItem(22, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
				}
			} else {
				switch (cubOwnerRegionCounter) {
					case 0 -> mainMenuGUI.setItem(22, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
					case 1 -> mainMenuGUI.setItem(21, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
					case 2 -> mainMenuGUI.setItem(23, houseItem, ((player, gui, slot, type) -> {
						clickOwnerCuboid(player, cubOwner, type);
					}));
				}
			}
			cubOwnerRegionCounter++;
		}
		mainMenuGUI.open(p);
	}
	//-----------------------------------------------------------------------generate description cuboid
	public ArrayList<String> getCuboidDescription(Player p, PSRegion cuboid) {
		ArrayList<String> description = new ArrayList<>();
		description.add(Utils.color(String.format("&r&7Świat: &3%s", Utils.translateWorldName(cuboid.getWorld()))));
		int[] pos = {cuboid.getProtectBlock().getX(), cuboid.getProtectBlock().getY(), cuboid.getProtectBlock().getZ()};
		description.add(Utils.color(String.format("&r&7Lokalizacja: &3%d, %d, %d", pos[0], pos[1], pos[2])));
		description.add(Utils.color(String.format("&r&7Ilość członków: &3%d", cuboid.getMembers().size())));
		description.add(Utils.color(String.format("&r&7Typ: &3%s", getCuboidSize(cuboid))));
		description.add(Utils.color(String.format("&r&7Blok działki: %s", cuboid.isHidden() ? "&8UKRYTY" : "&aWIDOCZNY")));
		if (wgAPI.isOnRegion(p, cuboid.getId())) {
			description.add(" ");
			description.add(Utils.color("&2&lTutaj jesteś."));
		}
		return description;
	}
	//-----------------------------------------------------------------------interact with any cuboid
	public void clickOwnerCuboid(Player p, PSRegion cuboid, ClickType clickType) {
		switch (clickType) {
			case LEFT -> {
				p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
				String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
				//p.performCommand("ps home " + cuboidName);
				teleportSafeToCuboid(p, cuboid);
				String msg = String.format("&aTeleportacja do działki &7%s&a...", cuboidName);
				PlayerUtils.sendColorMessage(p, msg);
				//mainMenuGUI.open(p);
			}
			case RIGHT -> {
				p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
				//mainMenuGUI.close(p);
				renderMenageCuboidGUI(p, cuboid);
			}
		}
	}
	//-----------------------------------------------------------------------menage selected cuboid
	public void renderMenageCuboidGUI(Player p, PSRegion cuboid) {
		String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
		actualCuboidSet = cuboid;
		String guiTitle = Utils.color(String.format("&5&l%s", cuboidName));
		menageCuboidGUI = MagicGUI.create(guiTitle, 54);
		menageCuboidGUI.setAutoRemove(true);
		for (int x = 0; x < 54; x++) {
			switch (x) {
				case 4, 12, 14, 20, 24, 33, 34, 28, 29, 42, 51, 38, 47, 48, 49, 50 -> {//home edge
					menageCuboidGUI.setItem(x, homePaneManage);
				}
				case 13, 21, 23, 31, 39, 41 -> {//menu
					if(x == 13)//sethome
					{
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color("&r&7Użyj &5&l/dzialka-teleport&7 - znajdując się"));
						description.add(Utils.color("&r&7na działce, aby ustawić teleport na tej działce."));
						if (!wgAPI.isOnRegion(p, cuboid.getId())) {
							description.add(" ");
							description.add(Utils.color("&r&cNie znajdujesz się na tej działce."));
						} else {
							description.add(" ");
							description.add(Utils.color("&r&a&lSHIFT+PPM &7- aby ustawić teleport."));
						}
						ItemStack button = Utils.itemWithDisplayName(hTeleport, Utils.color("&r&2&lUstaw teleport"), description);
						menageCuboidGUI.setItem(x, button, ((player, gui, slot, type) -> {
							if (type == ClickType.SHIFT_RIGHT) {
								player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
								player.performCommand("ps sethome");
								menageCuboidGUI.close(player);
							}
						}));
					}
					else if (x == 21) {//add member
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color(String.format("&r&7Ilość członków: &3%d", cuboid.getMembers().size())));
						description.add(" ");
						description.add(Utils.color("&r&a&lKliknij &7- aby dodać gracza do działki."));
						description.add(" ");
						description.add(Utils.color("&r&7Użyj &5&l/dodaj [nazwa gracza]&7 - znajdując się"));
						description.add(Utils.color("&r&7na działce, aby dodać gracza."));
						if (!wgAPI.isOnRegion(p, cuboid.getId())) {
							description.add(" ");
							description.add(Utils.color("&r&cNie znajdujesz się na tej działce."));
						}
						ItemStack button = Utils.itemWithDisplayName(hGreenPlus, Utils.color("&r&2&lDodaj członka"), description);
						menageCuboidGUI.setItem(x, button, ((player, gui, slot, type) -> {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							sendTypingAddMemberMessage(player);
							startTypingAddMember(player);
							menageCuboidGUI.close(player);
						}));
					} else if(x == 23) {//remove member
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color(String.format("&r&7Ilość członków: &3%d", cuboid.getMembers().size())));
						description.add(" ");
						description.add(Utils.color("&r&a&lKliknij &7- aby usunąć gracza z działki."));
						description.add(" ");
						description.add(Utils.color("&r&7Użyj &5&l/usun [nazwa gracza]&7 - znajdując się"));
						description.add(Utils.color("&r&7na działce, aby usunąć gracza."));
						if (!wgAPI.isOnRegion(p, cuboid.getId())) {
							description.add(" ");
							description.add(Utils.color("&r&cNie znajdujesz się na tej działce."));
						}
						ItemStack button = Utils.itemWithDisplayName(hRedMinus, Utils.color("&r&2&lUsuń członka"), description);
						menageCuboidGUI.setItem(x, button, ((player, gui, slot, type) -> {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							renderMenageMembersGUI(player, 1);
						}));
					} else if(x == 31) {//hide unhide block
						menageCuboidGUI.setItem(x, getBlockByStatus(cuboid), ((player, gui, slot, type) -> {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							cuboid.toggleHide();
							menageCuboidGUI.setUpdateItem(slot, getBlockByStatus(cuboid));
							//menageCuboidGUI.update();
						}));
					} else if (x == 39) {//change name cuboid
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color(String.format("&r&7Nazwa działki: &3%s", cuboidName)));
						description.add(" ");
						description.add(Utils.color("&r&a&lKliknij &7- aby zmienić nazwę działki."));
						description.add(" ");
						description.add(Utils.color("&r&7Użyj &5&l/dzialka-nazwa [nazwa działki]&7 -"));
						description.add(Utils.color("&r&7znajdując się na działce, aby zmienić"));
						description.add(Utils.color("&r&7nazwę działki."));
						if (!wgAPI.isOnRegion(p, cuboid.getId())) {
							description.add(" ");
							description.add(Utils.color("&r&cNie znajdujesz się na tej działce."));
						}
						ItemStack button = Utils.itemWithDisplayName(new ItemStack(Material.NAME_TAG), Utils.color("&r&2&lZmień nazwę"), description);
						menageCuboidGUI.setItem(x, button, ((player, gui, slot, type) -> {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							sendTypingCuboidNameMessage(player);
							startTypingCuboidName(player);
							menageCuboidGUI.close(player);
						}));
					} else if (x == 41) {//show border
						ArrayList<String> description = new ArrayList<>();
						description.add(Utils.color("&r&7Użyj &5&l/granica&7 - znajdując się"));
						description.add(Utils.color("&r&7na działce, aby zobaczyć granice działki."));
						if (!wgAPI.isOnRegion(p, cuboid.getId())) {
							description.add(" ");
							description.add(Utils.color("&r&cNie znajdujesz się na tej działce."));
						} else {
							description.add(" ");
							description.add(Utils.color("&r&a&lKliknij &7- aby pokazać granicę."));
						}
						ItemStack button = Utils.itemWithDisplayName(hBorder, Utils.color("&r&2&lPokaż granicę"), description);
						menageCuboidGUI.setItem(x, button, ((player, gui, slot, type) -> {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							player.performCommand("ps view");
							//wgAPI.glowWorldGuardEdge(player, cuboid.getId());
							menageCuboidGUI.close(player);
						}));
					}
				}
				case 45 -> {
					ItemStack exitButton = Utils.itemWithDisplayName(hRedArrowLeft, Utils.color("&r&7&lWróć"), null);
					menageCuboidGUI.setItem(x, exitButton, ((player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						renderMainMenuGUI(player);
						actualCuboidSet = null;
					}));
				}
				default -> {//background
					menageCuboidGUI.setItem(x, backgroundPane);
				}
			}
		}
		menageCuboidGUI.open(p);
	}
	//------------------------------------------------------------------------------------menage members cuboid GUI
	public record MemberEntry(UUID uuid, String playerName, ItemStack head, PSRegion cuboid) { }

	public void renderMenageMembersGUI(Player p, int page) {
		renderMenageMembersGUI(p, page, null);
	}

	private void renderMenageMembersGUI(Player p, int page, List<MemberEntry> list) {
		PSRegion cuboid = actualCuboidSet;
		//generate list
		if(list == null) {
			List<MemberEntry> preparedList = new ArrayList<>();
			for (UUID memberUUID : cuboid.getMembers()) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
				if(offlinePlayer.hasPlayedBefore()) {
					String playerName = offlinePlayer.getName();
					String playerNameDisplay = Utils.color(String.format("&5&l%s", playerName));
					ArrayList<String> description = new ArrayList<>();
					description.add(" ");
					description.add(Utils.color("&r&a&lSHIFT+PPM &7- aby usunąć gracza z działki."));
					ItemStack playerHead = Utils.getPlayerHead(playerName, playerNameDisplay, description);
					MemberEntry me = new MemberEntry(memberUUID, playerName, playerHead, cuboid);
					preparedList.add(me);
				}
			}
			renderMenageMembersGUI(p, 1, preparedList);
			return;
		}
		//init pages
		int itemsPerPage = 45;
		int startIndex = (page-1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, list.size());
		int totalPages = (int) Math.ceil((double) list.size() / itemsPerPage);

		String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
		String guiTitle = Utils.color(String.format("&5&lCzłonkowie %d/%d (%d)", page, Math.max(totalPages, 1), list.size()));
		MagicGUI menageMembersGUI = MagicGUI.create(guiTitle, 54);
		menageMembersGUI.setAutoRemove(true);
		//generate list
		for (int i = startIndex; i < endIndex; i++) {
			MemberEntry me = list.get(i);
			menageMembersGUI.addItem(me.head(), (player, gui, slot, type) -> {
				if (type == ClickType.SHIFT_RIGHT) {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					list.remove(me);
					cuboid.removeMember(me.uuid());
					menageMembersGUI.setItem(slot, new ItemStack(Material.AIR));
					String msg = String.format("&a%s&7 został/a usunięty/a z działki %s.", me.playerName(), cuboidName);
					player.sendMessage(Utils.color(msg));
				}
			});
		}
		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if(x == 45) {
				ArrayList<String> description = new ArrayList<>(getCuboidDescription(p, cuboid));
				ItemStack infoItem = Utils.itemWithDisplayName(gVar.customItems.get("hInfo"), Utils.color("&r&e&lInformacja"), description);
				menageMembersGUI.setItem(x, infoItem);//Info
			} else if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					menageMembersGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int previousPage = page - 1;
						renderMenageMembersGUI(player, previousPage, list);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					menageMembersGUI.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lWróć"), null);
				menageMembersGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					renderMenageCuboidGUI(player, cuboid);
				});
			} else if (x == 51) {
				if (endIndex < list.size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					menageMembersGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						renderMenageMembersGUI(player, nextPage, list);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					menageMembersGUI.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				menageMembersGUI.setItem(x, navButton);
			}
		}
		//open GUI
		menageMembersGUI.open(p);
	}
	//------------------------------------------------------------------------------------get cuboid block by status
	private ItemStack getBlockByStatus(PSRegion cuboid) {
		if(cuboid.isHidden()) {
			ArrayList<String> description = new ArrayList<>();
			description.add(Utils.color("&r&7Blok działki jest ukryty."));
			description.add(" ");
			description.add(Utils.color("&r&a&lKliknij &7- aby pokazać blok."));
			ItemStack cuboidBlock = Utils.itemWithDisplayName(new ItemStack(Material.GLASS), Utils.color("&r&7&lBlok ukryty"), description);
			return cuboidBlock;
		} else {

			ArrayList<String> description = new ArrayList<>();
			description.add(Utils.color("&r&7Blok działki jest widoczny."));
			description.add(" ");
			description.add(Utils.color("&r&a&lKliknij &7- aby ukryć blok."));
			ItemStack cuboidBlock = Utils.itemWithDisplayName(new ItemStack(cuboid.getProtectBlock().getType()), Utils.color("&r&a&lBlok widoczny"), description);
			return cuboidBlock;
		}
	}
	//-----------------------------------------------------------------------------------typing cuboid name
	public void startTypingCuboidName(Player player) {
		if(typingCuboidName) return;
		new BukkitRunnable() {
			int step = 0;
			@Override
			public void run() {
				String msg;
				switch (step) {
					case 0 -> {
						msg = "&7Wpisz nową nazwę działki na czacie.";
					}
					case 1 -> {
						msg = "&7Wpisz &a&lnone&7, aby usunąć nazwę działki.";
					}
					default -> {
						msg = "&7Wpisz &a&lanuluj&7, aby anulować.";
						step = -1;
					}
				}

				if(typingCuboidName && player.isOnline()) {
					player.sendTitle("", Utils.color(msg), 10, 80, 10);
					step++;
				} else {
					cancel();
					typingCuboidName = false;
					print.debug("typingCuboidName: cancelTimer");
				}
			}
		}.runTaskTimer(staticPlugin, 1, 100);
		typingCuboidName = true;
	}

	public static void sendTypingCuboidNameMessage(Player player)
	{
		String msg = "&2&m-------------------------------------------------\n" +
				"&7&lWpisz nową nazwę działki na czacie.\n" +
				"&7&lWpisz &anone&7&l, aby usunąć nazwę działki.\n" +
				"&7&lWpisz &aanuluj&7&l, aby anulować.\n" +
				"&2&m-------------------------------------------------";
		player.sendMessage(Utils.color(msg));
	}

	public synchronized void stopTypingCuboidName(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				typingCuboidName = false;
				player.resetTitle();
				renderMenageCuboidGUI(player, actualCuboidSet);
			}
		}.runTask(staticPlugin);
	}
	//-------------------------------------------------------------------------------------typing add member to cuboid
	public void startTypingAddMember(Player player) {
		if(typingAddMemberCuboid) return;
		new BukkitRunnable() {
			int step = 0;
			@Override
			public void run() {
				String msg;
				switch (step) {
					case 0 -> {
						msg = "&7Wpisz nazwę gracza na czacie.";
					}
					case 1 -> {
						msg = "&7Aby dodać go do działki.";
					}
					default -> {
						msg = "&7Wpisz &a&lanuluj&7, aby anulować.";
						step = -1;
					}
				}

				if(typingAddMemberCuboid && player.isOnline()) {
					player.sendTitle("", Utils.color(msg), 10, 80, 10);
					step++;
				} else {
					cancel();
					typingAddMemberCuboid = false;
					print.debug("startTypingAddMember: cancelTimer");
				}
			}
		}.runTaskTimer(staticPlugin, 1, 100);
		typingAddMemberCuboid = true;
	}

	public static void sendTypingAddMemberMessage(Player player)
	{
		String msg = "&2&m-------------------------------------------------\n" +
				"&7&lWpisz nazwę gracza na czacie.\n" +
				"&7&lWpisz &aanuluj&7&l, aby anulować.\n" +
				"&2&m-------------------------------------------------";
		player.sendMessage(Utils.color(msg));
	}

	public synchronized void stopTypingAddMember(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				typingAddMemberCuboid = false;
				player.resetTitle();
				//renderMenageCuboidGUI(player, actualCuboidSet);
				renderMenageMembersGUI(player, 1);
			}
		}.runTask(staticPlugin);

	}
	//----------------------------------------------------------------------------------------member teleport gui
	public void renderMemberCuboids(Player p, int page) {
		int itemsPerPage = 45;
		int startIndex = (page-1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, memberCuboids.size());
		int totalPages = (int) Math.ceil((double) memberCuboids.size() / itemsPerPage);
		String guiTitle = Utils.color(String.format("&5&lCzłonkostwo %d/%d (%d)", page, Math.max(totalPages, 1), memberCuboids.size()));
		MagicGUI memberCuboidsGUI = MagicGUI.create(guiTitle, 54);
		memberCuboidsGUI.setAutoRemove(true);
		for (int i = startIndex; i < endIndex; i++) {
			PSRegion cuboid = memberCuboids.get(i);
			String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
			String ownerName = Bukkit.getOfflinePlayer(cuboid.getOwners().get(0)).getName();
			int[] pos = {cuboid.getProtectBlock().getX(), cuboid.getProtectBlock().getY(), cuboid.getProtectBlock().getZ()};
			ArrayList<String> description = new ArrayList<>();
			description.add(Utils.color(String.format("&r&7Właściciel: &3%s", ownerName)));
			description.add(Utils.color(String.format("&r&7Świat: &3%s", Utils.translateWorldName(cuboid.getWorld()))));
			description.add(Utils.color(String.format("&r&7Lokalizacja: &3%d, %d, %d", pos[0], pos[1], pos[2])));
			description.add(Utils.color(String.format("&r&7Typ: &3%s", getCuboidSize(cuboid))));
			description.add(" ");
			description.add(Utils.color("&r&a&lLPM &7- aby się teleportować."));
			description.add(Utils.color("&r&a&lSHIFT+PPM &7- aby przestać być członkiem."));

			if (wgAPI.isOnRegion(p, cuboid.getId())) {
				description.add(" ");
				description.add(Utils.color("&2&lTutaj jesteś."));
			}

			ItemStack cuboidIcon = new ItemStack(cuboid.getProtectBlock().getRelative(BlockFace.DOWN).getType());
			if(!cuboidIcon.getType().isSolid()) {//if air change to block type
				cuboidIcon = new ItemStack(Material.valueOf(cuboid.getType()));
			}

			String formatCuboidName = Utils.color(String.format("&5&l%s", cuboidName));

			ItemStack finalCuboidIcon = Utils.itemWithDisplayName(cuboidIcon, formatCuboidName, description);
			memberCuboidsGUI.addItem(finalCuboidIcon, ((player, gui, slot, type) -> {
				if (type == ClickType.LEFT) {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					if(cuboid.isMember(player.getUniqueId())) {
						teleportSafeToCuboid(player, cuboid);
						String msg = String.format("&aTeleportacja do działki &7%s&a...", cuboidName);
						PlayerUtils.sendColorMessage(player, msg);
					} else {
						PlayerUtils.sendColorMessage(player, "&7Nie jesteś członkiem tej działki.");
						memberCuboidsGUI.close(player);
					}
				} else if(type == ClickType.SHIFT_RIGHT) {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					if(cuboid.isMember(player.getUniqueId())) {
						memberCuboidsGUI.setItem(slot, new ItemStack(Material.AIR));
						memberCuboids.remove(cuboid);
						allCuboids.remove(cuboid);
						cuboid.removeMember(player.getUniqueId());
						String msg = String.format("&7Przestałeś/aś być członkiem działki: &a%s&7.", cuboidName);
						PlayerUtils.sendColorMessage(player, msg);
					}
				}
			}));
		}
		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					memberCuboidsGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int previousPage = page - 1;
						renderMemberCuboids(player, previousPage);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					memberCuboidsGUI.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lWróć"), null);
				memberCuboidsGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					renderMainMenuGUI(player);
				});
			} else if (x == 51) {
				if (endIndex < memberCuboids.size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					memberCuboidsGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						renderMemberCuboids(player, nextPage);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					memberCuboidsGUI.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				memberCuboidsGUI.setItem(x, navButton);
			}
		}
		memberCuboidsGUI.open(p);
	}
	//----------------------------------------------------------------------teleport to cuboid by name
	public boolean teleportToCuboidByName(Player p, String cubName) {
		for (PSRegion cuboid : allCuboids) {
			String cuboidName = cuboid.getName() == null ? cuboid.getId() : cuboid.getName();
			if(!cuboidName.equalsIgnoreCase(cubName)) continue;
			if(!cuboid.getMembers().contains(p.getUniqueId()) &&
					!cuboid.getOwners().contains(p.getUniqueId())) continue;
			teleportSafeToCuboid(p, cuboid);
			String msg = String.format("&aTeleportacja do działki &7%s&a...", cuboidName);
			PlayerUtils.sendColorMessage(p, msg);
			return true;
		}
		return false;
	}
	//------------------------------------------------------------------------better safe teleport to cuboid
	public void teleportSafeToCuboid(Player p, PSRegion cuboid) {
		Location posHome = cuboid.getHome();
		Material firstBlock = posHome.getBlock().getType();
		Location posSecondBlock = posHome.clone();
		posSecondBlock.setY(posSecondBlock.getY()+1);
		Material secondBlock = posSecondBlock.getBlock().getType();
		if(firstBlock.isAir() && secondBlock.isAir()) { //teleport is safe
			p.teleport(posHome);
		} else {
			Location safePos = Utils.findFreeAirBlockAbove(posHome);
			if (safePos == null) {//if we cant find safe pos, go to unsafe pos xD
				p.teleport(posHome);
			} else {
				p.teleport(safePos);
			}
		}
	}
	//------------------------------------------------------------------------get cuboid size
	public String getCuboidSize(PSRegion cuboid) {
		String result = "0x0";
		String alias = cuboid.getTypeOptions().alias;
		if(alias != null) {
			result = String.format("%sx%s", alias, alias);
		}
		return result;
	}
	//----------------------------------------------------------------------------------------------Listeners
	public static class HomeInterfaceListener implements Listener {
		@EventHandler
		public void onPlayerChat(AsyncPlayerChatEvent e) {
			Player player = e.getPlayer();
			PlayerData pd = PlayerData.get(player);
			HomesInterface hi = pd.homesInterface;
			if(hi != null) {
				PSRegion region = hi.actualCuboidSet;
				//-----------------------------------------typing cuboid name
				String message = e.getMessage();
				if(hi.typingCuboidName) {
					e.setCancelled(true);
					if(region == null) {
						player.sendMessage("Wystąpił nieoczekiwany błąd. Zgłoś to! (ID: rnull:0)");
						hi.stopTypingCuboidName(player);
						return;
					}
					String[] newRegionName = message.split(" ");
					if(newRegionName[0].length() > 24) {
						player.sendMessage(Utils.color("&7Nazwa domku jest za długa (maks 32 znaki)."));
						return;
					}
					if (newRegionName[0].equalsIgnoreCase("none")) {
						region.setName(null);
						String msg = String.format("&7Usunięto nazwę działki &f%s&7.", region.getId());
						player.sendMessage(Utils.color(msg));
						hi.stopTypingCuboidName(player);
						return;
					}
					if (newRegionName[0].equalsIgnoreCase("anuluj")) {
						String msg = String.format("&7Anulowano zmianę nazwy działki &f%s&7.", region.getId());
						player.sendMessage(Utils.color(msg));
						hi.stopTypingCuboidName(player);
						return;
					}
					if (!ProtectionStones.getInstance().getConfigOptions().allowDuplicateRegionNames
							&& ProtectionStones.isPSNameAlreadyUsed(newRegionName[0])) {
						String msg = String.format("&cNazwa działki &a%s&c jest już przez kogoś użyta.", newRegionName[0]);
						player.sendMessage(Utils.color(msg));
						return;
					}
					region.setName(newRegionName[0]);
					String msg = String.format("&7Ustawiono nazwę działki &f%s &7na &a%s&7.", region.getId(), region.getName());
					player.sendMessage(Utils.color(msg));
					hi.stopTypingCuboidName(player);
				}
				//---------------------------------------------------typing add member
				if(hi.typingAddMemberCuboid) {
					e.setCancelled(true);
					if(region == null) {
						player.sendMessage("Wystąpił nieoczekiwany błąd. Zgłoś to! (ID: rnull:1)");
						hi.stopTypingAddMember(player);
						return;
					}
					String[] memberName = message.split(" ");
					if (memberName[0].equalsIgnoreCase("anuluj")) {
						String msg = String.format("&7Anulowano dodawanie gracza do działki &f%s&7.", region.getId());
						player.sendMessage(Utils.color(msg));
						hi.stopTypingAddMember(player);
						return;
					}
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberName[0]);
					if (!offlinePlayer.hasPlayedBefore()) {
						String msg = "&7Taki gracz nie istnieje";
						player.sendMessage(Utils.color(msg));
						return;
					}
					if(region.isMember(offlinePlayer.getUniqueId())) {
						String msg = "&7Ten gracz jest już dodany do Twojej działki.";
						player.sendMessage(Utils.color(msg));
						return;
					}
					region.addMember(offlinePlayer.getUniqueId());
					String cuboidName = region.getName() == null ? region.getId() : region.getName();
					String msg = String.format("&7Gracz &f%s &7został dodany do działki &a%s&7.", memberName[0], cuboidName);
					player.sendMessage(Utils.color(msg));
					hi.stopTypingAddMember(player);
				}
			}
		}
		//---------------------------------------block commands while player typing value
		@EventHandler
		public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
			Player player = e.getPlayer();
			PlayerData pd = PlayerData.get(player);
			HomesInterface hi = pd.homesInterface;
			if (hi != null) {
				if (hi.typingCuboidName) {
					sendTypingCuboidNameMessage(player);
					e.setCancelled(true);
				} else if (hi.typingAddMemberCuboid) {
					sendTypingAddMemberMessage(player);
					e.setCancelled(true);
				}
			}
		}
	}


}
