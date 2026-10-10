package net.doomsday.fx;

import java.util.Random;

import net.doomsday.DoomsdayMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;

/** Shared helpers for particles, sound, damage and screen effects. */
public final class Fx {
	public static final Random RNG = new Random();
	/** Particles are sent to players up to this far away (vanilla default is only 32). */
	private static final double VIEW_DISTANCE = 400.0;

	private Fx() {}

	// ------------------------------------------------------------------ particles

	/** Random cloud of particles. With speed > 0 the particles explode outward (gaussian velocities). */
	public static void burst(ServerWorld w, ParticleEffect p, double x, double y, double z,
			int count, double ox, double oy, double oz, double speed) {
		int n = Math.max(1, count);
		for (ServerPlayerEntity pl : w.getPlayers()) {
			if (pl.squaredDistanceTo(x, y, z) <= VIEW_DISTANCE * VIEW_DISTANCE) {
				w.spawnParticles(pl, p, true, x, y, z, n, ox, oy, oz, speed);
			}
		}
	}

	/** A single particle with an explicit velocity (count 0 trick). */
	public static void dir(ServerWorld w, ParticleEffect p, double x, double y, double z,
			double vx, double vy, double vz) {
		for (ServerPlayerEntity pl : w.getPlayers()) {
			if (pl.squaredDistanceTo(x, y, z) <= VIEW_DISTANCE * VIEW_DISTANCE) {
				w.spawnParticles(pl, p, true, x, y, z, 0, vx, vy, vz, 1.0);
			}
		}
	}

	/** Horizontal ring. vOut = radial velocity, vUp = vertical velocity. */
	public static void ring(ServerWorld w, ParticleEffect p, double cx, double cy, double cz,
			double r, int points, double rot, double vOut, double vUp) {
		for (int i = 0; i < points; i++) {
			double a = rot + i * (Math.PI * 2.0 / points);
			double c = Math.cos(a);
			double s = Math.sin(a);
			dir(w, p, cx + c * r, cy, cz + s * r, c * vOut, vUp, s * vOut);
		}
	}

	/** Vertical ring, either in the XY plane (alongX) or the YZ plane. */
	public static void ringVertical(ServerWorld w, ParticleEffect p, double cx, double cy, double cz,
			double r, int points, double rot, boolean alongX) {
		for (int i = 0; i < points; i++) {
			double a = rot + i * (Math.PI * 2.0 / points);
			double c = Math.cos(a);
			double s = Math.sin(a);
			if (alongX) {
				dir(w, p, cx + c * r, cy + s * r, cz, 0, 0, 0);
			} else {
				dir(w, p, cx, cy + s * r, cz + c * r, 0, 0, 0);
			}
		}
	}

	public static Vec3d randomUnit() {
		double u = RNG.nextDouble() * 2.0 - 1.0;
		double a = RNG.nextDouble() * Math.PI * 2.0;
		double s = Math.sqrt(1.0 - u * u);
		return new Vec3d(s * Math.cos(a), u, s * Math.sin(a));
	}

	public static ParticleEffect dust(float r, float g, float b, float scale) {
		return new DustParticleEffect(new Vector3f(r, g, b), scale);
	}

	public static ParticleEffect dustFade(float r1, float g1, float b1, float r2, float g2, float b2, float scale) {
		return new DustColorTransitionParticleEffect(new Vector3f(r1, g1, b1), new Vector3f(r2, g2, b2), scale);
	}

	// ------------------------------------------------------------------ sound / text

	public static void sound(ServerWorld w, double x, double y, double z, SoundEvent s, float volume, float pitch) {
		w.playSound(null, x, y, z, s, SoundCategory.BLOCKS, volume, pitch);
	}

	public static void actionbar(ServerWorld w, Vec3d at, double range, Text msg) {
		for (ServerPlayerEntity pl : w.getPlayers()) {
			if (pl.getPos().distanceTo(at) <= range) {
				pl.sendMessage(msg, true);
			}
		}
	}

	// ------------------------------------------------------------------ client screen effects

	/** Camera shake + full-screen flash, scaled by each player's distance. */
	public static void screen(ServerWorld w, Vec3d at, double range, float shake, int shakeTicks,
			float flash, int flashTicks, int rgb) {
		for (ServerPlayerEntity pl : w.getPlayers()) {
			double d = pl.getPos().distanceTo(at);
			if (d > range) {
				continue;
			}
			double f = 1.0 - d / range;
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeFloat((float) (shake * (0.2 + 0.8 * f)));
			buf.writeInt(shakeTicks);
			buf.writeFloat((float) (flash * (0.1 + 0.9 * f * f)));
			buf.writeInt(flashTicks);
			buf.writeInt(rgb);
			ServerPlayNetworking.send(pl, DoomsdayMod.SCREEN_FX, buf);
		}
	}

	// ------------------------------------------------------------------ world helpers

	public static BlockPos pos(Vec3d v) {
		return pos(v.x, v.y, v.z);
	}

	public static BlockPos pos(double x, double y, double z) {
		return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
	}

	public static boolean loaded(ServerWorld w, BlockPos p) {
		return w.isChunkLoaded(p);
	}

	/** Y of the surface at x/z, or the fallback if the chunk is not loaded. */
	public static double groundY(ServerWorld w, double x, double z, double fallback) {
		BlockPos p = pos(x, fallback, z);
		if (!loaded(w, p)) {
			return fallback;
		}
		return w.getTopY(Heightmap.Type.MOTION_BLOCKING, p.getX(), p.getZ());
	}

	public static void bolt(ServerWorld w, double x, double y, double z, boolean cosmetic) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt == null) {
			return;
		}
		bolt.refreshPositionAfterTeleport(x, y, z);
		bolt.setCosmetic(cosmetic);
		w.spawnEntity(bolt);
	}

	/** Damage + knockback + fire to every living entity around c. */
	public static void blast(ServerWorld w, Vec3d c, double radius, float maxDamage, double knock, int fireSeconds) {
		Box box = new Box(c.x - radius, c.y - radius, c.z - radius, c.x + radius, c.y + radius, c.z + radius);
		DamageSource src = w.getDamageSources().explosion(null, null);
		for (Entity e : w.getOtherEntities(null, box, en -> en instanceof LivingEntity)) {
			Vec3d mid = e.getPos().add(0.0, e.getHeight() * 0.5, 0.0);
			double d = mid.distanceTo(c);
			if (d > radius) {
				continue;
			}
			double f = 1.0 - d / radius;
			e.damage(src, (float) (maxDamage * f));
			Vec3d dir = mid.subtract(c);
			dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 1.0, 0.0) : dir.normalize();
			double k = knock * f;
			e.addVelocity(dir.x * k, Math.min(1.5, 0.25 + dir.y * k), dir.z * k);
			e.velocityModified = true;
			if (fireSeconds > 0 && f > 0.15) {
				e.setOnFireFor(fireSeconds);
			}
		}
	}
}
