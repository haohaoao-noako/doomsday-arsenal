package net.doomsday.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * A black hole with a tilted, spinning accretion disk and relativistic jets. It drags in
 * entities and eats the terrain around it, collapses, then goes off like a supernova.
 */
public final class SingularityEffect implements Effect {
	private static final int GROW = 160;
	private static final int COLLAPSE = 30;
	private static final int BOOM = GROW + COLLAPSE;
	private static final int END = BOOM + 50;
	private static final double PULL_RANGE = 38.0;

	private static final ParticleEffect VOID = Fx.dust(0.02f, 0.0f, 0.04f, 4.0f);
	private static final ParticleEffect DISK_IN = Fx.dustFade(1.0f, 0.75f, 0.3f, 0.7f, 0.15f, 1.0f, 1.7f);
	private static final ParticleEffect DISK_OUT = Fx.dustFade(0.3f, 0.5f, 1.0f, 0.6f, 0.1f, 0.9f, 1.7f);
	private static final ParticleEffect INFALL = Fx.dustFade(0.8f, 0.4f, 1.0f, 1.0f, 1.0f, 1.0f, 1.2f);
	private static final ParticleEffect BLAST = Fx.dustFade(1.0f, 1.0f, 1.0f, 0.6f, 0.1f, 1.0f, 4.0f);

	private final ServerWorld w;
	private final Vec3d c;
	private int age = 0;

	public SingularityEffect(ServerWorld world, Vec3d center) {
		this.w = world;
		this.c = center;
	}

	@Override
	public boolean tick() {
		if (age == 0) {
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_SPAWN, 8.0f, 0.5f);
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, 6.0f, 0.6f);
			Fx.actionbar(w, c, 128.0, Text.translatable("message.doomsday.singularity").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
			EffectManager.add(new Crater(w, Fx.pos(c), 12.0, 0.08));
		}

		if (age < BOOM) {
			double s = age < GROW ? 1.0 : Math.max(0.04, 1.0 - (age - GROW) / (double) COLLAPSE);
			if (age == GROW) {
				Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 12.0f, 0.5f);
			}
			if (age % 30 == 0) {
				Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 8.0f, 0.5f);
			}
			if (age % 8 == 0) {
				Fx.screen(w, c, 70.0, 0.35f, 10, 0.0f, 0, 0xFFFFFF);
			}
			render(s);
			pull();
		} else {
			if (age == BOOM) {
				explode();
			}
			shock(age - BOOM);
		}
		age++;
		return age > END;
	}

	private void render(double s) {
		// black core
		double coreR = Math.min(3.2, 0.8 + age * 0.02) * s;
		Fx.burst(w, VOID, c.x, c.y, c.z, 36, coreR * 0.35, coreR * 0.35, coreR * 0.35, 0.0);

		// tilted accretion disk with 4 spiral arms (inner parts spin faster)
		double tilt = 0.38;
		double sinT = Math.sin(tilt), cosT = Math.cos(tilt);
		for (int arm = 0; arm < 4; arm++) {
			for (int j = 0; j < 13; j++) {
				double r = (2.4 + j * 1.05) * s;
				double a = arm * (Math.PI / 2.0) + j * 0.3 - age * (0.9 / Math.sqrt(r + 1.0));
				double px = Math.cos(a) * r;
				double pz = Math.sin(a) * r;
				double py = pz * sinT;
				pz = pz * cosT;
				Fx.dir(w, j < 6 ? DISK_IN : DISK_OUT, c.x + px, c.y + py, c.z + pz, 0.0, 0.0, 0.0);
			}
		}

		// polar jets
		for (int d = -1; d <= 1; d += 2) {
			for (int i = 0; i < 2; i++) {
				Fx.dir(w, ParticleTypes.END_ROD,
						c.x + Fx.RNG.nextGaussian() * 0.2, c.y + d * 1.5 * s, c.z + Fx.RNG.nextGaussian() * 0.2,
						0.0, d * 0.9, 0.0);
			}
		}

		// matter streaming in from all directions
		int n = age < GROW ? 10 : 22;
		for (int i = 0; i < n; i++) {
			Vec3d u = Fx.randomUnit();
			double dist = 20.0 + Fx.RNG.nextDouble() * 14.0;
			Fx.dir(w, INFALL, c.x + u.x * dist, c.y + u.y * dist, c.z + u.z * dist, -u.x * 0.7, -u.y * 0.7, -u.z * 0.7);
		}
	}

	private void pull() {
		Box box = new Box(c.x - PULL_RANGE, c.y - PULL_RANGE, c.z - PULL_RANGE,
				c.x + PULL_RANGE, c.y + PULL_RANGE, c.z + PULL_RANGE);
		for (Entity e : w.getOtherEntities(null, box)) {
			if (e instanceof PlayerEntity p && p.isSpectator()) {
				continue;
			}
			Vec3d to = c.subtract(e.getPos().add(0.0, e.getHeight() * 0.5, 0.0));
			double d = to.length();
			if (d > PULL_RANGE || d < 0.01) {
				continue;
			}
			double f = 1.0 - d / PULL_RANGE;
			Vec3d n = to.multiply(1.0 / d);
			double strength = (0.03 + 0.17 * f * f) * (age < GROW ? 1.0 : 1.8);
			e.addVelocity(n.x * strength, n.y * strength, n.z * strength);
			e.velocityModified = true;
			e.fallDistance = 0.0F;
			if (d < 3.0) {
				if (e instanceof ItemEntity || e instanceof ExperienceOrbEntity) {
					e.discard();
				} else if (e instanceof LivingEntity && age % 4 == 0) {
					e.damage(w.getDamageSources().magic(), 5.0f);
				}
			}
		}
	}

	private void explode() {
		Fx.screen(w, c, 300.0, 3.5f, 100, 1.0f, 45, 0xF2E6FF);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, 20.0f, 0.5f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 16.0f, 0.4f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_DEATH, 10.0f, 0.6f);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 20.0f, 0.5f);

		Fx.burst(w, ParticleTypes.FLASH, c.x, c.y, c.z, 4, 1.5, 1.5, 1.5, 0.0);
		Fx.burst(w, ParticleTypes.END_ROD, c.x, c.y, c.z, 600, 0.3, 0.3, 0.3, 1.1);
		Fx.burst(w, BLAST, c.x, c.y, c.z, 500, 0.3, 0.3, 0.3, 1.0);
		Fx.burst(w, ParticleTypes.DRAGON_BREATH, c.x, c.y, c.z, 300, 0.5, 0.5, 0.5, 0.6);
		Fx.burst(w, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 6, 4.0, 4.0, 4.0, 0.0);

		Fx.blast(w, c, 42.0, 220.0f, 3.5, 10);
		EffectManager.add(new Crater(w, Fx.pos(c), 22.0, 2.0));
	}

	/** Three expanding rings (one flat, two vertical) look like a spherical shell from a distance. */
	private void shock(int k) {
		double r = k * 2.2;
		if (r > 90.0) {
			return;
		}
		int pts = (int) Math.min(90.0, 24.0 + r * 1.5);
		Fx.ring(w, BLAST, c.x, c.y, c.z, r, pts, k * 0.1, 0.0, 0.0);
		Fx.ringVertical(w, BLAST, c.x, c.y, c.z, r, pts, k * 0.1, true);
		Fx.ringVertical(w, BLAST, c.x, c.y, c.z, r, pts, k * 0.1, false);
	}
}
