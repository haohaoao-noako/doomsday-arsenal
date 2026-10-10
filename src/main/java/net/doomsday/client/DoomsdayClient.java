package net.doomsday.client;

import java.util.Random;

import net.doomsday.DoomsdayMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/** Camera shake and full-screen flash, driven by a packet from the server. */
@Environment(EnvType.CLIENT)
public class DoomsdayClient implements ClientModInitializer {
	private static final Random RNG = new Random();

	private static float shakeAmp;
	private static int shakeTicks;
	private static int shakeTotal = 1;

	private static float flashPeak;
	private static int flashTicks;
	private static int flashTotal = 1;
	private static int flashRgb = 0xFFFFFF;

	private static float lastYawOffset;
	private static float lastPitchOffset;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DoomsdayMod.SCREEN_FX, (client, handler, buf, sender) -> {
			float shake = buf.readFloat();
			int shakeLen = buf.readInt();
			float flash = buf.readFloat();
			int flashLen = buf.readInt();
			int rgb = buf.readInt();
			client.execute(() -> {
				float remaining = shakeAmp * shakeTicks / (float) Math.max(1, shakeTotal);
				if (shake > 0.0f && shakeLen > 0 && shake >= remaining) {
					shakeAmp = shake;
					shakeTicks = shakeLen;
					shakeTotal = shakeLen;
				}
				if (flash > 0.0f && flashLen > 0) {
					flashPeak = flash;
					flashTicks = flashLen;
					flashTotal = flashLen;
					flashRgb = rgb;
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(DoomsdayClient::onTick);
		HudRenderCallback.EVENT.register(DoomsdayClient::onHud);
	}

	private static void onTick(MinecraftClient mc) {
		ClientPlayerEntity player = mc.player;
		if (player == null) {
			shakeTicks = 0;
			flashTicks = 0;
			lastYawOffset = 0.0f;
			lastPitchOffset = 0.0f;
			return;
		}

		// undo last tick's shake so the view always returns to where the player is really looking
		if (lastYawOffset != 0.0f || lastPitchOffset != 0.0f) {
			player.setYaw(player.getYaw() - lastYawOffset);
			player.setPitch(player.getPitch() - lastPitchOffset);
			lastYawOffset = 0.0f;
			lastPitchOffset = 0.0f;
		}

		if (shakeTicks > 0 && !mc.isPaused()) {
			float k = shakeAmp * shakeTicks / (float) shakeTotal;
			lastYawOffset = (RNG.nextFloat() - 0.5f) * 2.0f * k;
			float wantedPitch = player.getPitch() + (RNG.nextFloat() - 0.5f) * 1.4f * k;
			float newPitch = MathHelper.clamp(wantedPitch, -90.0f, 90.0f);
			lastPitchOffset = newPitch - player.getPitch();
			player.setYaw(player.getYaw() + lastYawOffset);
			player.setPitch(newPitch);
			shakeTicks--;
		}

		if (flashTicks > 0) {
			flashTicks--;
		}
	}

	private static void onHud(DrawContext context, float tickDelta) {
		if (flashTicks <= 0) {
			return;
		}
		float t = Math.max(0.0f, (flashTicks - tickDelta) / flashTotal);
		float a = flashPeak * t * t;
		int alpha = (int) (255.0f * Math.min(1.0f, a));
		if (alpha <= 2) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		context.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
				(alpha << 24) | (flashRgb & 0xFFFFFF));
	}
}
