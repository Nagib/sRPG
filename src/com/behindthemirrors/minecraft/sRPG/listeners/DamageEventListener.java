
package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.sRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.MiscBukkit;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;

import org.bukkit.configuration.ConfigurationSection;

public class DamageEventListener implements Listener {
	
	private HashMap<Integer,Player> damageTracking = new HashMap<Integer,Player>();

	static ArrayList<String> ANIMALS = new ArrayList<String>(Arrays.asList(new String[] {"pig","sheep","chicken","cow","squid"}));
	static ArrayList<String> MONSTERS = new ArrayList<String>(Arrays.asList(new String[] {"zombie","spider","skeleton","creeper","slime","pigzombie","ghast","giant","wolf"}));
	static ArrayList<DamageCause> NATURAL_CAUSES = new ArrayList<DamageCause>(Arrays.asList(new DamageCause[] {DamageCause.FALL,DamageCause.FIRE,DamageCause.FIRE_TICK,DamageCause.LAVA,DamageCause.SUFFOCATION}));
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity || Settings.worldBlacklist.contains(event.getEntity().getWorld()))) {
			return;
		}
		LivingEntity source = null;
		LivingEntity target = (LivingEntity)event.getEntity();
		
		if (target instanceof Player && event.getCause() != DamageCause.FIRE_TICK&& event.getCause() != DamageCause.FIRE) {
			sRPG.dout("player damaged by "+event.getCause().name()+" ("+event.getDamage()+" damage)", "combat");
		}
		
//		if (event.getCause() != DamageCause.SUFFOCATION && event.getCause() != DamageCause.FIRE_TICK && event.getCause() != DamageCause.FIRE && event.getCause() != DamageCause.LAVA) {
//			sRPG.output(event.toString());
//			sRPG.output(event.getCause().toString());
//			sRPG.output(event.getDamage()+"");
//		}
//		if (event instanceof EntityDamageByEntityEvent) {
//			sRPG.output(((EntityDamageByEntityEvent)event).getDamager().toString());
//		}
		
		if (!NATURAL_CAUSES.contains(event.getCause())) {
			sRPG.dout("damage event: "+event.toString()+", cause:"+event.getCause().name(), "combat");
		}
		
		if (event.getCause() == DamageCause.FALL) {
			if (target instanceof Player) {
				ProfilePlayer profile = (ProfilePlayer)sRPG.profileManager.get(target);
				if (event.getCause() == DamageCause.FALL) {
					// check permissions
					
					Integer height = (int) Math.ceil(event.getEntity().getFallDistance());
					Integer damage = height - 2 + profile.getStat("fall-damage-modifier", 0);
					
					// auto-roll roll
					double roll = sRPG.generator.nextDouble();
					double autorollChance = profile.getStat("roll-chance");
					// manual roll check
					boolean manualRoll = (profile.player != null) && profile.player.isSneaking() && (System.currentTimeMillis() - ((ProfilePlayer) profile).sneakTimeStamp) < profile.getStat("manual-roll-window", 0);
					if (manualRoll || roll < autorollChance) {
						damage -= profile.getStat("roll-damage-reduction",0);
						if (manualRoll) {
							Messager.sendMessage(profile.player, "roll-manual");
						} else {
							Messager.sendMessage(profile.player, "roll-auto");
						}
					}
					// no negative damage
					if (damage < 0) {
						damage = 0;
					}
					
					event.setDamage(damage);
				}
			}
		} else if (event instanceof EntityDamageByEntityEvent && 
				((EntityDamageByEntityEvent)event).getDamager() instanceof LivingEntity ||
				event.getCause() == DamageCause.PROJECTILE) { // || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			
			EntityDamageByEntityEvent attackEvent = (EntityDamageByEntityEvent)event;
			CombatInstance combat = new CombatInstance(attackEvent);
			
			// debug message
			if (attackEvent.getDamager() instanceof LivingEntity) {
				source = (LivingEntity)attackEvent.getDamager();
				sRPG.dout("entity attack","combat");
			} else if (attackEvent.getDamager() instanceof Projectile) {
				source = ((Projectile)attackEvent.getDamager()).getShooter();
				sRPG.dout("projectile attack","combat");
				if (source instanceof Player) {
					double damage = event.getDamage();
					combat.bowcharge = (damage-2)/8.0;
					sRPG.dout("bow charge level: "+combat.bowcharge+" (from "+damage+")","combat");
				}
			}
			
			// check attack restrictions
			combat.attacker = sRPG.profileManager.get(source);
			combat.defender = sRPG.profileManager.get(target);
			if (source instanceof Player && Settings.advanced.getBoolean("combat.restrictions.enabled", false)) {
				String prefix = Settings.advanced.getString("combat.restrictions.group-prefix");
				boolean forbidden = false;
                                
                                ConfigurationSection combatRestrictionsGroupsSection = Settings.advanced.getConfigurationSection("combat.restrictions.groups");
                                
				for (String group : combatRestrictionsGroupsSection.getKeys(true)) {
					if (((Player)source).hasPermission(prefix+group)) {
						forbidden = true;
						String targetname = MiscBukkit.getEntityName(target);
						for (String otherGroup : Settings.advanced.getStringList("combat.restrictions.groups."+group)) {
							if ((target instanceof Player && ((Player)target).hasPermission(prefix+otherGroup)) || 
									(otherGroup.equalsIgnoreCase("animals") && DamageEventListener.ANIMALS.contains(targetname)) || 
									(otherGroup.equalsIgnoreCase("monsters") && DamageEventListener.MONSTERS.contains(targetname)) ) {
								forbidden = false;
							}
						}
						break;
					}
				}
				if (forbidden) {
					sRPG.dout("combat canceled because of combat restrictions","combat");
					combat.cancel();
				}
			}
			
			// resolve combat
			if (!combat.defender.entity.isDead()) {
				combat.resolve();
				sRPG.dout("combat resolved, damage changed to "+event.getDamage(),"combat");
			
				// track entity if damage source was player, for xp gain on kill
				if (!(target instanceof Player) && !event.isCancelled() && event.getDamage() > 0) {
					int id = target.getEntityId();
					if (source instanceof Player) {
						sRPG.dout("id of damaged entity: "+event.getEntity().getEntityId(),"combat");
						damageTracking.put(id, (Player)source);
					} else if (damageTracking.containsKey(id)) {
						damageTracking.remove(id);
					}
				}
			}
		}
		
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && target instanceof Player && sRPG.profileManager.profiles.containsKey((Player)target)) {
			sRPG.dout("overriding damage routine","combat");
			Player player = (Player)target;
			ProfilePlayer profile = sRPG.profileManager.get(player);
			sRPG.dout(profile.hp.toString(),"combat");
			profile.hp -= event.getDamage();
			if (profile.hp < 0) {
				profile.hp = 0.0;
			}
			Double normalized = 20 * profile.hp / profile.hp_max;
			if (normalized == 0 && profile.hp != 0) {
				normalized = 1.0;
			}
			sRPG.dout("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized","combat");
			event.setDamage(player.getHealth() - normalized);
		}
	}
	
        @EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (Settings.worldBlacklist.contains(event.getEntity().getWorld())) {
			return;
		}
		// override standard health change for players to enable variable maximum hp
		boolean deactivated = true; // not production ready yet
		if (!deactivated && !event.isCancelled() && event.getEntity() instanceof Player && sRPG.profileManager.profiles.containsKey((Player)event.getEntity())) {
			Player player = (Player)event.getEntity(); 
			ProfilePlayer profile = sRPG.profileManager.get(player);
			profile.hp += event.getAmount();
			profile.hp = profile.hp > profile.hp_max ? profile.hp_max : profile.hp;
			Double normalized = 20*profile.hp / profile.hp_max;
			if (normalized == 0 && profile.hp != 0) {
				normalized = 1.0;
			}
			sRPG.dout("player health changed to "+profile.hp+"/"+profile.hp_max+" ("+player.getHealth()+" to "+normalized+" normalized","combat");
			event.setAmount(normalized - player.getHealth());
		}
	}
	
	// check if entity was tracked, and if yes give the player who killed it xp
        @EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath (EntityDeathEvent event) {
		if (Settings.worldBlacklist.contains(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof LivingEntity)) {
			return;
		}
		LivingEntity entity = (LivingEntity)event.getEntity();
		int id = entity.getEntityId();
		sRPG.dout("entity with id "+id+" died","death");
		if (damageTracking.containsKey(id)) {
			ProfileNPC profile = sRPG.profileManager.get(entity);
			sRPG.dout("giving player"+damageTracking.get(id)+" xp","death");
			try {
				ProfilePlayer killer = sRPG.profileManager.get(damageTracking.get(id));
				killer.addXP((int) profile.getStat("xp"));
				killer.addChargeTicks(Settings.advanced.getInt("settings.charges.ticks.combat-kill", 0));
				
			} catch (NullPointerException ex) {
				sRPG.output("NPE at xp awarding, contact zaph34r about it");
				sRPG.output("profile: "+(profile == null ? null : profile.toString()));
				sRPG.output("tracking entry: "+(damageTracking.get(id) == null ? null : damageTracking.get(id).toString()));
				sRPG.output("xp stat: "+profile.getStat("xp"));
				sRPG.output("NPE end");
			}
			//TODO: maybe move saving to the data class
			sRPG.profileManager.save(damageTracking.get(id),"xp");
			sRPG.profileManager.save(damageTracking.get(id),"chargedata");
			damageTracking.remove(id);
		}
		if (!(entity instanceof Player)) {
			sRPG.dout("removing entity "+entity.toString()+" because of its death");
			sRPG.profileManager.scheduleRemoval(entity,5);
		}
	}
}
