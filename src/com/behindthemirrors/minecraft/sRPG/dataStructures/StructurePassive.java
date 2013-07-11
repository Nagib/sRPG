package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

public class StructurePassive implements Comparable<StructurePassive> {

	public String signature;
	public String name;
	public String description;
	public String adjective;
	String replaces;
	public HashMap<String,ConfigurationSection> effects;
	
	public StructurePassive(String uniqueName, ConfigurationSection node) {
		signature = uniqueName;
		name = node.getString("name");
		description = node.getString("description");
		adjective = node.getString("adjective");
		replaces = node.getString("replaces");
		effects = new HashMap<String, ConfigurationSection>();
                
                ConfigurationSection section = node.getConfigurationSection("effects");
                
		for (String effect : section.getKeys(false)) {
                        ConfigurationSection effectSection = node.getConfigurationSection("effects"+effect);
                        
			effects.put(effect, effectSection);
		}
	}

	public Integer getPotency() {
		Integer potency = 1;
		if (signature.contains("!")) {
			try {
				potency = Integer.parseInt(signature.substring(signature.indexOf("!")+1));
			} catch (NumberFormatException ex) {
			}
		}
		return potency;
	}
	
	@Override
	public int compareTo(StructurePassive other) {
		return name.compareTo(other.name);
	}
	
	@Override
	public String toString() {
		return signature;
		
	}
	
}
