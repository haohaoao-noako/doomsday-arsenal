package net.doomsday.fx;

import java.util.HashSet;
import java.util.Set;

import net.doomsday.ModRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 5s countdown (break the block to defuse) -> blinding flash, fireball, expanding
 * shockwave that eats the terrain, rising mushroom cloud, radioactive fallout.
 */
public final class NukeEffect implements Effect {
	private static final int FUSE = 100;
	private static final int TOTAL = FUSE + 300;
	private static final double CRATER_RADIUS = 30.0;

	private static final Set<String> ARMED = new HashSet<>();

	private static final ParticleEffect WARN = Fx.dust(1.0f, 0.1f, 0.05f, 1.5f);
	private static final ParticleEffect FIRE_BLOB = Fx.dustFade(1.0f, 0.55f, 0.1f, 0.25f, 0.2f, 0.2f, 4.0f);
	private static final ParticleEffect SMOKE_BLOB = Fx.dustFade(0.55f, 0.5f, 0.5f, 0.15f, 0.15f, 0.15f, 4.0f);
	private static final ParticleEffect SURGE = Fx.dust(0.92f, 0.88f, 0.82f, 4.0f);
	private static final ParticleEffect FALLOUT = Fx.dust(0.5f, 1.0f, 0.2f, 2.0f);

	private final ServerWorld w;
	private final BlockPos origin;
	private final Vec3d c;
	private final String key;
	private int age = 0;
	private double stemPrev = 0.0;

	private NukeEffect(ServerWorld w, BlockPos origin, String key) {
		this.w = w;
		this.origin = origin;
		this.c = Vec3d.ofCenter(origin);
		this.key = key;
	}

	/** Called by the block. Safe to call repeatedly for the same position. */
	public static void arm(ServerWorld w, BlockPos pos) {
		String key = w.getRegistryKey().getValue() + "@" + pos.asLong();
		if (ARMED.add(key)) {
			EffectManager.add(new NukeEffect(w, pos.toImmutable(), key));
		}
	}

	public static void clearArmed() {
		ARMED.clear();
	}

	@Override
	public boolean tick() {
		if (age < FUSE) {
			if (!Fx.loaded(w, origin)) {
				return false; // chunk unloaded: pause the countdown
			}
			if (!w.getBlockState(origin).isOf(ModRegistry.NUKE_BOMB)) {
				ARMED.remove(key);
				Fx.actionbar(w, c, 64.0, Text.translatable("message.doomsday.nuke_defused").formatted(Formatting.GREEN, Formatting.BOLD));
				return true; // bomb was broken: defused
			}
			countdown();
		} else if (age == FUSE) {
			detonate();
		}
		if (age >= FUSE) {
			aftermath(age - FUSE);
		}
		age++;
		return age > TOTAL;
	}

	private void countdown() {
		int left = FUSE - age;
		if (age == 0) {
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 8.0f, 0.6f);
		}
		if (age % 20 == 0) {
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, 6.0f, 0.5f + 1.2f * age / FUSE);
			int sec = (left + 19) / 20;
			Fx.actionbar(w, c, 160.0, Text.translatable("message.doomsday.nuke_countdown", sec).formatted(Formatting.RED, Formatting.BOLD));
		}
		if (age % 2 == 0) {
			double pulse = 1.0 + 0.15 * Math.sin(age * 0.4);
			Fx.ring(w, WARN, c.x, c.y - 0.4, c.z, 6.0 * pulse, 36, age * 0.08, 0.0, 0.0);
			Fx.ring(w, WARN, c.x, c.y - 0.4, c.z, 3.0 * pulse, 18, -age * 0.12, 0.0, 0.0);
			Fx.burst(w, WARN, c.x, c.y + 14.0, c.z, 10, 0.1, 14.0, 0.1, 0.0);
		}
		if (left < 40) {
			Fx.burst(w, ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.0, c.z, 10, 1.5, 1.5, 1.5, 0.3);
		}
	}

	private void detonate() {
		w.removeBlock(origin, false);
		ARMED.remove(key);

		Fx.screen(w, c, 450.0, 4.0f, 140, 1.0f, 70, 0xFFF4E0);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, 30.0f, 0.45f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, 30.0f, 0.35f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 20.0f, 0.4f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_DEATH, 15.0f, 0.5f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 30.0f, 0.4f);

		Fx.burst(w, ParticleTypes.FLASH, c.x, c.y + 2.0, c.z, 5, 2.0, 2.0, 2.0, 0.0);
		Fx.burst(w, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 2.0, c.z, 12, 8.0, 2.0, 8.0, 0.0);
		Fx.burst(w, ParticleTypes.END_ROD, c.x, c.y + 2.0, c.z, 700, 0.5, 0.5, 0.5, 1.3);
		Fx.burst(w, ParticleTypes.FLAME, c.x, c.y + 2.0, c.z, 700, 0.5, 0.5, 0.5, 0.8);
		Fx.burst(w, ParticleTypes.LAVA, c.x, c.y + 2.0, c.z, 200, 1.0, 1.0, 1.0, 0.6);
		Fx.burst(w, ParticleTypes.LARGE_SMOKE, c.x, c.y + 2.0, c.z, 300, 1.0, 1.0, 1.0, 0.6);

		EffectManager.add(new Crater(w, new BlockPos(origin.getX(), origin.getY() - 3, origin.getZ()), CRATER_RADIUS, 1.3));
		Fx.blast(w, c, 60.0, 400.0f, 4.0, 15);
	}

	private void aftermath(int k) {
		double x = c.x, y = c.y, z = c.z;

		// 1. rising fireball
		if (k < 36) {
			double r = 3.0 + k * 0.45;
			double cy = y + 2.0 + k * 0.35;
			Fx.burst(w, FIRE_BLOB, x, cy, z, 90, r * 0.45, r * 0.35, r * 0.45, 0.0);
			Fx.burst(w, ParticleTypes.FLAME, x, cy, z, 120, r * 0.4, r * 0.3, r * 0.4, 0.04);
		}

		// 2. mushroom: stem + umbrella cap
		if (k < 110) {
			double u = Math.min(1.0, k / 90.0);
			double h = 55.0 * (1.0 - (1.0 - u) * (1.0 - u));
			for (double hh = stemPrev; hh <= h; hh += 2.5) {
				double wob = Math.sin(hh * 0.2 + k * 0.1) * 0.4;
				Fx.burst(w, SMOKE_BLOB, x + wob, y + hh, z, 6, 1.8, 1.2, 1.8, 0.0);
				if (hh < 14.0) {
					Fx.burst(w, ParticleTypes.FLAME, x, y + hh, z, 4, 1.5, 1.0, 1.5, 0.02);
				}
			}
			stemPrev = h;

			double hy = y + h + 4.0;
			int pts = 28;
			for (int i = 0; i < pts; i++) {
				double a = i * (Math.PI * 2.0 / pts) + k * 0.13;
				double cs = Math.cos(a), sn = Math.sin(a);
				double v1 = 0.16 + Fx.RNG.nextDouble() * 0.05;
				Fx.dir(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, x + cs * 3.0, hy, z + sn * 3.0, cs * v1, 0.06, sn * v1);
				if (i % 2 == 0) {
					Fx.dir(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, x + cs * 5.0, hy - 3.0, z + sn * 5.0,
							cs * v1 * 1.4, -0.015, sn * v1 * 1.4);
				}
			}
			Fx.burst(w, FIRE_BLOB, x, hy, z, 30, 5.0, 2.5, 5.0, 0.0);
			Fx.burst(w, ParticleTypes.FLAME, x, hy, z, 30, 5.0, 2.0, 5.0, 0.03);
		}

		// 3. ground-hugging base surge
		if (k < 46 && k % 2 == 0) {
			double r = 2.0 + k * 1.3;
			int pts = (int) Math.min(120.0, 24.0 + r * 2.2);
			Fx.ring(w, SURGE, x, y + 0.5, z, r, pts, k * 0.2, 0.0, 0.04);
		}

		// 4. sparkling shock dome
		if (k < 32) {
			double rr = k * 2.3;
			for (int i = 0; i < 40; i++) {
				Vec3d u = Fx.randomUnit();
				double uy = Math.abs(u.y);
				Fx.dir(w, ParticleTypes.END_ROD, x + u.x * rr, y + uy * rr, z + u.z * rr, 0.0, 0.0, 0.0);
			}
		}

		// 5. second shockwave hits everything farther out
		if (k == 8) {
			Fx.blast(w, c, 80.0, 120.0f, 3.0, 8);
		}

		// 6. lightning storm around the blast
		if (k == 4) {
			for (int i = 0; i < 14; i++) {
				double a = Fx.RNG.nextDouble() * Math.PI * 2.0;
				double d = 20.0 + Fx.RNG.nextDouble() * 30.0;
				double bx = x + Math.cos(a) * d;
				double bz = z + Math.sin(a) * d;
				Fx.bolt(w, bx, Fx.groundY(w, bx, bz, y), bz, true);
			}
		}

		// 7. fallout
		if (k >= 30 && k < 260 && k % 5 == 0) {
			Fx.burst(w, ParticleTypes.ASH, x, y + 16.0, z, 160, 36.0, 14.0, 36.0, 0.0);
			Fx.burst(w, FALLOUT, x, y + 12.0, z, 40, 30.0, 12.0, 30.0, 0.0);
		}

		// 8. radiation
		if (k >= 20 && k < 280 && k % 20 == 0) {
			Box box = new Box(x - 40, y - 30, z - 40, x + 40, y + 60, z + 40);
			for (Entity e : w.getOtherEntities(null, box, en -> en instanceof LivingEntity)) {
				if (e.squaredDistanceTo(x, y, z) < 40.0 * 40.0) {
					((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
				}
			}
		}
	}
}
