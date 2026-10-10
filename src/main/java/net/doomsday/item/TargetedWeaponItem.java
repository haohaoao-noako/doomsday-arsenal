package net.doomsday.item;

import java.util.List;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A weapon that is aimed at a block and triggered with right-click. */
public class TargetedWeaponItem extends Item {

	@FunctionalInterface
	public interface Deployer {
		void deploy(ServerWorld world, ServerPlayerEntity player, Vec3d target);
	}

	private final Deployer deployer;
	private final int cooldownTicks;
	private final double range;

	public TargetedWeaponItem(Settings settings, int cooldownTicks, double range, Deployer deployer) {
		super(settings);
		this.cooldownTicks = cooldownTicks;
		this.range = range;
		this.deployer = deployer;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		HitResult hit = user.raycast(range, 0.0F, false);

		if (hit.getType() != HitResult.Type.BLOCK) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable("message.doomsday.no_target").formatted(Formatting.RED), true);
			}
			return TypedActionResult.fail(stack);
		}

		if (!world.isClient && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
			deployer.deploy(serverWorld, serverPlayer, hit.getPos());
			user.getItemCooldownManager().set(this, cooldownTicks);
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable(this.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.doomsday.cooldown", cooldownTicks / 20).formatted(Formatting.DARK_RED));
	}
}
