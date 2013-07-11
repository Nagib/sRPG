package com.behindthemirrors.minecraft.sRPG.dataStructures;

import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;

public class TriggerEffect {
	
	public ArrayList<String> triggers;
	public ConfigurationSection node;
	public EffectDescriptor descriptor;
	
	public TriggerEffect(ConfigurationSection node, EffectDescriptor descriptor) {
		this.node = node;
		this.descriptor = descriptor;
		triggers = (ArrayList<String>) node.getStringList("triggers");
	}
	
	public String toString() {
		return "Trigger: "+node.getString("action")+" ["+triggers.toString()+"]";
	}
}
