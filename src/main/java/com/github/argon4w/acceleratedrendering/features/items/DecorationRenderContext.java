package com.github.argon4w.acceleratedrendering.features.items;

import net.minecraft.world.item.ItemStack;

public record DecorationRenderContext(
		ItemStack	itemStack,
		String		countString,
		int			slotX,
		int			slotY
) {

}
