package com.rz.dave;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.block.BrewingStand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

public final class GameListener implements Listener {
    private final DaveManager manager;

    public GameListener(DaveManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTriggerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }
        if (!message.startsWith("trigger rz.")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "【权限】该指令需要管理员权限才能使用");
            return;
        }
        event.setCancelled(true);
        manager.handleTriggerCommand(player, message);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDaveInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getRightClicked() instanceof org.bukkit.entity.Wolf wolf && manager.isPetWolf(wolf)) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            if (wolf.getPassengers().contains(player)) {
                wolf.removePassenger(player);
            } else {
                wolf.addPassenger(player);
            }
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager) || !manager.isDave(villager)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().openInventory(new ShopMainMenu(event.getPlayer()).getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnPointInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand stand && manager.isSummonPoint(stand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (manager.isWindMaceFallImmune(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopClick(InventoryClickEvent event) {
        if (manager.isMenuClock(event.getCurrentItem()) || manager.isMenuClock(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (manager.isSpectatorExitItem(event.getCurrentItem())
                && event.getWhoClicked() instanceof Player spectator) {
            event.setCancelled(true);
            spectator.closeInventory();
            manager.exitSpectate(spectator);
            return;
        }
        if (event.getInventory().getHolder() instanceof OpMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == OpMenu.GAME_CTRL_SLOT) {
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_GAME, manager).getInventory());
            } else if (slot == OpMenu.TEAM_SLOT) {
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_TEAM, manager).getInventory());
            } else if (slot == OpMenu.SPAWN_SLOT) {
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_SPAWN, manager).getInventory());
            } else if (slot == OpMenu.BOSS_SLOT) {
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_BOSS, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof OpCategoryMenu categoryMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == OpCategoryMenu.BACK_SLOT) {
                player.openInventory(new OpMenu().getInventory());
                return;
            }
            if (slot == OpCategoryMenu.BIND_SLOT && categoryMenu.category() == OpCategoryMenu.CATEGORY_TEAM) {
                player.openInventory(new OpTeamMenu(true).getInventory());
                return;
            }
            if (slot == OpCategoryMenu.KILL_SLOT && categoryMenu.category() == OpCategoryMenu.CATEGORY_TEAM) {
                player.openInventory(new OpTeamMenu(false).getInventory());
                return;
            }
            if (slot == OpCategoryMenu.PVP_SLOT && categoryMenu.category() == OpCategoryMenu.CATEGORY_GAME) {
                manager.togglePvp();
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_GAME, manager).getInventory());
                return;
            }
            if (slot == OpCategoryMenu.WAVE_SLOT && categoryMenu.category() == OpCategoryMenu.CATEGORY_GAME) {
                player.openInventory(new WaveMenu().getInventory());
                return;
            }
            String command = OpCategoryMenu.commandForSlot(categoryMenu.category(), slot);
            if (command != null) {
                player.closeInventory();
                if (command.startsWith("trigger ")) {
                    manager.handleTriggerCommand(player, command);
                } else {
                    player.performCommand(command);
                }
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof WaveMenu waveMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == WaveMenu.BACK_SLOT) {
                player.openInventory(new OpCategoryMenu(OpCategoryMenu.CATEGORY_GAME, manager).getInventory());
                return;
            }
            int wave = waveMenu.waveForSlot(slot);
            if (wave >= 1) {
                player.sendMessage(ChatColor.GOLD + manager.jumpToWave(wave));
                player.openInventory(new WaveMenu().getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof OpTeamMenu teamMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == OpTeamMenu.BACK_SLOT) {
                player.openInventory(new OpMenu().getInventory());
                return;
            }
            String team = teamMenu.teamForSlot(slot);
            if (team != null) {
                player.closeInventory();
                if (teamMenu.bindMode()) {
                    player.performCommand("davepve bind " + team + " 16");
                } else {
                    player.performCommand("davepve kill " + team);
                }
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof MainMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == MainMenu.PLAY_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
            } else if (slot == MainMenu.INFO_SLOT) {
                player.openInventory(new GameplayInfoMenu().getInventory());
            } else if (slot == MainMenu.COMMANDS_SLOT) {
                player.openInventory(new CommandsMenu(player).getInventory());
            } else if (slot == MainMenu.MANAGE_SLOT && player.isOp()) {
                player.openInventory(new OpMenu().getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof PlayerMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == PlayerMenu.TEAM_SLOT) {
                if (!manager.isReady(player)) {
                    player.sendMessage(ChatColor.YELLOW + "【选队】请先点击准备，再选择队伍");
                    return;
                }
                player.openInventory(new TeamSelectionMenu(manager).getInventory());
            } else if (slot == PlayerMenu.MODE_NORMAL_SLOT) {
                player.openInventory(new ModeNormalMenu(player, manager).getInventory());
            } else if (slot == PlayerMenu.MODE_DEATH_SLOT) {
                player.openInventory(new ModeDeathMenu(player, manager).getInventory());
            } else if (slot == PlayerMenu.MODE_PVZ_SLOT) {
                player.openInventory(new PvzModeMenu(player, manager).getInventory());
            } else if (slot == PlayerMenu.BACK_SLOT) {
                player.openInventory(new MainMenu(player, manager).getInventory());
            } else if (slot == PlayerMenu.SPECTATE_SLOT) {
                player.closeInventory();
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    manager.exitSpectate(player);
                } else {
                    manager.startSpectate(player);
                }
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof ModeNormalMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == ModeNormalMenu.READY_SLOT) {
                manager.voteMode(player, false);
                player.closeInventory();
                player.performCommand("davepve ready");
            } else if (slot == ModeNormalMenu.UNREADY_SLOT) {
                player.closeInventory();
                player.performCommand("davepve unready");
            } else if (slot == ModeNormalMenu.BACK_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof ModeDeathMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == ModeDeathMenu.READY_SLOT) {
                manager.voteMode(player, true);
                player.closeInventory();
                player.performCommand("davepve ready");
            } else if (slot == ModeDeathMenu.UNREADY_SLOT) {
                player.closeInventory();
                player.performCommand("davepve unready");
            } else if (slot == ModeDeathMenu.ECONOMY_LOW_SLOT) {
                manager.voteEconomy(player, 0);
                player.openInventory(new ModeDeathMenu(player, manager).getInventory());
            } else if (slot == ModeDeathMenu.ECONOMY_MID_SLOT) {
                manager.voteEconomy(player, 1);
                player.openInventory(new ModeDeathMenu(player, manager).getInventory());
            } else if (slot == ModeDeathMenu.ECONOMY_HIGH_SLOT) {
                manager.voteEconomy(player, 2);
                player.openInventory(new ModeDeathMenu(player, manager).getInventory());
            } else if (slot == ModeDeathMenu.BACK_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof PvzModeMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            PvzMode pvz = manager.pvzMode();
            if (pvz == null) {
                player.sendMessage(ChatColor.RED + "【PVZ】PVZ 模式未加载");
                player.closeInventory();
                return;
            }
            int slot = event.getRawSlot();
            if (slot == PvzModeMenu.READY_SLOT) {
                boolean changed = pvz.setReady(player, true);
                player.sendMessage(ChatColor.GREEN + "【PVZ】"
                        + (changed ? "你已准备 PVZ（当前 " + pvz.readyCount() + " 人准备）"
                        : "你已经在 PVZ 准备状态"));
                if (changed) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "【PVZ】" + player.getName()
                            + " 已准备 PVZ（当前 " + pvz.readyCount() + " 人准备）");
                }
                player.openInventory(new PvzModeMenu(player, manager).getInventory());
            } else if (slot == PvzModeMenu.UNREADY_SLOT) {
                boolean changed = pvz.setReady(player, false);
                player.sendMessage(ChatColor.GREEN + "【PVZ】"
                        + (changed ? "你已取消 PVZ 准备" : "你本来就没在 PVZ 准备状态"));
                player.openInventory(new PvzModeMenu(player, manager).getInventory());
            } else if (slot == PvzModeMenu.BACK_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof TeamSelectionMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == TeamSelectionMenu.BACK_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
                return;
            }
            String team = switch (slot) {
                case TeamSelectionMenu.RED_SLOT -> "red";
                case TeamSelectionMenu.BLUE_SLOT -> "blue";
                case TeamSelectionMenu.YELLOW_SLOT -> "yellow";
                case TeamSelectionMenu.GREEN_SLOT -> "green";
                case TeamSelectionMenu.RANDOM_SLOT -> null;
                default -> "INVALID";
            };
            if ("INVALID".equals(team)) {
                return;
            }
            manager.setTeamPreference(player, team);
            player.sendMessage(ChatColor.GREEN + "【选队】已选择" + (team == null ? "随机分配" : "【" + team + "队】"));
            player.openInventory(new PlayerMenu(player, manager).getInventory());
            return;
        }
        if (event.getInventory().getHolder() instanceof GameplayInfoMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (event.getRawSlot() == GameplayInfoMenu.BACK_SLOT) {
                player.openInventory(new MainMenu(player, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof CommandsMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (event.getRawSlot() == CommandsMenu.BACK_SLOT) {
                player.openInventory(new MainMenu(player, manager).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof PlayerSpectateMenu spectateMenu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == PlayerSpectateMenu.BACK_SLOT) {
                player.openInventory(new PlayerMenu(player, manager).getInventory());
                return;
            }
            Player target = spectateMenu.targetForSlot(slot);
            if (target != null) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setSpectatorTarget(target);
                player.closeInventory();
                player.sendMessage(ChatColor.LIGHT_PURPLE + "【旁观】正在旁观 " + target.getName());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopMainMenu menu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (menu.page() == ShopMainMenu.PAGE_PRODUCTS) {
                ShopCategory category = menu.categoryForSlot(slot);
                if (category != null) {
                    if ("upgrade".equals(category.key())) {
                        player.openInventory(new EquipmentMenu().getInventory());
                    } else {
                        player.openInventory(ShopPage.forCategory(category).getInventory());
                    }
                    return;
                }
                if (slot == ShopMainMenu.TEAM_CHEST_SLOT) {
                    String team = manager.getTeamName(player);
                    if (team == null) {
                        player.sendMessage(ChatColor.AQUA + "【箱子】你还没有队伍，请先 /davepve ready 后被分队");
                        return;
                    }
                    player.openInventory(manager.openTeamChest(team));
                    return;
                }
                if (slot == ShopMainMenu.ENDER_CHEST_SLOT) {
                    player.openInventory(player.getEnderChest());
                    return;
                }
                if (slot == ShopMainMenu.TRASH_SLOT) {
                    player.openInventory(new TrashHolder().getInventory());
                    return;
                }
                if (slot == ShopMainMenu.NAV_SLOT) {
                    player.openInventory(new ShopMainMenu(player, ShopMainMenu.PAGE_STAR).getInventory());
                }
                return;
            }
            if (menu.page() == ShopMainMenu.PAGE_STAR) {
                if (slot == ShopMainMenu.STAR_STRENGTH_SLOT) {
                    manager.buyTeamBuff(player, PotionEffectType.STRENGTH);
                } else if (slot == ShopMainMenu.STAR_RESISTANCE_SLOT) {
                    manager.buyTeamBuff(player, PotionEffectType.RESISTANCE);
                } else if (slot == ShopMainMenu.STAR_SLOW_SLOT) {
                    manager.buyDaveSlowAura(player);
                } else if (slot == ShopMainMenu.STAR_MOVEMENT_SLOT) {
                    manager.buyTeamMovement(player);
                } else if (slot == ShopMainMenu.STAR_HASTE_SLOT) {
                    manager.buyTeamHaste(player);
                } else if (slot == ShopMainMenu.STAR_DAVE_RESISTANCE_SLOT) {
                    manager.buyDaveBuff(player, PotionEffectType.RESISTANCE);
                } else if (slot == ShopMainMenu.STAR_DAVE_REGEN_SLOT) {
                    manager.buyDaveBuff(player, PotionEffectType.REGENERATION);
                } else if (slot == ShopMainMenu.NAV_SLOT) {
                    player.openInventory(new ShopMainMenu(player, ShopMainMenu.PAGE_PRODUCTS).getInventory());
                }
                return;
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopPage page) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == ShopPage.BACK_SLOT) {
                player.openInventory(new ShopMainMenu(player).getInventory());
                return;
            }
            if (slot >= 0 && slot < page.category().items().size()) {
                manager.purchase(player, page.category().items().get(slot));
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof EquipmentMenu menu) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            EquipmentCatalog.Kind kind = menu.kindForSlot(slot);
            if (kind != null) {
                player.openInventory(new EquipmentDetailPage(player, manager, kind).getInventory());
            } else if (slot == EquipmentMenu.BACK_SLOT) {
                player.openInventory(new ShopMainMenu(player, ShopMainMenu.PAGE_PRODUCTS).getInventory());
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof EquipmentDetailPage page) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == EquipmentDetailPage.BACK_SLOT) {
                player.openInventory(new EquipmentMenu().getInventory());
                return;
            }
            if (page.kind() == EquipmentCatalog.Kind.SHOOTER
                    && (slot == EquipmentDetailPage.SHOOTER_FIRE_SLOT
                    || slot == EquipmentDetailPage.SHOOTER_NORMAL_SLOT
                    || slot == EquipmentDetailPage.SHOOTER_ICE_SLOT
                    || slot == EquipmentDetailPage.SHOOTER_SNIPER_SLOT)) {
                int branchSlot = slot == EquipmentDetailPage.SHOOTER_FIRE_SLOT
                        ? EquipmentCatalog.SHOOTER_FIRE_SLOT
                        : slot == EquipmentDetailPage.SHOOTER_NORMAL_SLOT
                        ? EquipmentCatalog.SHOOTER_NORMAL_SLOT
                        : slot == EquipmentDetailPage.SHOOTER_ICE_SLOT
                        ? EquipmentCatalog.SHOOTER_ICE_SLOT
                        : EquipmentCatalog.SHOOTER_SNIPER_SLOT;
                manager.handleShooterUpgrade(player, branchSlot);
                player.openInventory(new EquipmentDetailPage(player, manager, page.kind()).getInventory());
                return;
            }
            if (slot == EquipmentDetailPage.MATERIAL_SLOT && page.kind().hasMaterialUpgrade()) {
                manager.handleEquipmentUpgradeClick(player, page.kind());
                player.openInventory(new EquipmentDetailPage(player, manager, page.kind()).getInventory());
                return;
            }
            if (slot == EquipmentDetailPage.DAMAGE_SLOT && page.kind() != EquipmentCatalog.Kind.SHOOTER) {
                manager.handleWeaponDamageClick(player, page.kind());
                player.openInventory(new EquipmentDetailPage(player, manager, page.kind()).getInventory());
                return;
            }
            EquipmentCatalog.EnchantEntry enchant = page.enchantForSlot(slot);
            if (enchant != null) {
                manager.handleEnchantClick(player, page.kind(), enchant);
                player.openInventory(new EquipmentDetailPage(player, manager, page.kind()).getInventory());
            }
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopDrag(InventoryDragEvent event) {
        if (manager.isMenuClock(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopMainMenu
                || event.getInventory().getHolder() instanceof ShopPage
                || event.getInventory().getHolder() instanceof MainMenu
                || event.getInventory().getHolder() instanceof TeamSelectionMenu
                || event.getInventory().getHolder() instanceof EquipmentMenu
                || event.getInventory().getHolder() instanceof EquipmentDetailPage
                || event.getInventory().getHolder() instanceof GameplayInfoMenu
                || event.getInventory().getHolder() instanceof CommandsMenu
                || event.getInventory().getHolder() instanceof PlayerMenu
                || event.getInventory().getHolder() instanceof ModeNormalMenu
                || event.getInventory().getHolder() instanceof ModeDeathMenu
                || event.getInventory().getHolder() instanceof PvzModeMenu
                || event.getInventory().getHolder() instanceof PlayerSpectateMenu
                || event.getInventory().getHolder() instanceof OpMenu
                || event.getInventory().getHolder() instanceof OpCategoryMenu
                || event.getInventory().getHolder() instanceof OpTeamMenu
                || event.getInventory().getHolder() instanceof WaveMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingFuel(BrewingStandFuelEvent event) {
        if (manager.isOwnedStand(event.getBlock().getLocation())) {
            event.setConsuming(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BrewingStand stand
                && manager.isOwnedStand(stand.getLocation())
                && event.getRawSlot() == 3) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TeamChestHolder holder) {
            manager.saveTeamChest(holder.team(), event.getInventory());
        } else if (event.getInventory().getHolder() instanceof TrashHolder) {
            event.getInventory().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (DaveManager.isOneShotAxe(first) || DaveManager.isOneShotAxe(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (DaveManager.isOneShotAxe(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (DaveManager.isOneShotAxe(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!manager.isPlaying(player)) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        manager.returnLoyaltyTridents(player);
        if (manager.isDeathMode() || manager.isBossWave()) {
            player.setGameMode(GameMode.SPECTATOR);
            manager.markWaitingRespawn(player);
            player.sendMessage(ChatColor.RED + (manager.isDeathMode()
                    ? "【死战模式】你已阵亡，将在休整期复活！"
                    : "【Boss波】你已阵亡，将在休整期复活！"));
            return;
        }
        manager.scheduleForceRespawn(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!manager.isPlaying(player)) {
            return;
        }
        org.bukkit.Location respawn = manager.daveRespawnLocation(player);
        if (respawn == null) {
            return;
        }
        event.setRespawnLocation(respawn);
        player.setGameMode(GameMode.SPECTATOR);
        if (manager.isDeathMode() || manager.isBossWave()) {
            player.sendMessage(ChatColor.RED + (manager.isDeathMode()
                    ? "【死战模式】你已阵亡，将在休整期复活！"
                    : "【Boss波】你已阵亡，将在休整期复活！"));
            return;
        }
        player.sendMessage(ChatColor.AQUA + "【复活】你将在 10 秒后复活");
        manager.scheduleReturnToAdventure(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (manager.isMenuClock(dropped)) {
            event.setCancelled(true);
            return;
        }
        EquipmentCatalog.Kind kind = manager.heldWeaponKind(dropped);
        if ((kind == EquipmentCatalog.Kind.TRIDENT
                || kind == EquipmentCatalog.Kind.BOW
                || kind == EquipmentCatalog.Kind.CROSSBOW
                || kind == EquipmentCatalog.Kind.SPEAR
                || kind == EquipmentCatalog.Kind.AXE
                || kind == EquipmentCatalog.Kind.MACE)
                && manager.tryWeaponSkill(event.getPlayer(), kind, false, dropped)) {
            event.setCancelled(true);
        } else if (kind == EquipmentCatalog.Kind.CACTUS_SHOOTER
                && manager.tryCactusShooterSkill(event.getPlayer(), dropped)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (manager.isCactusShooter(held) && manager.tryCactusShooterSkill(event.getPlayer(), held)) {
            event.setCancelled(true);
        } else if (manager.shooterBranch(held) == DaveManager.BRANCH_SNIPER
                && manager.shooterTier(held) >= 1
                && event.getPlayer().getItemInUse() != null) {
            event.setCancelled(true);
            manager.fireSniperShooter(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmSwing(org.bukkit.event.player.PlayerAnimationEvent event) {
        if (event.getAnimationType() != org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (manager.shooterBranch(item) == DaveManager.BRANCH_SNIPER
                && manager.shooterTier(item) >= 1) {
            manager.fireSniperShooter(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        GameMode newMode = event.getNewGameMode();
        if (newMode != GameMode.ADVENTURE && newMode != GameMode.SPECTATOR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "你只能使用冒险模式或旁观者模式");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (manager.shooterBranch(item) == DaveManager.BRANCH_SNIPER
                    && manager.shooterTier(item) >= 1) {
                event.setCancelled(true);
                manager.fireSniperShooter(player);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentCatalog.Kind heldKind = manager.heldWeaponKind(item);
        if (manager.shooterBranch(item) == DaveManager.BRANCH_SNIPER) {
            // 狙击豌豆：右键保留原版望远镜缩放，不做拦截
            return;
        } else if ((heldKind == EquipmentCatalog.Kind.SWORD || heldKind == EquipmentCatalog.Kind.AXE)
                && manager.tryWeaponSkill(player, heldKind, true)) {
            event.setCancelled(true);
        } else if (DaveManager.isWindMace(item)) {
            event.setCancelled(true);
            manager.fireWindCharge(player);
        } else if (manager.isCactusShooter(item)) {
            event.setCancelled(true);
            manager.fireCactusShooter(player);
        } else if (manager.isIceShroom(item)) {
            event.setCancelled(true);
            manager.useIceShroom(player);
        } else if (manager.isBigPuffShroom(item)) {
            event.setCancelled(true);
            manager.fireBigPuffShroom(player);
        } else if (manager.isSmallPuffShroom(item)) {
            event.setCancelled(true);
            manager.fireSmallPuffShroom(player);
        } else if (manager.isTimidShroom(item)) {
            event.setCancelled(true);
            manager.fireTimidShroom(player);
        } else if (manager.isNut(item)) {
            event.setCancelled(true);
            manager.useNut(player, false);
        } else if (manager.isBigNut(item)) {
            event.setCancelled(true);
            manager.useNut(player, true);
        } else if (manager.shooterTier(item) >= 0) {
            event.setCancelled(true);
            manager.fireShooter(player);
        } else if (manager.isCherryBomb(item)) {
            event.setCancelled(true);
            manager.fireBomb(player, false);
        } else if (manager.isDestroyShroom(item)) {
            event.setCancelled(true);
            manager.fireBomb(player, true);
        } else if (manager.isMenuClock(item)) {
            event.setCancelled(true);
            player.openInventory(new MainMenu(player, manager).getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Villager villager && manager.isDave(villager)) {
            manager.handleDaveSpawn(villager);
        } else if (event.getEntity() instanceof Mob mob && manager.isMonster(mob)) {
            manager.retarget(mob);
        } else if (event.getEntity() instanceof WitherSkull skull
                && skull.getShooter() instanceof Wither wither
                && manager.isMonster(wither)
                && wither.getScoreboardTags().contains("big_boss")) {
            try {
                java.lang.reflect.Method scaleMethod = skull.getClass().getMethod("setSize", float.class);
                scaleMethod.invoke(skull, 5.0f);
            } catch (Exception ignored) {
                // 兼容：无法缩放时保持原尺寸
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!manager.isDeathMode()) {
            return;
        }
        Item item = event.getEntity();
        if (manager.isCurrencyItem(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDaveDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Wolf wolf) {
            manager.handleWolfDeath(wolf);
        }
        if (event.getEntity() instanceof Villager villager && manager.isDave(villager)) {
            manager.handleDaveDeath(villager);
            return;
        }
        if (event.getEntity() instanceof Slime slime && manager.isMonster(slime)) {
            event.getDrops().clear();
            manager.handleSlimeDeath(slime);
            return;
        }
        if (event.getEntity() instanceof Zombie zombie && zombie.getScoreboardTags().contains("dancing_zombie")) {
            manager.releaseBackupDancers(zombie);
        }
        if (event.getEntity() instanceof Mob mob && manager.isMonster(mob)) {
            if (manager.isDeathMode()) {
                event.getDrops().clear();
            }
            if (mob.getScoreboardTags().contains("random_zombie")) {
                event.getDrops().clear();
                manager.handleBlindBoxDeath(mob);
                return;
            }
            Player killer = manager.resolveMonsterKiller(mob);
            manager.addPlayerKill(killer);
            manager.collectKillDrops(event.getDrops(), killer);
            manager.removeVanillaDrops(event.getDrops());
            if (mob.getScoreboardTags().contains("mini_boss")) {
                manager.grantMiniBossStar(mob, killer);
            } else if (mob.getScoreboardTags().contains("dancing_zombie")) {
                manager.grantDancingZombieDrops(mob, killer);
            } else {
                manager.grantMonsterDrops(mob, killer);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (isBigBossExplosion(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (isBigBoss(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean isBigBoss(Entity entity) {
        return entity instanceof Wither wither
                && wither.getScoreboardTags().contains("big_boss");
    }

    private boolean isBigBossExplosion(Entity entity) {
        if (isBigBoss(entity)) {
            return true;
        }
        if (entity instanceof WitherSkull skull && skull.getShooter() instanceof Wither wither) {
            return wither.getScoreboardTags().contains("big_boss");
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (event.getEntity() instanceof Villager villager && manager.isDave(villager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SUSPICIOUS_STEW) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemName() || !"谜之炖菜".equals(meta.getItemName())) {
            return;
        }
        manager.applyStewEffect(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof WitherSkull skull
                && skull.getShooter() instanceof Wither wither
                && manager.isMonster(wither)
                && wither.getScoreboardTags().contains("big_boss")) {
            Location loc = skull.getLocation();
            loc.getWorld().createExplosion(loc, 0.0f, false, false);
            double radius = 5.0;
            double centerDamage = 4.0;
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living) || living.isDead()) {
                    continue;
                }
                double dist = living.getLocation().distance(loc);
                if (dist > radius) {
                    continue;
                }
                double dmg = (1.0 - dist / radius) * centerDamage;
                if (dmg > 0) {
                    living.damage(dmg);
                }
            }
        }
        if (event.getEntity() instanceof WindCharge windCharge
                && windCharge.getPersistentDataContainer().has(DaveManager.windMaceKey(), PersistentDataType.BYTE)
                && windCharge.getShooter() instanceof Player shooter) {
            manager.applyWindChargeLaunch(shooter, windCharge.getLocation());
        }
        if (event.getEntity() instanceof Snowball sniper
                && sniper.getPersistentDataContainer().has(DaveManager.sniperBulletKey(), PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof Mob mob && manager.isMonster(mob)) {
                double damage = sniper.getPersistentDataContainer()
                        .getOrDefault(DaveManager.shooterDamageKey(), PersistentDataType.DOUBLE, 50.0);
                if (sniper.getShooter() instanceof Player shooter) {
                    manager.recordPlayerDamage(mob, shooter, damage);
                    mob.damage(damage, shooter);
                    shooter.playSound(mob.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
                } else {
                    mob.damage(damage);
                }
                if (sniper.getPersistentDataContainer()
                        .has(DaveManager.sniperExplodeKey(), PersistentDataType.BYTE)) {
                    Location loc = mob.getLocation();
                    loc.getWorld().createExplosion(loc, 0.0f, false, false);
                    double radius = 2.0;
                    double center = 65.0;
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                        if (!(entity instanceof Mob m2) || !manager.isMonster(m2)) {
                            continue;
                        }
                        double dist = m2.getLocation().distance(loc);
                        double dmg = (1.0 - dist / radius) * center;
                        if (dmg <= 0) {
                            continue;
                        }
                        if (sniper.getShooter() instanceof Player shooter2) {
                            manager.recordPlayerDamage(m2, shooter2, dmg);
                            m2.damage(dmg, shooter2);
                        } else {
                            m2.damage(dmg);
                        }
                    }
                }
            }
            sniper.remove();
            return;
        }
        if (event.getEntity() instanceof Snowball cactus
                && cactus.getPersistentDataContainer().has(DaveManager.cactusShooterKey(), PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof Mob mob && manager.isMonster(mob)) {
                if (!manager.hasCactusHit(cactus, mob)) {
                    manager.markCactusHit(cactus, mob);
                    double damage = cactus.getPersistentDataContainer()
                            .getOrDefault(DaveManager.cactusDamageKey(), PersistentDataType.DOUBLE, 7.0);
                    if (cactus.getShooter() instanceof Player shooter) {
                        manager.recordPlayerDamage(mob, shooter, damage);
                        mob.damage(damage, shooter);
                    } else {
                        mob.damage(damage);
                    }
                }
            }
            if (event.getHitBlock() != null) {
                manager.removeCactusTracker(cactus);
                cactus.remove();
            }
            return;
        }
        if (event.getEntity() instanceof Snowball puff
                && puff.getPersistentDataContainer().has(DaveManager.smallPuffBulletKey(), PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof Mob mob && manager.isMonster(mob)) {
                double damage = puff.getPersistentDataContainer()
                        .getOrDefault(DaveManager.shooterDamageKey(), PersistentDataType.DOUBLE, 5.0);
                if (puff.getShooter() instanceof Player shooter) {
                    mob.damage(damage, shooter);
                } else {
                    mob.damage(damage);
                }
                int witherAmp = puff.getPersistentDataContainer()
                        .getOrDefault(DaveManager.smallPuffWitherAmpKey(), PersistentDataType.INTEGER, 2);
                mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WITHER, 100, witherAmp, false, true, true));
            }
            puff.remove();
            return;
        }
        if (event.getEntity() instanceof Snowball snowball
                && snowball.getPersistentDataContainer().has(DaveManager.shooterProjectileKey(), PersistentDataType.BYTE)
                && event.getHitEntity() instanceof Mob mob && manager.isMonster(mob)) {
            if (snowball.getShooter() instanceof Player shooter) {
                double damage = snowball.getPersistentDataContainer()
                        .getOrDefault(DaveManager.shooterDamageKey(), PersistentDataType.DOUBLE, 5.0);
                mob.setNoDamageTicks(0);
                mob.damage(damage, shooter);
                if (snowball.getPersistentDataContainer().has(DaveManager.shooterSlowKey(), PersistentDataType.BYTE)) {
                    mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 1, false, true, true));
                }
                if (snowball.getPersistentDataContainer().has(DaveManager.shooterFireKey(), PersistentDataType.BYTE)) {
                    mob.setFireTicks(20);
                }
                if (snowball.getPersistentDataContainer().has(DaveManager.shooterDragonKey(), PersistentDataType.BYTE)) {
                    mob.setFireTicks(20);
                }
                if (snowball.getPersistentDataContainer().has(DaveManager.shooterIceBlockKey(), PersistentDataType.BYTE)) {
                    mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 4, false, true, true));
                }
            }
        }
        if (event.getHitEntity() != null
                && event.getEntity() instanceof Arrow arrow
                && arrow.getPersistentDataContainer().has(DaveManager.shopBowArrowKey(), PersistentDataType.BYTE)) {
            arrow.setDamage(arrow.getDamage() * 2.0);
        }
        if (event.getHitEntity() != null
                && event.getEntity() instanceof Arrow arrow
                && arrow.getPersistentDataContainer().has(DaveManager.rangedBonusKey(), PersistentDataType.DOUBLE)) {
            double bonus = arrow.getPersistentDataContainer()
                    .get(DaveManager.rangedBonusKey(), PersistentDataType.DOUBLE);
            arrow.setDamage(arrow.getDamage() + bonus);
        }
        if (event.getEntity() instanceof Trident trident
                && trident.getShooter() instanceof Player player) {
            ItemStack tridentItem = trident.getItem();
            if (tridentItem != null && tridentItem.getItemMeta() != null) {
                int bonus = tridentItem.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(DaveManager.weaponDamageKey(), PersistentDataType.INTEGER, 0);
                if (bonus > 0 && event.getHitEntity() != null) {
                    trident.setDamage(trident.getDamage() + bonus);
                }
            }
        }
        if (event.getEntity() instanceof Arrow arrow
                && arrow.getPersistentDataContainer().has(DaveManager.explosiveArrowKey(), PersistentDataType.BYTE)) {
            manager.handleExplosiveArrowHit(arrow);
        }
        if (!(event.getEntity() instanceof Trident trident)
                || !(trident.getShooter() instanceof Player player)) {
            return;
        }
        ItemStack item = trident.getItem();
        if (item == null || !item.containsEnchantment(Enchantment.CHANNELING)) {
            return;
        }
        manager.channelingStrike(player, trident.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow != null && bow.getItemMeta() != null) {
            int count = bow.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(DaveManager.weaponDamageKey(), PersistentDataType.INTEGER, 0);
            EquipmentCatalog.Kind kind = bow.getType() == Material.CROSSBOW
                    ? EquipmentCatalog.Kind.CROSSBOW : EquipmentCatalog.Kind.BOW;
            double bonus = DaveManager.rangedBonusForLevel(kind, count);
            if (bonus > 0) {
                arrow.getPersistentDataContainer().set(
                        DaveManager.rangedBonusKey(), PersistentDataType.DOUBLE, bonus);
            }
        }
        if (manager.isShopBow(event.getBow())) {
            arrow.getPersistentDataContainer().set(DaveManager.shopBowArrowKey(), PersistentDataType.BYTE, (byte) 1);
        }
        if (manager.isBowExplosiveActive(player)) {
            arrow.getPersistentDataContainer().set(DaveManager.explosiveArrowKey(), PersistentDataType.BYTE, (byte) 1);
            arrow.getPersistentDataContainer().set(DaveManager.skillArrowKey(), PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        manager.handleDamage(event);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.addScoreboardTag("player");
        manager.markUnready(player);
        if (manager.restoreOnRejoin(player)) {
            return;
        }
        if (!player.isOp()) {
            player.setGameMode(GameMode.ADVENTURE);
            manager.teleportToLobby(player);
        }
        manager.applyLobbyItems(player);
        manager.refreshPlayerListName(player);
        if (!manager.isPlaying(player)) {
            manager.sendWelcome(player);
        }
        manager.refreshBossBars();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.markUnready(event.getPlayer());
        manager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileCollide(com.destroystokyo.paper.event.entity.ProjectileCollideEvent event) {
        org.bukkit.entity.Projectile projectile = event.getEntity();
        if (projectile.getPersistentDataContainer().has(DaveManager.shooterProjectileKey(), PersistentDataType.BYTE)
                && (event.getCollidedWith() instanceof Player
                || manager.isDave(event.getCollidedWith())
                || (event.getCollidedWith() instanceof org.bukkit.entity.Wolf wolf && manager.isPetWolf(wolf)))) {
            event.setCancelled(true);
        } else if (projectile.getPersistentDataContainer()
                .has(DaveManager.smallPuffBulletKey(), PersistentDataType.BYTE)
                && (event.getCollidedWith() instanceof Player
                || manager.isDave(event.getCollidedWith())
                || (event.getCollidedWith() instanceof org.bukkit.entity.Wolf wolf && manager.isPetWolf(wolf)))) {
            event.setCancelled(true);
        } else if (projectile.getPersistentDataContainer()
                .has(DaveManager.skillArrowKey(), PersistentDataType.BYTE)
                && (event.getCollidedWith() instanceof Player
                || manager.isDave(event.getCollidedWith())
                || (event.getCollidedWith() instanceof org.bukkit.entity.Wolf wolf && manager.isPetWolf(wolf)))) {
            event.setCancelled(true);
        } else if (projectile.getPersistentDataContainer()
                .has(DaveManager.cactusShooterKey(), PersistentDataType.BYTE)
                && !(event.getCollidedWith() instanceof org.bukkit.block.Block)) {
            event.setCancelled(true);
        } else if (projectile.getPersistentDataContainer()
                .has(DaveManager.axeBulletKey(), PersistentDataType.BYTE)
                && !(event.getCollidedWith() instanceof org.bukkit.block.Block)) {
            event.setCancelled(true);
        } else if (projectile.getPersistentDataContainer()
                .has(DaveManager.sniperBulletKey(), PersistentDataType.BYTE)
                && (event.getCollidedWith() instanceof Player
                || manager.isDave(event.getCollidedWith())
                || (event.getCollidedWith() instanceof org.bukkit.entity.Wolf wolf && manager.isPetWolf(wolf)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        manager.refreshBossBars();
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }
}
