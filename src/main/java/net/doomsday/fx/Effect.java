package net.doomsday.fx;

/** A server-side, tick-driven effect (no entities needed). */
public interface Effect {
	/** @return true when the effect is finished and should be removed. */
	boolean tick();
}
