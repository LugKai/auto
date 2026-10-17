package me.plugin.autoreplant.listeners;

import me.plugin.autoreplant.AutoReplant;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CropHarvestListener implements Listener {

    private final AutoReplant plugin;

    private static final Set<Material> HOES = Set.of(
        Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
        Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
    );

    private static final Map<Material, Material> CROP_SEED_MAP = new HashMap<>();

    static {
        CROP_SEED_MAP.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_SEED_MAP.put(Material.CARROTS, Material.CARROT);
        CROP_SEED_MAP.put(Material.POTATOES, Material.POTATO);
        CROP_SEED_MAP.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_SEED_MAP.put(Material.NETHER_WART, Material.NETHER_WART);
        CROP_SEED_MAP.put(Material.COCOA, Material.COCOA_BEANS);
        CROP_SEED_MAP.put(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES);
        tryAdd("PITCHER_CROP", "PITCHER_POD");
        tryAdd("TORCHFLOWER_CROP", "TORCHFLOWER_SEEDS");
    }

    private static void tryAdd(String crop, String seed) {
        try { CROP_SEED_MAP.put(Material.valueOf(crop), Material.valueOf(seed)); }
        catch (IllegalArgumentException ignored) {}
    }

    public CropHarvestListener(AutoReplant plugin) {
        this.plugin = plugin;
    }

    // ป้องกันทำลายพืชที่ยังไม่โตด้วย Hoe
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProtect(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("settings.require-hoe", true)) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!isHolding(player)) return;
        if (!CROP_SEED_MAP.containsKey(block.getType())) return;
        if (!isFullyGrown(block)) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("settings.show-messages", true)) {
                player.sendMessage(color(plugin.getConfig().getString(
                    "messages.not-grown", "&eพืชยังไม่โตเต็มที่!")));
            }
        }
    }

    // Auto replant เมื่อเก็บพืชโตเต็มที่
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // ต้องถือ Hoe
        if (plugin.getConfig().getBoolean("settings.require-hoe", true) && !isHolding(player)) return;

        if (!player.hasPermission("autoreplant.use")) return;
        if (!plugin.getPlayerDataManager().isEnabled(player.getUniqueId())) return;
        if (!CROP_SEED_MAP.containsKey(blockType)) return;
        if (!isFullyGrown(block)) return;
        if (!plugin.getConfig().getBoolean("crops." + blockType.name(), true)) return;

        Material seedMaterial = CROP_SEED_MAP.get(blockType);
        boolean requireSeeds = plugin.getConfig().getBoolean("settings.require-seeds", true);

        if (requireSeeds && !hasSeed(player, seedMaterial)) {
            if (plugin.getConfig().getBoolean("settings.show-messages", true)) {
                String msg = plugin.getConfig().getString("messages.no-seeds", "&cไม่มีเมล็ด {crop}!")
                        .replace("{crop}", getCropName(blockType));
                player.sendMessage(color(msg));
            }
            return;
        }

        Location loc = block.getLocation().clone();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block target = loc.getBlock();
            if (!canReplant(target, blockType)) return;

            if (requireSeeds) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                    removeSeed(player, seedMaterial), 1L);
            }

            replantCrop(target, blockType);

            if (plugin.getConfig().getBoolean("settings.show-messages", true)) {
                String msg = plugin.getConfig().getString("messages.replanted", "&aปลูกคืน {crop} แล้ว!")
                        .replace("{crop}", getCropName(blockType));
                player.sendMessage(color(msg));
            }

            loc.getWorld().playSound(loc, Sound.ITEM_CROP_PLANT, 0.8f, 1.0f);
            loc.getWorld().spawnParticle(Particle.COMPOSTER,
                    loc.clone().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, 0);
        }, 1L);
    }

    private boolean isHolding(Player player) {
        return HOES.contains(player.getInventory().getItemInMainHand().getType());
    }

    private boolean isFullyGrown(Block block) {
        BlockData data = block.getBlockData();
        if (block.getType() == Material.SWEET_BERRY_BUSH)
            return data instanceof Ageable a && a.getAge() >= 3;
        if (block.getType() == Material.COCOA)
            return data instanceof Ageable a && a.getAge() >= 2;
        return data instanceof Ageable a && a.getAge() == a.getMaximumAge();
    }

    private boolean canReplant(Block block, Material cropType) {
        if (block.getType() != Material.AIR) return false;
        Material below = block.getRelative(0, -1, 0).getType();
        return switch (cropType) {
            case NETHER_WART -> below == Material.SOUL_SAND;
            case COCOA -> below.name().contains("JUNGLE") && below.name().contains("LOG");
            case SWEET_BERRY_BUSH -> below == Material.GRASS_BLOCK || below == Material.DIRT
                    || below == Material.PODZOL || below == Material.COARSE_DIRT
                    || below == Material.FARMLAND;
            default -> below == Material.FARMLAND;
        };
    }

    private void replantCrop(Block block, Material cropType) {
        block.setType(cropType);
        BlockData data = block.getBlockData();
        if (data instanceof Ageable a) { a.setAge(0); block.setBlockData(a); }
    }

    private boolean hasSeed(Player player, Material seed) {
        for (ItemStack i : player.getInventory().getContents())
            if (i != null && i.getType() == seed && i.getAmount() > 0) return true;
        return false;
    }

    private void removeSeed(Player player, Material seed) {
        ItemStack[] inv = player.getInventory().getContents();
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null && inv[i].getType() == seed) {
                if (inv[i].getAmount() > 1) inv[i].setAmount(inv[i].getAmount() - 1);
                else player.getInventory().setItem(i, null);
                return;
            }
        }
    }

    private String getCropName(Material m) {
        return switch (m) {
            case WHEAT -> "ข้าวสาลี";
            case CARROTS -> "แครอท";
            case POTATOES -> "มันฝรั่ง";
            case BEETROOTS -> "บีทรูท";
            case NETHER_WART -> "วอร์ตนรก";
            case COCOA -> "โกโก้";
            case SWEET_BERRY_BUSH -> "เบอร์รี่หวาน";
            default -> m.name().replace("_", " ").toLowerCase();
        };
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
