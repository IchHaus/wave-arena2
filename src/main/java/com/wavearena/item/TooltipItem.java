package com.wavearena.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.wavearena.game.Text;

/** Item mit festen Beschreibungszeilen im Tooltip. */
public class TooltipItem extends Item {
	private final String[] lines;

	public TooltipItem(Item.Properties properties, String... lines) {
		super(properties);
		this.lines = lines;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			Consumer<Component> consumer, TooltipFlag flag) {
		for (String line : lines) {
			consumer.accept(Text.line(line, ChatFormatting.GRAY));
		}
	}
}
