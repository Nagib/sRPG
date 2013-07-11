package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.behindthemirrors.minecraft.sRPG.sRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.MiscBukkit;
import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;

public class SpawnEventListener implements Listener {
	
	// for testing
	static boolean spawnInvincible = false;
	
	public static ArrayList<int[]> depthTiers;
	public static boolean dangerousDepths;
	
	public void addExistingCreatures() {
		for (World world : sRPG.plugin.getServer().getWorlds()) {
			if (Settings.worldBlacklist.contains(world)) {
				continue;
			}
			for (Entity entity : world.getEntities()) {
				if (entity instanceof LivingEntity) {
					onCreatureSpawn(new CreatureSpawnEvent(entity,CreatureType.CHICKEN,entity.getLocation(), SpawnReason.NATURAL));
				}
			}
		}
	}
	
        @EventHandler(priority = EventPriority.MONITOR)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (Settings.worldBlacklist.contains(event.getLocation().getWorld())) {
			return;
		}
		String creature = MiscBukkit.getEntityName(event.getEntity());
		LivingEntity entity = (LivingEntity)event.getEntity();
                
                sRPG.output("Trying to spawn " + creature);
                
		// for testing
		if (Settings.mobs.containsKey(creature)) {
			ProfileNPC profile = sRPG.profileManager.get(entity);
			profile.currentJob = Settings.mobs.get(creature);
			if (profile.currentJob == null) {
				profile.currentJob = Settings.mobs.get("default");
				sRPG.dout("Warning: could not find fitting job for "+creature);
			}
			profile.jobLevels.put(profile.currentJob, 1);
			// depth modifier
			if (dangerousDepths) {
				for (int[] data : SpawnEventListener.depthTiers) {
					if (entity.getLocation().getY() < (double)data[0]) {
						profile.jobLevels.put(profile.currentJob, 1+data[1]);
					}
				}
			}
			profile.recalculate();
			entity.setHealth((int) profile.getStat("health"));
		} else {
			sRPG.output("Warning: spawned "+creature+", job not available");
		}
		if (spawnInvincible) {
			sRPG.profileManager.get(entity).addEffect(Settings.passives.get("invincibility"), new EffectDescriptor(10));
		}
	}
}
