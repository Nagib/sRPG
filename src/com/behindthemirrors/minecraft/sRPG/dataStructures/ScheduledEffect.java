package com.behindthemirrors.minecraft.sRPG.dataStructures;

import com.behindthemirrors.minecraft.sRPG.ResolverActive;
import org.bukkit.configuration.ConfigurationSection;

public class ScheduledEffect {
	
	String effect;
	ConfigurationSection node;
	ArgumentsActive arguments;
	public int ticksToActivation;
	
	public ScheduledEffect(Integer delay, String effect, ConfigurationSection node, ArgumentsActive arguments) {
		this.effect = effect;
		this.node = node;
		this.arguments = arguments;
		ticksToActivation = delay;
	}
	
	public void activate() {
		ResolverActive.resolveActiveEffect(effect, node, arguments);
	}
	
	@Override
	public String toString() {
		return effect+" ("+ticksToActivation+")";
	}
}
