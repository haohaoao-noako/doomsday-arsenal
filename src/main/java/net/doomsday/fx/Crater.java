package net.doomsday.fx;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Carves a rough sphere out of the world, growing outward a little each tick
 * so the destruction looks like a shockwave instead of one giant lag spike.
 */
public final class Crater implements Effect {
	private final ServerWorld world;
	private final BlockPos center;
	private final double max;
	private final double step;
	private double current = 0.0;

	public Crater(ServerWorld world, BlockPos center, double maxRadius, double radiusPerTick) {
		this.world = world;
		this.center = center;
		this.max = maxRadius;
		this.step = radiusPerTick;
	}

	@Override
	public boolean tick() {
		double prev = current;
		current = Math.min(max, current + step);
		double prev2 = prev * prev;
		double cur2 = current * current;
		int b = (int) Math.ceil(current * 1.06) + 1;
		int minY = world.getBottomY();
		int maxY = world.getTopY() - 1;
		BlockPos.Mutable m = new BlockPos.Mutable();
		BlockState air = Blocks.AIR.getDefaultState();

		for (int dx = -b; dx <= b; dx++) {
			int x = center.getX() + dx;
			for (int dz = -b; dz <= b; dz++) {
				int z = center.getZ() + dz;
				m.set(x, center.getY(), z);
				if (!world.isChunkLoaded(m)) {
					continue;
				}
				for (int dy = -b; dy <= b; dy++) {
					int y = center.getY() + dy;
					if (y < minY || y > maxY) {
						continue;
					}
					double jd2 = (dx * dx + dy * dy + dz * dz) * jitter(x, y, z);
					if (jd2 < prev2 || jd2 >= cur2) {
						continue;
					}
					m.set(x, y, z);
					BlockState s = world.getBlockState(m);
					if (s.isAir() || s.getHardness(world, m) < 0.0F) {
						continue;
					}
					world.setBlockState(m, air, Block.NOTIFY_LISTENERS);
				}
			}
		}
		return current >= max;
	}

	/** Stable per-block noise in [0.9, 1.25] so the crater edge is ragged. */
	private static double jitter(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return 0.9 + 0.35 * ((h & 0xFFFF) / 65535.0);
	}
}
