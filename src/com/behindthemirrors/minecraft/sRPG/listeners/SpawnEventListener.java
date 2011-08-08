package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.behindthemirrors.minecraft.sRPG.SRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.Utility;
import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;



public class SpawnEventListener extends EntityListener {
	
	static boolean debug = false;
	// for testing
	static boolean spawnInvincible = false;
	
	public static ArrayList<int[]> depthTiers;
	public static boolean dangerousDepths;
	
	public void addExistingCreatures() {
		for (World world : SRPG.plugin.getServer().getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (entity instanceof LivingEntity) {
					onCreatureSpawn(new CreatureSpawnEvent(entity,CreatureType.CHICKEN,entity.getLocation(), SpawnReason.NATURAL));
				}
			}
		}
	}
	
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		String creature = Utility.getEntityName(event.getEntity());
		if (debug) {
			SRPG.output("creature spawned: "+creature);
		}
		LivingEntity entity = (LivingEntity)event.getEntity();
		// for testing
		if (Settings.mobs.containsKey(creature)) {
			ProfileNPC profile = SRPG.profileManager.get(entity);
			profile.currentJob = Settings.mobs.get(creature);
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
			SRPG.output("Warning: spawned "+creature+", job not available");
		}
		if (spawnInvincible) {
			SRPG.profileManager.get(entity).addEffect(Settings.passives.get("invincibility"), new EffectDescriptor(10));
		}
	}
}