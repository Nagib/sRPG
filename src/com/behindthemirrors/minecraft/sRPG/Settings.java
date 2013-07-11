package com.behindthemirrors.minecraft.sRPG;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureActive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureJob;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.Watcher;
import com.behindthemirrors.minecraft.sRPG.listeners.BlockEventListener;
import com.behindthemirrors.minecraft.sRPG.listeners.SpawnEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

public class Settings {
	
	static String difficulty;
	
	static File dataFolder;
	
	static boolean mySQLenabled;
	
	public static Configuration config;
	public static Configuration advanced;
	public static Configuration jobsettings;
        	
	public static HashMap<String,StructureActive> actives;
	public static HashMap<String,StructurePassive> passives;
	public static HashMap<String,StructureJob> jobs;
	public static HashMap<String,StructureJob> mobs;
	static ArrayList<StructureJob> initialJobs;
	
	public static ArrayList<World> worldBlacklist = new ArrayList<World>();
	
	public static HashMap<String,Configuration> localization;
	static String defaultLocale;
	
	public static HashMap<String, HashMap<String, String>> JOB_ALIASES;
	public static HashMap<String, HashMap<String, String>> PASSIVE_ALIASES;
	public static HashMap<String, HashMap<String, String>> ACTIVE_ALIASES;
	public static ArrayList<String> SKILLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","axes","pickaxes","shovels","hoes","bow","ukemi","evasion", "focus"}));
	static ArrayList<String> TOOLS = new ArrayList<String>(Arrays.asList(new String[] {"swords","pickaxes","axes","shovels","hoes"}));
	static ArrayList<String> GRADES =  new ArrayList<String>(Arrays.asList(new String[] {"wood","stone","iron","gold","diamond"}));
	
	static Material[] TOOL_MATERIALS = {Material.WOOD_SWORD,Material.STONE_SWORD,Material.IRON_SWORD,Material.GOLD_SWORD, Material.DIAMOND_SWORD,
									    Material.WOOD_PICKAXE,Material.STONE_PICKAXE,Material.IRON_PICKAXE,Material.GOLD_PICKAXE,Material.DIAMOND_PICKAXE,
									    Material.WOOD_AXE,Material.STONE_AXE,Material.IRON_AXE,Material.GOLD_AXE,Material.DIAMOND_AXE,
									    Material.WOOD_SPADE,Material.STONE_SPADE,Material.IRON_SPADE,Material.GOLD_SPADE,Material.DIAMOND_SPADE,
									    Material.WOOD_HOE,Material.STONE_HOE,Material.IRON_HOE,Material.GOLD_HOE,Material.DIAMOND_HOE};
	static HashMap<Material,String> TOOL_MATERIAL_TO_STRING = new HashMap<Material,String>();
	public static HashMap<Material,String> TOOL_MATERIAL_TO_TOOL_GROUP = new HashMap<Material,String>();
	static {
		// initialize Material to string mappings
		for (int i = 0; i < TOOLS.size(); i++) {
			int length = GRADES.size();
			for (int j = 0; j < length; j++) {
				TOOL_MATERIAL_TO_STRING.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i)+"."+GRADES.get(j));
				TOOL_MATERIAL_TO_TOOL_GROUP.put(TOOL_MATERIALS[i*length+j], TOOLS.get(i));
			}
		}
		TOOL_MATERIAL_TO_TOOL_GROUP.put(Material.BOW,"ranged");
		TOOL_MATERIAL_TO_STRING.put(Material.BOW,"ranged.bow");
	}
	static ArrayList<Double> ARMOR_FACTORS;
	
	public static ArrayList<Material> BLOCK_CLICK_BLACKLIST = new ArrayList<Material>(Arrays.asList(new Material[] {Material.BED,
															Material.BED_BLOCK,Material.DISPENSER,Material.FURNACE,Material.BURNING_FURNACE,Material.JUKEBOX,
															Material.NOTE_BLOCK,Material.STORAGE_MINECART,Material.WOOD_DOOR,
															Material.WOODEN_DOOR,Material.CHEST,Material.WORKBENCH,Material.TNT,
															Material.MINECART,Material.BOAT,Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.TRAP_DOOR}));
	
	
	static HashMap<String, HashMap<String, String>> nameReplacements;

	Configuration openConfig(File folder, String name, String description, String defaultFileName) {
		File file = MiscGeneric.createDefaultFile(new File(folder, name+".yml"),description, defaultFileName+".yml");
                
		if (file.exists()) {
			Configuration configuration = YamlConfiguration.loadConfiguration(file);
                        
			// TODO: add try/catch for .yml parsing errors
			// configuration.setDefaults(config);
			return configuration;
		} else {
			sRPG.output("Error loading "+description+" ("+name+".yml)");
			return null;
		}
	}
	
	public void load() {
		// get config data
		Boolean disable = false;
		config = openConfig(dataFolder,"config","basic configuration","config");
		advanced = openConfig(dataFolder,"config_advanced","advanced configuration","config_advanced");
		
		if (config == null || advanced == null) {
			disable = true;
		} else {
			// default debug modes
			sRPG.debugmodes = (ArrayList<String>) config.getStringList("debugmodes");
			
			// read world data
			sRPG.output("loading world data");
			worldBlacklist.clear();
			for (String worldname : config.getStringList("settings.disabled-worlds")) {
				World world = sRPG.plugin.getServer().getWorld(worldname);
				if (world != null) {
					worldBlacklist.add(world);
				}
			}
			
			// read locale data
			ArrayList<String> availableLocales = (ArrayList<String>)config.getStringList("settings.locales.available");
			defaultLocale = config.getString("settings.locales.default");
			if (!availableLocales.contains(defaultLocale)) {
				availableLocales.add(defaultLocale);
			}
			localization = new HashMap<String,Configuration>();
			nameReplacements = new HashMap<String, HashMap<String,String>>();
			JOB_ALIASES = new HashMap<String, HashMap<String,String>>();
			PASSIVE_ALIASES = new HashMap<String, HashMap<String,String>>();
			ACTIVE_ALIASES = new HashMap<String, HashMap<String,String>>();
			
			// create plugin default locale file
			for (String name : new String[] {"EN"}) {
				MiscGeneric.createDefaultFile(new File(new File(dataFolder,"locales"),name+".yml"), "'"+name+"' locale settings", "locale"+name+".yml");
			}
			// create plugin default difficulties
			for (String name : new String[] {"default","original"}) {
				MiscGeneric.createDefaultFile(new File(new File(dataFolder,"difficulties"),name+".yml"), "'"+name+"' difficulty settings", "difficulty_"+name+".yml");
				MiscGeneric.createDefaultFile(new File(new File(dataFolder,"difficulties"),name+"_mobs.yml"), "'"+name+"' mob settings", "definitions_mobs_"+name+".yml");
			}
			
			for (String locale : availableLocales) {
				File file = new File(new File(dataFolder,"locales"),locale+".yml");
				// plugin default locale
				if (!file.exists()){
					sRPG.output("Error loading locale '"+locale+"', initializing from EN");
					// create copy of EN for specified locale if no file is present
					file = MiscGeneric.createDefaultFile(new File(new File(dataFolder,"locales"),locale+".yml"), "'"+locale+"' locale settings", "locale_EN.yml");
				}
				// disable plugin if file could not be created or opened
				if (!file.exists()) {
					disable = true;
				} else {
					localization.put(locale, YamlConfiguration.loadConfiguration(file));
					// TODO: add try/catch for .yml parsing errors
					//localization.get(locale).load(); 
					                                            
                                        ConfigurationSection jobsSection = localization.get(locale).getConfigurationSection("jobs");
                                        ConfigurationSection passivesSection = localization.get(locale).getConfigurationSection("passives");
                                        ConfigurationSection activesSection = localization.get(locale).getConfigurationSection("actives");
                                        
					// update skill aliases
					JOB_ALIASES.put(locale,new HashMap<String,String>());
					if (jobsSection != null) {
						for (String name : jobsSection.getKeys(false)) {
							JOB_ALIASES.get(locale).put(localization.get(locale).getString("jobs."+name).toLowerCase(),name);
						}
					}
					PASSIVE_ALIASES.put(locale,new HashMap<String,String>());
					if (passivesSection != null) {
						for (String name : passivesSection.getKeys(false)) {
							PASSIVE_ALIASES.get(locale).put(localization.get(locale).getString("passives."+name).toLowerCase(),name);
						}
					}
					ACTIVE_ALIASES.put(locale,new HashMap<String,String>());
					if (activesSection != null) {
						for (String name : activesSection.getKeys(false)) {
							ACTIVE_ALIASES.get(locale).put(localization.get(locale).getString("actives."+name).toLowerCase(),name);
						}
					}
				}
			}
		
			// update player locales if set locales are not available anymore
			for (LivingEntity entity : sRPG.profileManager.profiles.keySet()) {
				if (!(entity instanceof Player)) {
					continue;
				}
				Player player = (Player)entity;
				ProfilePlayer profile = sRPG.profileManager.get(player);
				if (!Settings.localization.containsKey(profile.locale)) {
					profile.locale = defaultLocale;
					sRPG.profileManager.save(player,"locale");
					sRPG.output("changed locale for player "+profile.name+" to default");
				}
			}
			
			// read config data
                        ConfigurationSection node = config.getConfigurationSection("mySQL");
			mySQLenabled = node.getBoolean("enabled", false);
			Database db = new Database();
			if (mySQLenabled) {
				db.server = node.getString("server");
				db.port = node.getString("port");
				db.name = node.getString("dbName");
				db.user = node.getString("dbUser");
				db.pass = node.getString("dbPass");
			}
			db.tablePrefix = node.getString("table_prefix");
			sRPG.database = db;
			
			// read ability settings
			ProfilePlayer.chargeMax = 10; //TODO: make configurable (needs integration with spout GUI)
			ProfilePlayer.ticksPerCharge = advanced.getInt("settings.charges.ticks-to-charge", 1);
			
			// read difficulty/combat settings
			difficulty = config.getString("settings.combat.difficulty");
			File file = new File(new File(dataFolder,"difficulties"),difficulty+".yml");
			if (!file.exists()){
				sRPG.output("Error loading settings for difficulty '"+difficulty+"', initializing from default");
				// create copy of default settings for specified difficulty name if no file is present
				file = MiscGeneric.createDefaultFile(new File(new File(dataFolder,"difficulties"),difficulty+".yml"), "'"+difficulty+"' difficulty settings", "difficulty_default.yml");
			}
			// disable plugin if file could not be created or opened
			if (!file.exists()) {
				disable = true;
			} else {
				Configuration difficultyConfig = YamlConfiguration.loadConfiguration(file);
				// TODO: add try/catch for .yml parsing errors
				//difficultyConfig.load();
                                
				ARMOR_FACTORS = new ArrayList<Double>();
				for (String type : new String[] {"leather","chain","iron","diamond","gold"}) {
					ARMOR_FACTORS.add(difficultyConfig.getDouble("settings.combat.armor-strength."+type, 1.0));
				}
				
				// damage increase with depth
				SpawnEventListener.dangerousDepths = config.getBoolean("settings.combat.dangerous-depths", false);
				
				SpawnEventListener.depthTiers = new ArrayList<int[]>();
				ArrayList<Integer> thresholds = (ArrayList<Integer>) difficultyConfig.getIntegerList("settings.dangerous-depths.thresholds");
				ArrayList<Integer> levelincrease = (ArrayList<Integer>) difficultyConfig.getIntegerList("settings.dangerous-depths.level-increase");
				if (thresholds.size() == levelincrease.size()) {
					for (int i = 0;i < thresholds.size();i++) {
						SpawnEventListener.depthTiers.add(new int[] {thresholds.get(i),levelincrease.get(i)});
					}
				} else {
					sRPG.output("Warning: Invalid depth settings in difficulty config");
				}
				
				// tool damage
				CombatInstance.damageTableTools = new HashMap<String, Integer>();                                
                                ConfigurationSection statsTools = difficultyConfig.getConfigurationSection("stats.tools");
                                
				try {
					for (String toolgroup : statsTools.getKeys(false)) {
                                            
                                                ConfigurationSection toolgroupSection = difficultyConfig.getConfigurationSection("stats.tools." + toolgroup);
                                            
						for (String tool : toolgroupSection.getKeys(false)) {
							tool = toolgroup+"."+tool;
							if (!node.getBoolean(tool+".override", false)) {
								int basedamage = node.getInt(tool+".damage", 1);
								CombatInstance.damageTableTools.put(tool, basedamage);
								CombatInstance.damageTableTools.put(tool+"-range", Math.max(node.getInt(tool+".max-damage", basedamage),basedamage)-basedamage);
							}
						}
					}
				} catch (NullPointerException ex) {
					sRPG.output("Error in difficulty configuration, check tool damage section");
				}
			}
			
			jobsettings = openConfig(dataFolder,"job_settings","class configuration","job_settings");
			Configuration passiveDefinitions = openConfig(new File(dataFolder,"definitions"), "passives", "skill definitions","definitions_passives");
			Configuration activeDefinitions = openConfig(new File(dataFolder,"definitions"), "actives", "ability definitions","definitions_actives");
			Configuration jobDefinitions = openConfig(new File(dataFolder,"definitions"), "jobs", "job definitions","definitions_jobs");
			Configuration mobDefinitions = openConfig(new File(dataFolder,"difficulties"), difficulty+"_mobs", "'"+difficulty+"'mob definitions","definitions_mobs_default");
			if (jobsettings == null || passiveDefinitions == null || activeDefinitions == null || jobDefinitions == null || mobDefinitions == null) {
				disable = true;
			} else {
				// load job xp formula
				StructureJob.xp_base = jobsettings.getDouble("settings.xp.base", 1000);
				StructureJob.xp_offset = jobsettings.getDouble("settings.xp.offset", 0);
				StructureJob.level_coefficient = jobsettings.getDouble("settings.xp.level-coefficient", 1);
				StructureJob.level_exponent = jobsettings.getDouble("settings.xp.level-exponent", 1);
				StructureJob.tier_coefficient = jobsettings.getDouble("settings.xp.tier-coefficient", 1);
				StructureJob.tier_exponent = jobsettings.getDouble("settings.xp.tier-exponent", 1);
				// load job prefixes
				StructureJob.ranks = new HashMap<Integer, String>();
                                
                                ConfigurationSection jobPrefixes = jobsettings.getConfigurationSection("job-prefixes");
                                ConfigurationSection section;
                                
				if (jobPrefixes != null) {
					for (String prefix : jobPrefixes.getKeys(false)) {
						StructureJob.ranks.put(Integer.parseInt(prefix.substring(prefix.indexOf(" ")+1)), jobsettings.getString("job-prefixes."+prefix));
					}
				}
				// load skill definitions
				passives = new HashMap<String, StructurePassive>();
				PASSIVE_ALIASES.put(null,new HashMap<String, String>());
				for (String signature : passiveDefinitions.getKeys(false)) {
                                        section =  passiveDefinitions.getConfigurationSection(signature);
					passives.put(signature, new StructurePassive(signature, section));
					PASSIVE_ALIASES.get(null).put(passives.get(signature).name.toLowerCase(), signature);
				}
				sRPG.output("loaded "+(new Integer(passives.size())).toString()+" "+MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.passive"),passives.size()));
				
				// load ability definitions
				actives = new HashMap<String, StructureActive>();
				ACTIVE_ALIASES.put(null,new HashMap<String, String>());
				for (String signature : activeDefinitions.getKeys(false)) {
                                        section = activeDefinitions.getConfigurationSection(signature);
					actives.put(signature, new StructureActive(signature, section));
					ACTIVE_ALIASES.get(null).put(actives.get(signature).name.toLowerCase(), signature);
				}
				sRPG.output("loaded "+(new Integer(actives.size())).toString()+" "+MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.active"),actives.size()));
				
				// load job definitions
				jobs = new HashMap<String, StructureJob>();
				JOB_ALIASES.put(null,new HashMap<String, String>());
				for (String signature : jobDefinitions.getKeys(false)) {
                                        ConfigurationSection tree = jobsettings.getConfigurationSection("tree");
                                        
					if (tree.contains(signature) && jobDefinitions.getBoolean(signature+".enabled", true)) {
                                                section = jobDefinitions.getConfigurationSection(signature);
						jobs.put(signature, new StructureJob(signature, section));
						JOB_ALIASES.get(null).put(jobs.get(signature).name.toLowerCase(), signature);
						
						// load job prerequisites from jobtree
						jobs.get(signature).prerequisites = new HashMap<StructureJob, Integer>();
                                                
                                                ConfigurationSection treePrerequisites = jobsettings.getConfigurationSection("tree."+signature+".prerequisites");
                                                
                                                
						if (treePrerequisites != null) {
							for (String prereq : treePrerequisites.getKeys(false)) {
								jobs.get(signature).prerequisites.put(jobs.get(prereq), jobsettings.getInt("tree."+signature+".prerequisites."+prereq, 1));
							}
						} 
					}
				}
				// disable all jobs with missing prerequisites
				ArrayList<String> deactivate = new ArrayList<String>();
				for (String signature : jobs.keySet()) {
					for (StructureJob job : jobs.get(signature).prerequisites.keySet()) {
						if (job == null || !jobs.containsKey(job.signature)) {
							deactivate.add(signature);
							deactivate.addAll(MiscBukkit.getChildren(jobs, signature));
							break;
						}
					}
				}
				deactivate = new ArrayList<String>(new HashSet<String>(deactivate));
				for (String signature : deactivate) {
					jobs.remove(signature);
				}
				// populate default job list
				initialJobs = new ArrayList<StructureJob>();
				for (StructureJob job : jobs.values()) {
					if (job.prerequisites.isEmpty()){
						initialJobs.add(job);
					}
				}
				
				// load mobs
				mobs = new HashMap<String, StructureJob>();
				for (String creature : mobDefinitions.getKeys(false)) {
                                        section = mobDefinitions.getConfigurationSection(creature);
					mobs.put(creature, new StructureJob(creature, section));
				}
				
				// status report
				sRPG.output("loaded "+(new Integer(jobs.size())).toString()+" "+MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"),jobs.size()));
				if (deactivate.size() > 0) {
					sRPG.output((new Integer(deactivate.size())).toString()+" "+MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"),deactivate.size())+" could not be loaded due to missing prerequisites");
				}
				if (jobs.isEmpty()) {
					sRPG.output(MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"), 1)+" tree is empty!");
					disable = true;
				} else if (initialJobs.isEmpty()) {
					sRPG.output("No "+MiscBukkit.parseSingularPlural(localization.get(defaultLocale).getString("terminology.job"), 1)+" without prerequisites available in the job tree!");
					disable = true;
				}
			}
			
			// block xp and charge settings
			BlockEventListener.materialToXpGroup.clear();
			BlockEventListener.xpValuesMin.clear();
			BlockEventListener.xpValuesRange.clear();
			BlockEventListener.xpChances.clear();
                        
                        ConfigurationSection settingsBlocksGroups = Settings.advanced.getConfigurationSection("settings.blocks.groups");
                        
                        for (String group : settingsBlocksGroups.getKeys(false)) {
                                
				String name = group; 
				node = Settings.advanced.getConfigurationSection("settings.blocks.groups." + name);
                                
				if (name.equalsIgnoreCase("default")) {
					name = null;
				} 
				for (Material material : MiscBukkit.parseMaterialList(node.getStringList("materials"))) {
					BlockEventListener.materialToXpGroup.put(material,name);
				}
				String valueString = node.getString("xp");
				Integer min = 1;
				Integer max = 1;
				String[] tokens = valueString.split("-");
				try {
					min = Integer.parseInt(tokens[0]);
					try {
						max = Integer.parseInt(tokens[1]);
					} catch (IndexOutOfBoundsException ex) {
					}
				} catch (NumberFormatException ex) {
				}
				max = Math.max(min, max);
				BlockEventListener.xpValuesMin.put(name, min);
				BlockEventListener.xpValuesRange.put(name, max-min);
				BlockEventListener.xpChances.put(name, node.getDouble("chance",1.0));
				
				BlockEventListener.chargeTicks.put(name, node.getInt("charge-ticks",0));
			}
		} 
		// disable plugin if anything went wrong while loading configuration
		if (disable) {
			sRPG.output("disabling plugin");
			sRPG.pm.disablePlugin(sRPG.plugin);
		} else {
			sRPG.output("Successfully loaded config");
		}
	}
}
