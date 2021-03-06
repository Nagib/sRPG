package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfileNPC;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureJob;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class ProfileManager {
	
	public HashMap<LivingEntity,ProfileNPC> profiles = new HashMap<LivingEntity,ProfileNPC>();
	public HashMap<LivingEntity,Integer> entitiesScheduledForRemoval = new HashMap<LivingEntity,Integer>();
	
	public void clear() {
		profiles.clear();
	}
	// create new PlayerData and load data from database
	public void add(Player player) {
		ProfilePlayer profile = new ProfilePlayer();
		profiles.put(player,profile);
		profile.entity = (LivingEntity)player; 
		profile.player = player;
		profile.name = player.getName();
		profile.locale = Settings.defaultLocale;
		
		profile.id = sRPG.database.getSingleIntValue("users", "user_id", "user", player.getName());
		if (profile.id == null) {
			enterIntoDatabase(player);
			sRPG.output("created player data");
		}
//		profile.initializeHUD();
		load(player);
//		profile.updateChargeDisplay();
		if (!sRPG.debug && Settings.config.getStringList("debuggers").contains(profile.name)) {
			sRPG.debug = true;
			player.getWorld().setTime(0);
			sRPG.dout("Debug mode enabled ("+profile.name+" has joined)");
		}
	}
	
	public void add(LivingEntity entity) {
		ProfileNPC profile = new ProfileNPC();
		profile.entity = entity;
		profiles.put(entity, profile);
	}
	
	public void remove(Player player) {
		save(player);
		profiles.remove(player);
	}
	
	public void remove(LivingEntity entity) {
		profiles.remove(entity);
	}
	
	public void scheduleRemoval(LivingEntity entity, int delay) {
		entitiesScheduledForRemoval.put(entity, delay);
	}
	
	public void checkEntityRemoval() {
		Iterator<Entry<LivingEntity,Integer>> iterator = entitiesScheduledForRemoval.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<LivingEntity,Integer> entry = iterator.next();
			entry.setValue(entry.getValue()-1);
			if (entry.getValue()<=0) {
				iterator.remove();
				remove(entry.getKey());
			}
		}
	}
	
	public ProfileNPC get(LivingEntity entity) {
		if (!profiles.containsKey(entity)) {
			add(entity);
			sRPG.dout("added new entity "+entity.toString()+" as it was requested and not already assigned a profile");
		}
		return profiles.get(entity);
	}
	
	public ProfilePlayer get(Player player) {
		return (ProfilePlayer)profiles.get(player);
	}
	
	public ProfilePlayer get(String name) {
		for (ProfileNPC profile : profiles.values()) {
			if (profile instanceof ProfilePlayer && ((ProfilePlayer)profile).name.equals(name)) {
				return (ProfilePlayer)profile;
			}
		}
		return null;
	}
	
	public boolean has(ProfilePlayer profile) {
		return profiles.containsKey(profile.player);
	}
	
	public boolean has(String name) {
		return has(sRPG.plugin.getServer().getPlayer(name));
	}
	
	public boolean has(Player player) {
		return profiles.containsKey(player);
	}

	// load player data from database
	public void load(Player player) {
		ProfilePlayer profile = (ProfilePlayer)profiles.get(player);
		// read current class
		String jobname = sRPG.database.getSingleStringValue("users", "currentjob", "user_id", profile.id);
		if (!Settings.jobs.containsKey(jobname)) {
			jobname = Settings.initialJobs.get(sRPG.generator.nextInt(Settings.initialJobs.size())).signature;
		}
		profile.currentJob = Settings.jobs.get(jobname);
		// read locale
		profile.locale = sRPG.database.getSingleStringValue("users", "locale", "user_id", profile.id);
		// change to default locale if the set locale is not available anymore
		if (!Settings.localization.containsKey(profile.locale)) {
			profile.locale = Settings.defaultLocale;
			save(player,"locale");
		}
		
		// read hp
		profile.hp = sRPG.database.getSingleDoubleValue("users", "hp", "user_id", profile.id);
		profile.hp_max = 40.0;
		//Integer normalized = data.hp*20 / data.hp_max;
		//player.setHealth(normalized == 0 && data.hp != 0 ? 1 : normalized);

		// read job xp
		ArrayList<String> jobs = new ArrayList<String>();
		jobs.addAll(Settings.jobs.keySet());
		ArrayList<Integer> xp = sRPG.database.getSingleIntRow("jobxp", jobs, "user_id", profile.id);
		profile.jobXP.clear();
		profile.jobAvailability.clear();
		profile.jobLevels.clear();
		for (int i=0; i < jobs.size();i++) {
			StructureJob job = Settings.jobs.get(jobs.get(i));
			profile.jobXP.put(job, xp.get(i));
			if (xp.get(i) > 0 || job.maximumLevel <= 1 || job == profile.currentJob) {
				profile.checkLevelUp(job);
			}
		}
		profile.charges = sRPG.database.getSingleIntValue("users", "charges", "user_id", profile.id);
		profile.chargeProgress = sRPG.database.getSingleIntValue("users", "chargeprogress", "user_id", profile.id);
		profile.suppressRecalculation = false;
		profile.changeJob(profile.currentJob);
		profile.suppressMessages = false;
	}
	
	// create database entry for player
	public void enterIntoDatabase(Player player) { 
		String name = player.getName();
		ProfilePlayer profile = (ProfilePlayer)profiles.get(player);
		sRPG.dout("trying to enter "+name+" into the database","db");
		HashMap<String,String> map = new HashMap<String, String>();
		map.put("user", name);
		map.put("hp", ""+player.getHealth());
		map.put("locale", profile.locale);
		sRPG.database.insertStringValues("users", map);
		
		sRPG.dout("users table written, proceeding to fetch id","db");
		profile.id = sRPG.database.getSingleIntValue("users", "user_id", "user", name);
		sRPG.database.insertSingleIntValue("jobxp", "user_id", profile.id);
	}
	
	// save all data
	public void save(Player player) {
		save(player,"");
	}
	
	public void save(ProfilePlayer profile) {
		save(profile,"");
	}
	
	public void save(Player player, String partial) {
		save((ProfilePlayer)profiles.get(player),partial);
	}
	
	// save specific part of data to database
	public void save(ProfilePlayer profile, String partial) {
		// write xp
		if (partial.isEmpty() || partial.equalsIgnoreCase("xp")) {
			//TODO: FIND NPE !!
			sRPG.database.setSingleIntValue("jobxp", profile.currentJob.signature, profile.jobXP.get(profile.currentJob), "user_id", profile.id);
		}
		// write job
		if (partial.isEmpty() || partial.equalsIgnoreCase("job")) {
			sRPG.database.setSingleStringValue("users", "currentjob", profile.currentJob.signature, "user_id", profile.id);
		}
		// write hp
		if (partial.isEmpty() || partial.equalsIgnoreCase("hp")) {
			sRPG.database.setSingleDoubleValue("users", "hp", profile.hp, "user_id", profile.id);
		}
		// write locale
		if (partial.isEmpty() || partial.equalsIgnoreCase("locale")) {
			sRPG.database.setSingleStringValue("users", "locale", profile.locale, "user_id", profile.id);
		}
		// write charge data
		if (partial.isEmpty() || partial.equalsIgnoreCase("chargedata")) {
			sRPG.database.setSingleIntValue("users", "charges", profile.charges, "user_id", profile.id);
			sRPG.database.setSingleIntValue("users", "chargeprogress", profile.chargeProgress, "user_id", profile.id);
		}
	}
	
}
