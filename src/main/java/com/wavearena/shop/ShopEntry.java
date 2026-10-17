package com.wavearena.shop;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import com.wavearena.game.PlayerData;
import com.wavearena.game.Text;

/** Ein Angebot im Shop. */
public abstract class ShopEntry {
	protected final String name;
	protected final Item iconItem;
	protected final int iconCount;
	protected final String[] description;

	protected ShopEntry(String name, Item iconItem, int iconCount, String... description) {
		this.name = name;
		this.iconItem = iconItem;
		this.iconCount = iconCount;
		this.description = description;
	}

	public String name() {
		return name;
	}

	/** Aktueller Preis, oder -1 wenn nicht mehr kaufbar (z. B. Upgrade maximal). */
	public abstract int price(PlayerData data);

	/** Grund, warum der Kauf gerade nicht geht, sonst null. */
	public abstract String blockReason(ServerPlayer player, PlayerData data);

	/** Führt den Kauf aus. Das Geld hat der Shop schon abgezogen. */
	public abstract void apply(ServerPlayer player, PlayerData data);

	/** Zusätzliche gelbe Zeile im Tooltip, z. B. die Upgrade-Stufe. */
	protected String status(ServerPlayer player, PlayerData data) {
		return null;
	}

	public ItemStack icon(ServerPlayer player, PlayerData data) {
		ItemStack stack = new ItemStack(iconItem, iconCount);
		stack.set(DataComponents.CUSTOM_NAME, Text.bold(name, ChatFormatting.GOLD));

		List<Component> lore = new ArrayList<>();
		int price = price(data);
		if (price >= 0) {
			boolean affordable = data.money >= price;
			lore.add(Text.line("Preis: " + price + " Münzen", affordable ? ChatFormatting.GREEN : ChatFormatting.RED));
		} else {
			lore.add(Text.line("Maximal ausgebaut", ChatFormatting.AQUA));
		}
		for (String line : description) {
			lore.add(Text.line(line, ChatFormatting.GRAY));
		}
		String status = status(player, data);
		if (status != null) {
			lore.add(Text.line(status, ChatFormatting.YELLOW));
		}
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}
}
