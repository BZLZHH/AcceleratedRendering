package com.github.argon4w.acceleratedrendering.features.filter;

import com.github.argon4w.acceleratedrendering.configs.FeatureConfig;
import com.github.argon4w.acceleratedrendering.configs.FeatureStatus;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public class FilterFeature {

	private	static final Deque	<FeatureStatus>			ENTITIES_FILTER_CONTROLLER_STACK		= new ArrayDeque<>			();
	private	static final Deque	<FeatureStatus>			BLOCK_ENTITIES_FILTER_CONTROLLER_STACK	= new ArrayDeque<>			();
	private	static final Deque	<FeatureStatus>			ITEM_FILTER_CONTROLLER_STACK			= new ArrayDeque<>			();
	private	static final Set	<EntityType<?>>			ENTITY_FILTER_VALUES					= new ReferenceOpenHashSet<>();
	private	static final Set	<BlockEntityType<?>>	BLOCK_ENTITY_FILTER_VALUES				= new ReferenceOpenHashSet<>();
	private static final Set	<Item>					ITEM_FILTER_VALUES						= new ReferenceOpenHashSet<>();

	static {
		FeatureConfig.CONFIG.filterEntityFilterValues		.get().forEach(string -> ENTITY_FILTER_VALUES		.add(BuiltInRegistries.ENTITY_TYPE		.get(ResourceLocation.tryParse(string))));
		FeatureConfig.CONFIG.filterBlockEntityFilterValues	.get().forEach(string -> BLOCK_ENTITY_FILTER_VALUES	.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.tryParse(string))));
		FeatureConfig.CONFIG.filterItemFilterValues			.get().forEach(string -> ITEM_FILTER_VALUES			.add(BuiltInRegistries.ITEM				.get(ResourceLocation.tryParse(string))));
	}

	public static boolean isEnabled() {
		return FeatureConfig.CONFIG.filterFeatureStatus.get() == FeatureStatus.ENABLED;
	}

	public static boolean testEntity(Entity entity) {
		return getEntityFilterType().test(ENTITY_FILTER_VALUES, entity.getType());
	}

	public static boolean testBlockEntity(BlockEntity entity) {
		return getBlockEntityFilterType().test(BLOCK_ENTITY_FILTER_VALUES, entity.getType());
	}

	public static boolean testItem(ItemStack itemStack) {
		return getItemFilterType().test(ITEM_FILTER_VALUES, itemStack.getItem());
	}

	public static boolean shouldFilterEntities() {
		return getEntityFilterSetting() == FeatureStatus.ENABLED;
	}

	public static boolean shouldFilterBlockEntities() {
		return getBlockEntityFilterSetting() == FeatureStatus.ENABLED;
	}

	public static boolean shouldFilterItems() {
		return getItemFilterSetting() == FeatureStatus.ENABLED;
	}

	public static FilterType getEntityFilterType() {
		return FeatureConfig.CONFIG.filterEntityFilterType.get();
	}

	public static FilterType getBlockEntityFilterType() {
		return FeatureConfig.CONFIG.filterBlockEntityFilterType.get();
	}

	public static FilterType getItemFilterType() {
		return FeatureConfig.CONFIG.filterItemFilterType.get();
	}

	public static void disableEntityFilter() {
		ENTITIES_FILTER_CONTROLLER_STACK.push(FeatureStatus.DISABLED);
	}

	public static void disableBlockEntityFilter() {
		BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.push(FeatureStatus.DISABLED);
	}

	public static void disableItemFilter() {
		ITEM_FILTER_CONTROLLER_STACK.push(FeatureStatus.DISABLED);
	}

	public static void forceEnableEntityFilter() {
		ENTITIES_FILTER_CONTROLLER_STACK.push(FeatureStatus.ENABLED);
	}

	public static void forceEnableBlockEntityFilter() {
		BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.push(FeatureStatus.ENABLED);
	}

	public static void forceEnableItemFilter() {
		ITEM_FILTER_CONTROLLER_STACK.push(FeatureStatus.ENABLED);
	}

	public static void forceSetEntityFilter(FeatureStatus status) {
		ENTITIES_FILTER_CONTROLLER_STACK.push(status);
	}

	public static void forceSetBlockEntityFilter(FeatureStatus status) {
		BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.push(status);
	}

	public static void forceSetItemFilter(FeatureStatus status) {
		ITEM_FILTER_CONTROLLER_STACK.push(status);
	}

	public static void resetEntityFilter() {
		ENTITIES_FILTER_CONTROLLER_STACK.pop();
	}

	public static void resetBlockEntityFilter() {
		BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.pop();
	}

	public static void resetItemFilter() {
		ITEM_FILTER_CONTROLLER_STACK.pop();
	}

	public static FeatureStatus getEntityFilterSetting() {
		return ENTITIES_FILTER_CONTROLLER_STACK.isEmpty() ? getDefaultEntityFilterSetting() : ENTITIES_FILTER_CONTROLLER_STACK.peek();
	}

	public static FeatureStatus getBlockEntityFilterSetting() {
		return BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.isEmpty() ? getDefaultBlockEntityFilterSetting() : BLOCK_ENTITIES_FILTER_CONTROLLER_STACK.peek();
	}

	public static FeatureStatus getItemFilterSetting() {
		return ITEM_FILTER_CONTROLLER_STACK.isEmpty() ? getDefaultItemFilterSetting() : ITEM_FILTER_CONTROLLER_STACK.peek();
	}

	public static FeatureStatus getDefaultEntityFilterSetting() {
		return FeatureConfig.CONFIG.filterEntityFilter.get();
	}

	public static FeatureStatus getDefaultBlockEntityFilterSetting() {
		return FeatureConfig.CONFIG.filterBlockEntityFilter.get();
	}

	public static FeatureStatus getDefaultItemFilterSetting() {
		return FeatureConfig.CONFIG.filterItemFilter.get();
	}
}
