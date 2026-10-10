package net.doomsday.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.doomsday.DoomsdayMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class EffectManager {
	private static final List<Effect> ACTIVE = new ArrayList<>();
	private static final List<Effect> PENDING = new ArrayList<>();

	private EffectManager() {}

	public static void add(Effect effect) {
		PENDING.add(effect);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!PENDING.isEmpty()) {
				ACTIVE.addAll(PENDING);
				PENDING.clear();
			}
			Iterator<Effect> it = ACTIVE.iterator();
			while (it.hasNext()) {
				Effect effect = it.next();
				try {
					if (effect.tick()) {
						it.remove();
					}
				} catch (Throwable t) {
					DoomsdayMod.LOGGER.error("Effect crashed and was removed: " + effect.getClass().getSimpleName(), t);
					it.remove();
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			ACTIVE.clear();
			PENDING.clear();
			NukeEffect.clearArmed();
		});
	}
}
