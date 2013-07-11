package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.behindthemirrors.minecraft.sRPG.MiscBukkit;

public class StructureActive implements Comparable<StructureActive> {

	public String name;
	public String signature;
	public String description;
	public String feedback;
	public String broadcast;
	public Double broadcastRange;
	public Integer cost;
	public Integer range;
	Double cooldown;
	String replaces;
	public HashMap<String,ConfigurationSection> effects;
	boolean combat;
	
	public ArrayList<Material> validMaterials;
	public ArrayList<Material> versusMaterials;
	
	public StructureActive(String uniqueName, ConfigurationSection node) {
		signature = uniqueName;
		name = node.getString("name");
		description = node.getString("description");
		feedback = node.getString("feedback");
		broadcast = node.getString("broadcast");
		broadcastRange = node.getDouble("broadcast-range", 0.0);
		
		cost = node.getInt("cost",0);
		range = node.getInt("range",5);
		cooldown = node.getDouble("cooldown", 0);
		replaces = node.getString("replaces");
		effects = new HashMap<String, ConfigurationSection>();
                
                ConfigurationSection effectsSection = node.getConfigurationSection("effects");
                
		if (effectsSection != null) {
			for (String effect : effectsSection.getKeys(false)) {
                                ConfigurationSection effectsEffectSection = node.getConfigurationSection("effects."+effect);
				effects.put(effect, effectsEffectSection);
			}
		}
		validMaterials = MiscBukkit.parseMaterialList(node.getStringList("tools"));
		versusMaterials = MiscBukkit.parseMaterialList(node.getStringList("versus"));
		
		combat = node.getBoolean("combat", false);
	}

	public boolean validVs(ProfileNPC profile) {
		if (versusMaterials.isEmpty() || 
				(profile instanceof ProfilePlayer && versusMaterials.contains( ((ProfilePlayer)profile).player.getItemInHand().getType() ))) {
			return true;
		}
		return false;
	}
	
	public boolean validVs(Material material) {
		if (versusMaterials.isEmpty() || versusMaterials.contains(material)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(StructureActive other) {
		return name.compareTo(other.name);
	}
	
	@Override
	public String toString() {
		return signature+" "+validMaterials.toString();
	}
	
}
