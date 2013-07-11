package com.behindthemirrors.minecraft.sRPG.dataStructures;

//import com.behindthemirrors.minecraft.sRPG.sRPG;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class TriggerEffect {
	
	public List<String> triggers;
	public ConfigurationSection node;
	public EffectDescriptor descriptor;
	
	public TriggerEffect(ConfigurationSection node, EffectDescriptor descriptor) {
		this.node = node;
		this.descriptor = descriptor;
                triggers = node.getStringList("triggers");
	}
	
        @Override
	public String toString() {
		return "Trigger: "+node.getString("action")+" ["+triggers.toString()+"]";
	}
}
