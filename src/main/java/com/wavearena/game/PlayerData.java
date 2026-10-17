package com.wavearena.game;

import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import com.wavearena.WaveArena;

/** Alles, was der Wellenmodus über einen Mitspieler weiß. */
public final class PlayerData {
	private static final Identifier LIFE_ID = WaveArena.id("upgrade_life");
	private static final Identifier POWER_ID = WaveArena.id("upgrade_power");
	private static final Identifier SPEED_ID = WaveArena.id("upgrade_speed");

	public final UUID id;
	public final String name;
	public int money;
	public int kills;
	public int earned;
	public int combo;
	public long lastKillTick = -1000;
	public boolean alive = true;
	public boolean ready;
	public boolean attributesDirty = true;
	public final int[] levels = new int[Upgrade.values().length];

	// Zustand vor dem Spiel, wird danach wiederhergestellt.
	public ItemStack[] stash;
	public double returnX;
	public double returnY;
	public double returnZ;
	public float returnYaw;
	public float returnPitch;
	public GameType returnMode = GameType.SURVIVAL;

	public PlayerData(UUID id, String name) {
		this.id = id;
		this.name = name;
	}

	public int level(Upgrade upgrade) {
		return levels[upgrade.ordinal()];
	}

	public void applyAttributes(ServerPlayer player) {
		set(player, Attributes.MAX_HEALTH, LIFE_ID, level(Upgrade.LIFE) * 2.0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ATTACK_DAMAGE, POWER_ID, level(Upgrade.POWER) * 0.07, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, level(Upgrade.SPEED) * 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		attributesDirty = false;
	}

	public void clearAttributes(ServerPlayer player) {
		set(player, Attributes.MAX_HEALTH, LIFE_ID, 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ATTACK_DAMAGE, POWER_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void set(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(id);
		if (amount != 0.0) {
			instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}
}
