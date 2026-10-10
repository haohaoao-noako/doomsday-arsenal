package net.doomsday.fx;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** A black thunderstorm hangs over the target and hurls lightning at the ground and at anything alive. */
public final class ThorEffect implements Effect {
	private static final int DURATION = 160;
	private static final double AREA = 24.0;

	private static final ParticleEffect STORM = Fx.dustFade(0.25f, 0.25f, 0.32f, 0.08f, 0.08f, 0.12f, 4.0f);
	private static final ParticleEffect GLOW = Fx.dust(0.6f, 0.8f, 1.0f, 2.0f);

	private final ServerWorld w;
	private final Vec3d c;
	private int age = 0;

	public ThorEffect(ServerWorld world, Vec3d target) {
		this.w = world;
		this.c = target;
	}

	@Override
	public boolean tick() {
		if (age == 0) {
			Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 20.0f, 0.7f);
			Fx.actionbar(w, c, 96.0, Text.translatable("message.doomsday.thor").formatted(Formatting.AQUA, Formatting.BOLD));
			Fx.screen(w, c, 120.0, 0.8f, 40, 0.5f, 10, 0xBFD8FF);
		}

		if (age < DURATION - 20) {
			double cy = c.y + 42.0;
			Fx.burst(w, STORM, c.x, cy, c.z, 60, AREA * 0.7, 2.5, AREA * 0.7, 0.0);
			Fx.burst(w, ParticleTypes.CLOUD, c.x, cy - 1.0, c.z, 20, AREA * 0.6, 1.5, AREA * 0.6, 0.0);
			Fx.burst(w, ParticleTypes.ELECTRIC_SPARK, c.x, cy - 2.0, c.z, 12, AREA * 0.6, 2.0, AREA * 0.6, 0.4);
		}

		if (age < 30) {
			Fx.ring(w, GLOW, c.x, c.y + 0.2, c.z, age * 0.8, 36, age * 0.1, 0.0, 0.0);
		}

		if (age == 15) {
			megaStrike();
		}
		if (age > 15 && age < DURATION - 20) {
			if (age % 3 == 0) {
				double a = Fx.RNG.nextDouble() * Math.PI * 2.0;
				double d = Math.sqrt(Fx.RNG.nextDouble()) * AREA;
				strike(c.x + Math.cos(a) * d, c.z + Math.sin(a) * d, Fx.RNG.nextDouble() < 0.4);
			}
			if (age % 12 == 0) {
				homing();
			}
		}
		age++;
		return age > DURATION;
	}

	private void strike(double x, double z, boolean cosmetic) {
		double gy = Fx.groundY(w, x, z, c.y);
		Fx.bolt(w, x, gy, z, cosmetic);
		Fx.burst(w, ParticleTypes.ELECTRIC_SPARK, x, gy + 0.5, z, 40, 0.4, 0.4, 0.4, 0.6);
		Fx.burst(w, ParticleTypes.END_ROD, x, gy + 0.5, z, 12, 0.3, 0.3, 0.3, 0.25);
	}

	private void homing() {
		Box box = new Box(c.x - AREA, c.y - 20.0, c.z - AREA, c.x + AREA, c.y + 40.0, c.z + AREA);
		List<Entity> targets = w.getOtherEntities(null, box, e -> e instanceof LivingEntity);
		if (targets.isEmpty()) {
			return;
		}
		Entity e = targets.get(Fx.RNG.nextInt(targets.size()));
		Fx.bolt(w, e.getX(), e.getY(), e.getZ(), false);
	}

	private void megaStrike() {
		Fx.bolt(w, c.x, c.y, c.z, false);
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2.0 + 0.4;
			Fx.bolt(w, c.x + Math.cos(a) * 4.0, c.y, c.z + Math.sin(a) * 4.0, true);
		}
		Fx.screen(w, c, 150.0, 2.0f, 30, 0.9f, 12, 0xDDEEFF);
		Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, 8.0f, 1.2f);
		Fx.burst(w, ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.0, c.z, 400, 0.3, 0.3, 0.3, 1.0);
		Fx.burst(w, ParticleTypes.END_ROD, c.x, c.y + 1.0, c.z, 200, 0.3, 0.3, 0.3, 0.7);
		Fx.burst(w, ParticleTypes.FLASH, c.x, c.y + 1.0, c.z, 2, 0.5, 0.5, 0.5, 0.0);
		Fx.blast(w, c, 9.0, 40.0f, 2.0, 4);
		EffectManager.add(new Crater(w, Fx.pos(c.x, c.y - 1.0, c.z), 3.5, 3.5));
	}
}
