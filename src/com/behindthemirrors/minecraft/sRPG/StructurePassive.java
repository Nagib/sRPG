package com.behindthemirrors.minecraft.sRPG;

import java.util.HashMap;

import org.bukkit.util.config.ConfigurationNode;

public class StructurePassive implements Comparable<StructurePassive> {

	String signature;
	String name;
	String description;
	String replaces;
	HashMap<String,ConfigurationNode> effects;
	
	public StructurePassive(String uniqueName, ConfigurationNode node) {
		signature = uniqueName;
		name = node.getString("name");
		description = node.getString("description");
		replaces = node.getString("replaces");
		effects = new HashMap<String, ConfigurationNode>();
		for (String effect : node.getKeys("effects")) {
			effects.put(effect, node.getNode("effects."+effect));
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