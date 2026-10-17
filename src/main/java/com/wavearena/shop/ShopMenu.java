package com.wavearena.shop;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import com.wavearena.game.Fx;
import com.wavearena.game.GameManager;
import com.wavearena.game.PlayerData;
import com.wavearena.game.Text;

/**
 * Der Shop als normales Truhen-Menü. Die Items darin sind nur Schaufenster: Klicks werden abgefangen
 * und lösen Käufe aus, nichts kann herausgenommen werden. Deshalb braucht der Shop keinen Client-Code.
 */
public class ShopMenu extends ChestMenu {
	private static final int SIZE = 54;
	private static final int TAB_START = 45;
	private static final int INFO_SLOT = 52;
	private static final int MONEY_SLOT = 53;

	private final ServerPlayer player;
	private final SimpleContainer shop;
	private ShopCatalog.Category category = ShopCatalog.Category.WEAPONS;
	private List<ShopEntry> visible = List.of();

	public ShopMenu(int containerId, Inventory inventory, SimpleContainer shop, ServerPlayer player) {
		super(MenuType.GENERIC_9x6, containerId, inventory, shop, 6);
		this.shop = shop;
		this.player = player;
		refresh();
	}

	private void refresh() {
		PlayerData data = GameManager.data(player);
		shop.clearContent();
		if (data == null) {
			return;
		}

		visible = ShopCatalog.entries(category);
		for (int i = 0; i < visible.size() && i < TAB_START; i++) {
			shop.setItem(i, visible.get(i).icon(player, data));
		}

		for (int slot = TAB_START; slot < SIZE; slot++) {
			shop.setItem(slot, filler());
		}
		ShopCatalog.Category[] tabs = ShopCatalog.Category.values();
		for (int i = 0; i < tabs.length; i++) {
			ItemStack tab = new ItemStack(tabs[i].icon);
			boolean selected = tabs[i] == category;
			tab.set(DataComponents.CUSTOM_NAME, Text.bold(tabs[i].label, selected ? ChatFormatting.GOLD : ChatFormatting.WHITE));
			if (selected) {
				tab.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}
			tab.set(DataComponents.LORE, new ItemLore(List.of(Text.line(selected ? "Angezeigt" : "Klicken zum Wechseln", ChatFormatting.GRAY))));
			shop.setItem(TAB_START + i, tab);
		}

		ItemStack info = new ItemStack(Items.BOOK);
		info.set(DataComponents.CUSTOM_NAME, Text.bold("So funktioniert's", ChatFormatting.AQUA));
		info.set(DataComponents.LORE, new ItemLore(List.of(
				Text.line("Klicke auf ein Angebot, um es zu kaufen.", ChatFormatting.GRAY),
				Text.line("Münzen bekommst du für Kills und", ChatFormatting.GRAY),
				Text.line("fürs Überstehen von Wellen.", ChatFormatting.GRAY),
				Text.line("Wenn du stirbst, verlierst du 30 % deiner Münzen.", ChatFormatting.RED))));
		shop.setItem(INFO_SLOT, info);

		ItemStack money = new ItemStack(Items.GOLD_INGOT);
		money.set(DataComponents.CUSTOM_NAME, Text.bold("Münzen: " + data.money, ChatFormatting.GOLD));
		money.set(DataComponents.LORE, new ItemLore(List.of(
				Text.line("Kills: " + data.kills, ChatFormatting.GRAY),
				Text.line("Insgesamt verdient: " + data.earned, ChatFormatting.GRAY))));
		shop.setItem(MONEY_SLOT, money);
	}

	private static ItemStack filler() {
		ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		return pane;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
		// Klicks im eigenen Inventar sind harmlos, solange nichts in den Shop wandert.
		if (slotId < 0 || slotId >= SIZE) {
			if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP_ALL) {
				return;
			}
			super.clicked(slotId, button, clickType, clicker);
			return;
		}

		// Im Shop-Bereich lösen nur normale Klicks Aktionen aus. Der Server korrigiert die Anzeige beim Client selbst.
		if (clickType != ClickType.PICKUP) {
			return;
		}

		if (slotId >= TAB_START && slotId < TAB_START + ShopCatalog.Category.values().length) {
			category = ShopCatalog.Category.values()[slotId - TAB_START];
			refresh();
			broadcastChanges();
			return;
		}
		if (slotId < visible.size()) {
			buy(visible.get(slotId));
			refresh();
			broadcastChanges();
		}
	}

	private void buy(ShopEntry entry) {
		PlayerData data = GameManager.data(player);
		if (data == null) {
			player.closeContainer();
			return;
		}
		ServerLevel level = (ServerLevel) player.level();

		String reason = entry.blockReason(player, data);
		if (reason != null) {
			deny(level, reason);
			return;
		}
		int price = entry.price(data);
		if (price < 0) {
			deny(level, "Das ist nicht mehr kaufbar.");
			return;
		}
		if (data.money < price) {
			deny(level, "Dir fehlen " + (price - data.money) + " Münzen für " + entry.name() + ".");
			return;
		}

		data.money -= price;
		entry.apply(player, data);
		player.sendSystemMessage(Text.chat(entry.name() + " gekauft (-" + price + " Münzen).", ChatFormatting.GREEN));
		Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_TRADE, 0.8F, 1.1F);
	}

	private void deny(ServerLevel level, String message) {
		player.sendSystemMessage(Text.chat(message, ChatFormatting.RED));
		Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
	}

	@Override
	public ItemStack quickMoveStack(Player clicker, int index) {
		return ItemStack.EMPTY;
	}
}
