package dev.armordurability;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;

import java.util.HashMap;
import java.util.Map;

public class ArmorDurabilityPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<Material, Integer> customDurability = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDurabilityFromConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("armordurability").setExecutor(this);
        getLogger().info("ArmorDurability enabled! Loaded " + customDurability.size() + " entries.");
    }

    private void loadDurabilityFromConfig() {
        customDurability.clear();
        var section = getConfig().getConfigurationSection("durability");
        if (section == null) {
            getLogger().warning("Section 'durability' not found in config.yml!");
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase());
            if (material == null) {
                getLogger().warning("Unknown material in config: " + key);
                continue;
            }
            int value = section.getInt(key);
            customDurability.put(material, value);
        }
        getLogger().info("Loaded durability for: " + customDurability.keySet());
    }

    public boolean applyDurability(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Integer customDur = customDurability.get(item.getType());
        if (customDur == null) return false;
        item.setData(DataComponentTypes.MAX_DAMAGE, customDur);
        item.setData(DataComponentTypes.DAMAGE, 0);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("armordurability.reload")) {
                sender.sendMessage("§cНет прав!");
                return true;
            }
            reloadConfig();
            loadDurabilityFromConfig();
            sender.sendMessage("§aArmorDurability: конфиг перезагружен! Загружено " + customDurability.size() + " значений.");
            return true;
        }
        sender.sendMessage("§eArmorDurability v1.0 | /armordurability reload");
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(CraftItemEvent event) {
        applyDurability(event.getCurrentItem());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        applyDurability(event.getInventory().getResult());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(PlayerPickupItemEvent event) {
        applyDurability(event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            applyDurability(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        for (ItemStack drop : event.getDrops()) {
            applyDurability(drop);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        applyDurability(event.getCurrentItem());
        applyDurability(event.getCursor());
    }
}
