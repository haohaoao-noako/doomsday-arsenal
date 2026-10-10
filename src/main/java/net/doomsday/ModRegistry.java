package net.doomsday;

import net.doomsday.block.NukeBombBlock;
import net.doomsday.fx.EffectManager;
import net.doomsday.fx.MeteorShowerEffect;
import net.doomsday.fx.OrbitalStrikeEffect;
import net.doomsday.fx.SingularityEffect;
import net.doomsday.fx.ThorEffect;
import net.doomsday.item.TargetedWeaponItem;
import net.doomsday.item.TooltipBlockItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModRegistry {
	private ModRegistry() {}

	// ---- Targeted weapons (aim at a block, right-click) ----
	public static final Item ORBITAL_STRIKE_DESIGNATOR = item("orbital_strike_designator",
			new TargetedWeaponItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), 20 * 25, 128.0,
					(world, player, target) -> EffectManager.add(new OrbitalStrikeEffect(world, target))));

	public static final Item METEOR_CALLER = item("meteor_caller",
			new TargetedWeaponItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), 20 * 40, 128.0,
					(world, player, target) -> EffectManager.add(new MeteorShowerEffect(world, target))));

	public static final Item THORS_WRATH = item("thors_wrath",
			new TargetedWeaponItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), 20 * 30, 128.0,
					(world, player, target) -> EffectManager.add(new ThorEffect(world, target))));

	public static final Item SINGULARITY_CORE = item("singularity_core",
			new TargetedWeaponItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), 20 * 60, 128.0,
					(world, player, target) -> EffectManager.add(new SingularityEffect(world, target.add(0.0, 10.0, 0.0)))));

	// ---- Nuclear bomb block ----
	public static final Block NUKE_BOMB = Registry.register(Registries.BLOCK, DoomsdayMod.id("nuke_bomb"),
			new NukeBombBlock(FabricBlockSettings.create().strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL)));

	public static final Item NUKE_BOMB_ITEM = item("nuke_bomb",
			new TooltipBlockItem(NUKE_BOMB, new Item.Settings().rarity(Rarity.EPIC)));

	// ---- Creative tab ----
	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, DoomsdayMod.id("main"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(NUKE_BOMB_ITEM))
					.displayName(Text.translatable("itemGroup.doomsday.main"))
					.entries((context, entries) -> {
						entries.add(ORBITAL_STRIKE_DESIGNATOR);
						entries.add(NUKE_BOMB_ITEM);
						entries.add(METEOR_CALLER);
						entries.add(THORS_WRATH);
						entries.add(SINGULARITY_CORE);
					})
					.build());

	private static <T extends Item> T item(String name, T item) {
		return Registry.register(Registries.ITEM, DoomsdayMod.id(name), item);
	}

	/** Touching this method loads the class, which registers everything above. */
	public static void init() {}
}
