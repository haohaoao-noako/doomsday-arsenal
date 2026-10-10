package net.doomsday.block;

import net.doomsday.fx.NukeEffect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Arm it with flint & steel / fire charge or with a redstone signal.
 * You have 5 seconds. Break the block before then to defuse it.
 */
public class NukeBombBlock extends Block {

	public NukeBombBlock(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemStack stack = player.getStackInHand(hand);
		boolean flint = stack.isOf(Items.FLINT_AND_STEEL);
		if (!flint && !stack.isOf(Items.FIRE_CHARGE)) {
			return super.onUse(state, world, pos, player, hand, hit);
		}
		if (!world.isClient) {
			arm(world, pos);
			if (!player.isCreative()) {
				if (flint) {
					stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
				} else {
					stack.decrement(1);
				}
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!world.isClient && !oldState.isOf(state.getBlock()) && world.isReceivingRedstonePower(pos)) {
			arm(world, pos);
		}
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		if (!world.isClient && world.isReceivingRedstonePower(pos)) {
			arm(world, pos);
		}
	}

	private static void arm(World world, BlockPos pos) {
		if (world instanceof ServerWorld serverWorld) {
			NukeEffect.arm(serverWorld, pos);
		}
	}
}
