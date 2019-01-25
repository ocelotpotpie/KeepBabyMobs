/*
 * KeepBabyMobs, a Bukkit plugin that provides
 * a game mechanic for preserving baby mobs
 * 
 * Copyright 2014 tompreuss <tompreuss@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.tompreuss.keepbabymobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;

public final class KeepBabyMobs extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    /**
     * {@see org.bukkit.plugin.java.JavaPlugin#onEnable}.
     */
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
    }

    // ------------------------------------------------------------------------
    /**
     * Loads the list of breeding items from config.
     */
    private void loadConfig() {
        saveDefaultConfig();
        ConfigurationSection config = getConfig().getConfigurationSection("breeding-items");
        if (config != null) {
            for (String key : config.getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key.toUpperCase());
                    HashSet<Material> food = new HashSet<>();
                    config.getStringList(key).stream()
                                             .map(Material::getMaterial)
                                             .forEach(food::add);
                    EDIBLES.put(entityType, food);
                } catch (Exception e) {
                    getLogger().info("Invalid EntityType " + key + " specified.");
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If player names a baby mob with a name tag, age lock the baby.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntityEvent(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        if (player == null || !(entity instanceof Ageable)) {
            return;
        }
        
        ItemStack mainHand = player.getEquipment().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        Ageable ageable = (Ageable) entity;

        // prevent growing up upon feeding
        if (ageable.getAgeLock()) {
            HashSet<Material> breedingItems = EDIBLES.get(ageable.getType());
            if (breedingItems != null && breedingItems.contains(mainHand.getType())) {
                Bukkit.getScheduler().runTaskLater(this, ageable::setBaby, 1);
            }
            return;
        }

        ItemMeta itemMeta = mainHand.getItemMeta();
        if (!itemMeta.hasDisplayName()) {
            return;
        }

        if (!ageable.isAdult()) {
            ageable.setAgeLock(true);
            if (!(ageable instanceof Horse)) {
                ageable.setAge(Integer.MIN_VALUE);
            }
            player.sendMessage(ChatColor.GOLD + "That mob has now been age locked. How adorable!");            
            getLogger().info(new StringBuilder().append(player.getName())
                                                .append(" age locked ")
                                                .append(ageable.getType())
                                                .append(" named ")
                                                .append(ageable.getCustomName())
                                                .append(" at ")
                                                .append(ageable.getLocation())
                                                .toString());
        }

    } // onPlayerInteractEntityEvent

    // ------------------------------------------------------------------------
    /**
     * If an age locked baby mob is killed, log it.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player player = event.getEntity().getKiller();
        if (player == null || !(entity instanceof Ageable)) {
            return;
        }

        Ageable ageable = (Ageable) entity;
        if (ageable.getAgeLock()) {
            
            String extraInfo = "";
            if (entity instanceof Ocelot) {
                extraInfo = "type = " + ((Ocelot) entity).getCatType().name();
            }
            if (entity instanceof Tameable) {
                extraInfo += " owner = " + ((Tameable) entity).getOwner().getName();
            }
       
            getLogger().info(new StringBuilder().append(player.getName())
                                                .append(" killed ")
                                                .append(ageable.getType())
                                                .append(" named ")
                                                .append(ageable.getCustomName())
                                                .append(" at ")
                                                .append(ageable.getLocation())
                                                .append(". ")
                                                .append(extraInfo)
                                                .toString());
        }

    } // onEntityDeathEvent

    // ------------------------------------------------------------------------
    /**
     * A mapping from each Ageable type to all items used for breeding it.
     */
    private static final HashMap<EntityType, HashSet<Material>> EDIBLES = new HashMap<>();

} // class KeepBabyMobs
