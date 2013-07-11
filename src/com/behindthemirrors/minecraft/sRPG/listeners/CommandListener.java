package com.behindthemirrors.minecraft.sRPG.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.behindthemirrors.minecraft.sRPG.CombatInstance;
import com.behindthemirrors.minecraft.sRPG.Messager;
import com.behindthemirrors.minecraft.sRPG.MiscGeneric;
import com.behindthemirrors.minecraft.sRPG.sRPG;
import com.behindthemirrors.minecraft.sRPG.Settings;
import com.behindthemirrors.minecraft.sRPG.dataStructures.EffectDescriptor;
import com.behindthemirrors.minecraft.sRPG.dataStructures.ProfilePlayer;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureActive;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructureJob;
import com.behindthemirrors.minecraft.sRPG.dataStructures.StructurePassive;


public class CommandListener implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			ProfilePlayer profile = sRPG.profileManager.get(player);
			if (command.getName().equals("srpg")) {
				if (args.length < 1) {
					Messager.sendMessage(player, "welcome");
					return true;
				// change locale
				} else if (args[0].equalsIgnoreCase("locale")) {
					if (args.length > 1 && Settings.localization.containsKey(args[1])) {
						profile.locale = args[1];
						sRPG.profileManager.save(profile, "locale");
						Messager.sendMessage(player, "locale-changed");
						return true;
					}
				// display xp,skillpoints,milestones (if enabled)
				} else if (args[0].equalsIgnoreCase("jobs")) {
					Messager.sendMessage(profile,"job-list-header");
					for (StructureJob job : Settings.jobs.values()) {
						if (job.prerequisitesMet(profile)) {
							Messager.sendMessage(profile, "job-list-entry",job.signature);
						} else {
							Messager.sendMessage(profile,"job-list-hidden",job.signature);
						}
					}
					return true;
				// display available charges with the current tool
				} else if (args[0].equalsIgnoreCase("charges")) {
					Messager.chargeDisplay(player, false);
					return true;
				// get info about a skill or increase it
				// TODO find NPE
				} else if (args.length >= 3 && (args[0]+" "+args[1]).equalsIgnoreCase("change to")) {
					if (Settings.worldBlacklist.contains(player.getWorld())) {
						Messager.sendMessage(player,"disabled-world");
					}
					// TODO: accommodate for names with spaces
					String name = MiscGeneric.join(new ArrayList<String>(new ArrayList<String>(Arrays.asList(args)).subList(2, args.length)), " ");
					StructureJob job = Settings.jobs.get(Settings.JOB_ALIASES.get(profile.locale).containsKey(name) ? 
							Settings.JOB_ALIASES.get(profile.locale).get(name) : name.toLowerCase());
					if (job != null) {
						if (player.hasPermission("srpg.jobs") || player.hasPermission("srpg.jobs."+job.signature)) {
							if (job == profile.currentJob) {
								Messager.sendMessage(player,"job-already-selected",job.signature);
							} else if (profile.jobAvailability.get(job)) {
								sRPG.profileManager.get(player).changeJob(job);
								Messager.sendMessage(player,"job-changed",job.signature);
							} else {
								Messager.sendMessage(player,"job-prerequisite-missing",job.signature);
//								for (Map.Entry<StructureJob, Integer> entry : job.prerequisites.entrySet()) {
//									MessageParser.sendMessage(player,"job-prerequisite-missing",entry.getKey().signature+","+entry.getValue());
//								}
							}
						} else {
							Messager.sendMessage(player,"job-no-permissions");
						}
					} else {
						Messager.sendMessage(player,"job-not-available");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("lock")) {
					profile.locked = !profile.locked;
					Messager.sendMessage(player, "actives-"+(profile.locked?"":"un")+"locked");
				} else if (args[0].equalsIgnoreCase("stats")) {
					Messager.sendMessage(player,"not-implemented");
					for (Entry<Material, HashMap<Material, HashMap<String, Double>>> entry : profile.stats.get(0).entrySet()) {
						String tab = "";
						if (entry.getKey() != null) {
							player.sendMessage("using "+entry.getKey().toString());
							tab = "    ";
						}
						for (Entry<Material, HashMap<String, Double>> subentry : entry.getValue().entrySet()) {
							String subtab = "";
							if (subentry.getKey() != null) {
								player.sendMessage(tab+"vs "+subentry.getKey().toString());
								subtab = "    ";
							}
							for (Entry<String, Double> stat : subentry.getValue().entrySet()) {
								String docstring = Messager.localize("","autodoc.effects.boost."+stat.getKey(),profile);
								if (!docstring.isEmpty()) {
									String line = Messager.parseLine(profile,docstring,stat.getValue().toString());
									if (entry.getKey() == null && subentry.getKey() == null) {
										line = line.replace("+", "");
										line = line.replace("-", "");
									}
									player.sendMessage(tab+subtab+line);
								}
							}
						}
					}
					Messager.sendMessage(player,"not-implemented");
					return true;
				} else if (args[0].equalsIgnoreCase("passive")) {
					if (args.length >= 2) {
						String name = MiscGeneric.join(Arrays.asList(args).subList(1, args.length), " ");
						if (Settings.PASSIVE_ALIASES.get(profile.locale).containsKey(name.toLowerCase())) {
							name = Settings.PASSIVE_ALIASES.get(profile.locale).get(name.toLowerCase());
						} else if (Settings.PASSIVE_ALIASES.get(null).containsKey(name.toLowerCase())) {
							name = Settings.PASSIVE_ALIASES.get(null).get(name.toLowerCase());
						}
						StructurePassive passive = Settings.passives.get(name);
						if (passive != null) {
							for (String line : Messager.documentPassive(profile, passive)) {
								player.sendMessage(line);
							}
						} else {
							Messager.sendMessage(player,"passive-not-available");
						}
						return true;
					} else {
						Messager.sendMessage(player, "needs-more-arguments");
						return false;
					}
				}else if (args[0].equalsIgnoreCase("passive")) {
					if (args.length >= 2) {
						String name = MiscGeneric.join(Arrays.asList(args).subList(1, args.length), " ");
						if (Settings.ACTIVE_ALIASES.get(profile.locale).containsKey(name.toLowerCase())) {
							name = Settings.ACTIVE_ALIASES.get(profile.locale).get(name.toLowerCase());
						} else if (Settings.ACTIVE_ALIASES.get(null).containsKey(name.toLowerCase())) {
							name = Settings.ACTIVE_ALIASES.get(null).get(name.toLowerCase());
						}
						StructureActive active = Settings.actives.get(name);
						if (active != null) {
							if (active.description != null && !active.description.isEmpty()) {
								player.sendMessage(Messager.parseLine(profile, active.description, active.signature));
							}
						} else {
							Messager.sendMessage(player,"active-not-available");
						}
						return true;
					} else {
						Messager.sendMessage(player, "needs-more-arguments");
						return false;
					}
				} else if (args[0].equalsIgnoreCase("info")) {
					String name = null;
					if (args.length >= 2) {
						name = Settings.JOB_ALIASES.get(profile.locale).get(args[2]);
						if (name == null) {
							name = Settings.jobs.containsKey(args[2].toLowerCase()) ? Settings.jobs.get(args[2].toLowerCase()).name : null;
						}
					} else {
						name = profile.currentJob.signature;
					}
					if (name != null) {
						StructureJob job = Settings.jobs.get(name);
						Integer level = profile.jobLevels.get(job);
						Messager.sendMessage(player, "job-header",name);
						if (job.prerequisitesMet(profile)) {
							Messager.sendMessage(player, "job-progress",name);
							Messager.sendMessage(player, "traits-header",name);
							for (Entry<StructurePassive,EffectDescriptor> entry : job.traits.entrySet()) {
								player.sendMessage(Messager.localizedPassive(entry.getKey().signature, profile)+MiscGeneric.potencyToRoman(entry.getValue().potency));
							}
							Messager.sendMessage(player, "passives-header",name);
							for (Entry<StructurePassive,EffectDescriptor> entry : job.getPassives(level).entrySet()) {
								player.sendMessage(Messager.localizedPassive(entry.getKey().signature, profile)+MiscGeneric.potencyToRoman(entry.getValue().potency));
							}
							Messager.sendMessage(player, "actives-header",name);
							for (Entry<StructureActive,EffectDescriptor> entry : job.getActives(level).entrySet()) {
								player.sendMessage(Messager.localizedActive(entry.getKey().signature, profile)+MiscGeneric.potencyToRoman(entry.getValue().potency));
							}
							// TODO: extend to display locked passives too or something, maybe create a special description per passive/active while it is still locked
						} else {
							Messager.sendMessage(player, "job-not-unlocked",name);
						}
					} else {
						Messager.sendMessage(player,"job-not-available");
					}
					return true;
					
				// internal help (TODO: maybe eventually replaced with some help plugin)
				} else if (args[0].equalsIgnoreCase("help")) {
					//String topic = args[1];
					// TODO: distinguish between help command with arguments and without
					Messager.sendMessage(player, "help-general");
					return true;
				}
			}
		} else {
			// server console commands
			if (command.getName().equals("srpg")) {
				// toggle debug messages
				if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
					sRPG.debug = !sRPG.debug;
					sRPG.output("debug mode set to "+sRPG.debug);
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("debug")) {
					// remove item stacks if something was incorrectly dropped
					if (args[1].equalsIgnoreCase("removeitems")) {
						for (Entity entity : sRPG.plugin.getServer().getWorlds().get(0).getEntities()) {
							if (entity instanceof Item) {
								entity.remove();
							}
						}
						sRPG.output("removed all items");
						return true;
					} else if (args[1].equalsIgnoreCase("dbpurge")) {
						ArrayList<String> columns = sRPG.database.getColumns("jobxp");
						if (args.length >= 3) {
							if (!args[2].equalsIgnoreCase("user_id") && columns.contains(args[3]) && !Settings.jobs.containsKey(args[2])) {
								columns.clear();
								columns.add(args[2]);
							} else {
								sRPG.output("invalid column, either not present or protected");
							}
						} else {
							Iterator<String> iterator = columns.iterator();
							while (iterator.hasNext()) {
								String column = iterator.next();
								if (Settings.jobs.containsKey(column) || column.equals("user_id")) {
									iterator.remove();
								}
							}
						}
						for (String column : columns) {
							sRPG.output("purging xp for job "+column);
							sRPG.database.update("UPDATE "+sRPG.database.tablePrefix+"jobxp SET "+column+" = 0;");
						}
						return true;
					} else if (args[1].equalsIgnoreCase("spawninvincible")) {
						SpawnEventListener.spawnInvincible = !SpawnEventListener.spawnInvincible;
						sRPG.output("spawn invincibility set to "+(new Boolean(SpawnEventListener.spawnInvincible).toString()));
						return true;
					} else {
						if (!sRPG.debugmodes.contains(args[1])) {
							sRPG.debugmodes.add(args[1]);
							sRPG.output("added '"+args[1]+"' to debugmodes");
						} else {
							sRPG.debugmodes.remove(args[1]);
							sRPG.output("removed '"+args[1]+"' from debugmodes");
						}
						return true;
					}
				} else if (args.length >= 2 && args[0].equalsIgnoreCase("list")) {
					// display hp values
					if (args[1].equalsIgnoreCase("tooldamage")) {
						Iterator<Map.Entry<String,Integer>> pairs = CombatInstance.damageTableTools.entrySet().iterator();
						while (pairs.hasNext()) {
							Map.Entry<String,Integer>pair = pairs.next();
							sRPG.dout(pair.getKey()+": "+pair.getValue());
						}
						return true;
					} else if (args[1].equalsIgnoreCase("inventory") && args.length >= 3) {
						ProfilePlayer profile = sRPG.profileManager.get(args[2]);
						if (profile != null) {
							for (int i = 0;i<40;i++) {
								try {
									ItemStack item = profile.player.getInventory().getItem(i);
									sRPG.dout(i+": "+item.getAmount()+" x "+item.getType().toString());
								} catch (ArrayIndexOutOfBoundsException ex) {
									sRPG.dout(i+": no valid slot");
								}
							}
						} else {
							sRPG.dout("No player by that name");
						}
						return true;
					}
					
				} else if (args.length >= 2 && sRPG.profileManager.has(args[1])) {
					ProfilePlayer profile = sRPG.profileManager.get(args[1]);
					
					if (Settings.worldBlacklist.contains(profile.player.getWorld())) {
						sRPG.output("the targeted player is in a world that is set as disabled");
					} else if (args[0].endsWith("charge")) {
						if (args[0].startsWith("un")) {
							profile.charges = 0;
						} else {
							profile.charges = 11;
						}
//						profile.updateChargeDisplay();
						sRPG.profileManager.save(profile, "chargedata");
						sRPG.output("gave player "+args[1]+" maximum charges");
					} else if (args[0].equalsIgnoreCase("enrage")) {
						StructurePassive buff = Settings.passives.get("rage");
						profile.addEffect(buff, new EffectDescriptor(10));
						sRPG.output("enraged player "+args[1]);
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
					} else if (args[0].equalsIgnoreCase("protect")) {
						StructurePassive buff = Settings.passives.get("invincibility");
						profile.addEffect(buff, new EffectDescriptor(10));
						sRPG.output("protected player "+args[1]);
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
					} else if (args[0].equalsIgnoreCase("poison")) {
						Integer potency;
						try {
							potency = Integer.parseInt(args[2]); 
						} catch (NumberFormatException ex) {
							potency = 1;
						} catch (IndexOutOfBoundsException ex) {
							potency = 1;
						}
						EffectDescriptor descriptor = new EffectDescriptor(5);
						descriptor.potency = potency < 1 ? 1 : potency;
						StructurePassive buff = Settings.passives.get(potency == 0 ? "weakpoison" : "poison");
						profile.addEffect(buff, descriptor);
						Messager.sendMessage(profile, "acquired-buff",buff.signature);
						sRPG.output("poisoned player "+args[1]);
					} else if (args[0].equalsIgnoreCase("xp") && args.length >= 3) {
						Integer amount;
						try {
							amount = Integer.parseInt(args[2]);
						} catch (NumberFormatException e) {
							sRPG.output("Not a valid number");
							return false;
						}
						
						profile.addXP(amount);
						sRPG.profileManager.save(profile, "xp");
						sRPG.output("gave "+amount.toString()+" xp to player "+args[1]);
						return true;
					} else if (args[0].equalsIgnoreCase("setboost") && args.length >= 4) {
						Double value;
						try {
							value = Double.parseDouble(args[3]);
						} catch (NumberFormatException e) {
							sRPG.output("Not a valid number");
							return false;
						}
						
						profile.stats.get(0).get(null).get(null).put(args[2], value);
						sRPG.output("Set boost "+args[2]+" to "+value);
						sRPG.output(profile.stats.toString());
						return true;
					} else {
						return false;
					}
					return true;
				}
			}
		}
		
		// reload settings (TODO: still needs proper testing, some settings might be not properly re-initialized)
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				if (!(sender instanceof Player) || ((sender instanceof Player) && ((Player)sender).hasPermission("srpg.reload"))) {
					sRPG.settings.load();
					sRPG.output("Reloaded configuration");
					return true;
				}
			}
		}
		return false;
	}
}
