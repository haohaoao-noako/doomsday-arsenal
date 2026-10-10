package net.doomsday.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** 26 flaming meteors rain down around the target, each leaving a smoking trail and a crater. */
public final class MeteorShowerEffect implements Effect {
	private static final int TOTAL = 26;
	private static final double AREA = 26.0;
	private static final double SPEED = 2.6;

	private static final ParticleEffect HEAD = Fx.dustFade(1.0f, 0.9f, 0.5f, 1.0f, 0.3f, 0.05f, 4.0f);
	private static final ParticleEffect TRAIL = Fx.dustFade(1.0f, 0.5f, 0.1f, 0.2f, 0.2f, 0.2f, 3.0f);

	private static final class Meteor {
		double x, y, z, vx, vy, vz;
	}

	private final ServerWorld w;
	private final Vec3d c;
	private final List<Meteor> meteors = new ArrayList<>();
	private int age = 0;
	private int spawned = 0;

	public MeteorShowerEffect(ServerWorld world, Vec3d target) {
		this.w = world;
		this.c = target;
	}

	@Override
	public boolean tick() {
		if (age == 0) {
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_SPAWN, 6.0f, 0.5f);
			Fx.actionbar(w, c, 96.0, Text.translatable("message.doomsday.meteor").formatted(Formatting.GOLD, Formatting.BOLD));
			Fx.screen(w, c, 120.0, 0.6f, 30, 0.0f, 0, 0xFFFFFF);
		}
		if (spawned < TOTAL && age % 4 == 0) {
			spawn();
			spawned++;
		}
		Iterator<Meteor> it = meteors.iterator();
		while (it.hasNext()) {
			if (step(it.next())) {
				it.remove();
			}
		}
		age++;
		return spawned >= TOTAL && meteors.isEmpty();
	}

	private void spawn() {
		double ang = Fx.RNG.nextDouble() * Math.PI * 2.0;
		double dist = Math.sqrt(Fx.RNG.nextDouble()) * AREA;
		double tx = c.x + Math.cos(ang) * dist;
		double tz = c.z + Math.sin(ang) * dist;
		double ty = Fx.groundY(w, tx, tz, c.y);

		Meteor m = new Meteor();
		m.x = tx + (Fx.RNG.nextDouble() - 0.5) * 70.0;
		m.z = tz + (Fx.RNG.nextDouble() - 0.5) * 70.0;
		m.y = ty + 130.0;
		Vec3d v = new Vec3d(tx - m.x, ty - m.y, tz - m.z).normalize().multiply(SPEED);
		m.vx = v.x;
		m.vy = v.y;
		m.vz = v.z;
		meteors.add(m);
		Fx.sound(w, tx, ty, tz, SoundEvents.ITEM_FIRECHARGE_USE, 3.0f, 0.5f);
	}

	/** @return true when the meteor is gone. */
	private boolean step(Meteor m) {
		for (int s = 0; s < 3; s++) {
			m.x += m.vx / 3.0;
			m.y += m.vy / 3.0;
			m.z += m.vz / 3.0;
			if (m.y < w.getBottomY()) {
				return true;
			}
			BlockPos bp = Fx.pos(m.x, m.y, m.z);
			if (!Fx.loaded(w, bp)) {
				continue;
			}
			if (!w.getBlockState(bp).isAir()) {
				impact(m);
				return true;
			}
		}

		Fx.burst(w, HEAD, m.x, m.y, m.z, 14, 0.5, 0.5, 0.5, 0.0);
		Fx.burst(w, ParticleTypes.FLAME, m.x, m.y, m.z, 10, 0.4, 0.4, 0.4, 0.02);
		Fx.burst(w, ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 5, 0.4, 0.4, 0.4, 0.01);
		for (int j = 1; j <= 4; j++) {
			Fx.burst(w, TRAIL, m.x - m.vx * j * 1.2, m.y - m.vy * j * 1.2, m.z - m.vz * j * 1.2, 6, 0.4, 0.4, 0.4, 0.0);
		}
		return false;
	}

	private void impact(Meteor m) {
		Vec3d p = new Vec3d(m.x, m.y, m.z);
		Fx.sound(w, p.x, p.y, p.z, SoundEvents.ENTITY_GENERIC_EXPLODE, 7.0f, 0.75f);
		Fx.burst(w, ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		Fx.burst(w, ParticleTypes.FLAME, p.x, p.y, p.z, 220, 0.3, 0.3, 0.3, 0.55);
		Fx.burst(w, ParticleTypes.LAVA, p.x, p.y, p.z, 40, 0.5, 0.5, 0.5, 0.5);
		Fx.burst(w, ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 80, 0.5, 0.5, 0.5, 0.3);
		Fx.screen(w, p, 100.0, 1.0f, 18, 0.3f, 6, 0xFFA040);
		Fx.blast(w, p, 10.0, 30.0f, 1.4, 6);
		EffectManager.add(new Crater(w, Fx.pos(p), 4.5, 4.5));
	}
}
