package net.doomsday.fx;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * Timeline: 3.5s red targeting laser -> beam slams down from orbit -> impact,
 * shockwave, crater -> lingering plasma column with a helix.
 */
public final class OrbitalStrikeEffect implements Effect {
	private static final int CHARGE = 70;
	private static final int FALL = 8;
	private static final int IMPACT = CHARGE + FALL;
	private static final int END = IMPACT + 90;
	private static final double SKY = 300.0;

	private static final ParticleEffect LASER = Fx.dust(1.0f, 0.1f, 0.1f, 1.2f);
	private static final ParticleEffect LOCK_RING = Fx.dustFade(1.0f, 0.9f, 0.2f, 1.0f, 0.1f, 0.1f, 1.4f);
	private static final ParticleEffect BEAM = Fx.dustFade(0.35f, 0.9f, 1.0f, 1.0f, 0.95f, 0.6f, 3.0f);
	private static final ParticleEffect SHOCK = Fx.dustFade(1.0f, 0.8f, 0.3f, 1.0f, 0.25f, 0.05f, 3.0f);

	private final ServerWorld w;
	private final Vec3d t;
	private int age = 0;

	public OrbitalStrikeEffect(ServerWorld world, Vec3d target) {
		this.w = world;
		this.t = target;
	}

	@Override
	public boolean tick() {
		double x = t.x, y = t.y, z = t.z;
		if (age == 0) {
			Fx.sound(w, x, y, z, SoundEvents.BLOCK_BEACON_ACTIVATE, 6.0f, 0.5f);
			Fx.sound(w, x, y, z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 10.0f, 0.6f);
			Fx.actionbar(w, t, 96.0, Text.translatable("message.doomsday.orbital").formatted(Formatting.RED, Formatting.BOLD));
		}
		if (age < CHARGE) {
			charge(x, y, z);
		} else if (age < IMPACT) {
			fall(x, y, z);
		} else {
			if (age == IMPACT) {
				impact(x, y, z);
			}
			afterglow(x, y, z);
		}
		age++;
		return age > END;
	}

	private void charge(double x, double y, double z) {
		double prog = age / (double) CHARGE;
		if (age % 2 == 0) {
			// thin laser line from the sky
			for (double h = 0.0; h < SKY; h += 10.0) {
				Fx.burst(w, LASER, x, y + h + 5.0, z, 6, 0.03, 5.0, 0.03, 0.0);
			}
			// pulsing outer ring + collapsing inner ring
			double r1 = 9.0 + Math.sin(age * 0.35) * 0.8;
			Fx.ring(w, LASER, x, y + 0.15, z, r1, 40, age * 0.05, 0.0, 0.0);
			double r2 = Math.max(0.5, 9.0 * (1.0 - prog));
			Fx.ring(w, LOCK_RING, x, y + 0.15, z, r2, 24, -age * 0.1, 0.0, 0.0);
			// rotating crosshair
			for (int k = 0; k < 4; k++) {
				double a = k * Math.PI / 2.0 + age * 0.05;
				for (double d = 1.0; d <= 12.0; d += 1.5) {
					Fx.dir(w, LASER, x + Math.cos(a) * d, y + 0.15, z + Math.sin(a) * d, 0.0, 0.0, 0.0);
				}
			}
		}
		if (age % 10 == 0) {
			Fx.sound(w, x, y, z, SoundEvents.BLOCK_BEACON_POWER_SELECT, 4.0f, 0.8f + (float) prog);
		}
	}

	private void fall(double x, double y, double z) {
		if (age == CHARGE) {
			Fx.sound(w, x, y, z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 10.0f, 1.5f);
			Fx.screen(w, t, 120.0, 0.7f, 12, 0.0f, 0, 0xFFFFFF);
		}
		double hPrev = SKY * (1.0 - (age - CHARGE) / (double) FALL);
		double hNew = SKY * (1.0 - (age - CHARGE + 1) / (double) FALL);
		beam(x, y, z, hNew, hPrev, 1.6, 1.0);
	}

	private void beam(double x, double y, double z, double h0, double h1, double radius, double density) {
		int dustCount = Math.max(1, (int) (14 * density));
		int rodCount = Math.max(1, (int) (5 * density));
		for (double h = h0; h <= h1; h += 4.0) {
			Fx.burst(w, BEAM, x, y + h, z, dustCount, radius * 0.35, 2.0, radius * 0.35, 0.0);
			Fx.burst(w, ParticleTypes.END_ROD, x, y + h, z, rodCount, radius * 0.15, 2.0, radius * 0.15, 0.0);
		}
	}

	private void impact(double x, double y, double z) {
		Fx.screen(w, t, 260.0, 3.0f, 100, 1.0f, 28, 0xFFFFFF);
		Fx.sound(w, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, 14.0f, 0.55f);
		Fx.sound(w, x, y, z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 12.0f, 0.5f);
		Fx.sound(w, x, y, z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 10.0f, 0.5f);
		Fx.sound(w, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 12.0f, 0.6f);

		Fx.burst(w, ParticleTypes.FLASH, x, y + 1.0, z, 3, 1.0, 1.0, 1.0, 0.0);
		Fx.burst(w, ParticleTypes.EXPLOSION_EMITTER, x, y + 2.0, z, 5, 5.0, 2.0, 5.0, 0.0);
		Fx.burst(w, ParticleTypes.END_ROD, x, y + 1.0, z, 500, 0.5, 0.5, 0.5, 0.9);
		Fx.burst(w, ParticleTypes.FLAME, x, y + 1.0, z, 500, 0.5, 0.5, 0.5, 0.5);
		Fx.burst(w, ParticleTypes.LAVA, x, y + 1.0, z, 120, 1.0, 1.0, 1.0, 0.4);
		Fx.burst(w, ParticleTypes.LARGE_SMOKE, x, y + 1.0, z, 200, 1.0, 1.0, 1.0, 0.4);

		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4.0;
			double bx = x + Math.cos(a) * 14.0;
			double bz = z + Math.sin(a) * 14.0;
			Fx.bolt(w, bx, Fx.groundY(w, bx, bz, y), bz, true);
		}

		EffectManager.add(new Crater(w, Fx.pos(x, y - 3.0, z), 14.0, 2.5));
		Fx.blast(w, t, 24.0, 70.0f, 2.5, 8);
	}

	private void afterglow(double x, double y, double z) {
		int k = age - IMPACT;
		double fade = Math.max(0.05, 1.0 - k / 90.0);

		// lingering plasma column
		if (age % 2 == 0) {
			beam(x, y, z, 0.0, SKY, 2.4 * (0.6 + 0.4 * fade), fade);
		}
		// expanding shockwave disc
		if (k < 28) {
			double r = k * 1.9;
			int pts = (int) Math.min(140.0, 20.0 + r * 4.0);
			Fx.ring(w, ParticleTypes.CLOUD, x, y + 0.6, z, r, pts, k * 0.3, 0.0, 0.03);
			Fx.ring(w, SHOCK, x, y + 0.8, z, r * 0.92, Math.max(12, pts / 2), 0.0, 0.0, 0.0);
		}
		// burning crater rim
		if (k < 60) {
			Fx.burst(w, ParticleTypes.FLAME, x, y + 0.5, z, 24, 3.5, 0.4, 3.5, 0.04);
			Fx.burst(w, ParticleTypes.LARGE_SMOKE, x, y + 2.0, z, 8, 3.0, 1.0, 3.0, 0.03);
		}
		// double helix around the beam
		if (k < 70) {
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 26; j++) {
					double a = age * 0.45 + j * 0.5 + i * Math.PI;
					double h = j * 4.0;
					double rr = 4.5 + Math.sin(j * 0.4 + age * 0.2) * 0.8;
					Fx.dir(w, ParticleTypes.END_ROD, x + Math.cos(a) * rr, y + h, z + Math.sin(a) * rr, 0.0, 0.05, 0.0);
				}
			}
		}
	}
}
