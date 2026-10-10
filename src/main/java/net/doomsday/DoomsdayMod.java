package net.doomsday;

import net.doomsday.fx.EffectManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoomsdayMod implements ModInitializer {
	public static final String MOD_ID = "doomsday";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Server -> client packet: screen shake + flash. */
	public static final Identifier SCREEN_FX = new Identifier(MOD_ID, "screen_fx");

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModRegistry.init();
		EffectManager.init();
		LOGGER.info("Doomsday Arsenal loaded - handle with care.");
	}
}
