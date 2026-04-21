package com.artemis.glyphtoy;

/**
 * Maps real-world mission elapsed time to a trajectory waypoint index.
 *
 * Artemis II approximate timeline (total ~10 days):
 *   T+0h        Launch
 *   T+0–24h     Earth orbit checkout
 *   T+24h       Trans-Lunar Injection
 *   T+24–120h   Outbound coast (4 days)
 *   T+120h      Lunar flyby
 *   T+120–216h  Return coast (4 days)
 *   T+216–240h  Re-entry & splashdown
 *
 * This class linearly maps elapsed mission hours to waypoint indices along
 * the trajectory. When no mission is active it returns -1 and the service
 * falls back to a looping demo animation.
 */
public class MissionTracker {

    // ── Mission parameters (edit these per-mission) ────────────────────

    /** Set to 0 when no mission is scheduled. */
    private static final long LAUNCH_EPOCH_MILLIS = 0L;

    /** Total planned mission duration in hours. */
    private static final double MISSION_DURATION_HOURS = 240.0;

    // ── Phase boundaries (fraction of total duration) ──────────────────

    /** Earth-orbit phase ends, TLI happens. */
    private static final double PHASE_TLI = 0.10;         // T+24h / 240h

    /** Outbound coast ends, lunar flyby begins. */
    private static final double PHASE_FLYBY_START = 0.50;  // T+120h / 240h

    /** Flyby phase ends, return coast begins. */
    private static final double PHASE_FLYBY_END = 0.55;

    /** Return coast ends, re-entry begins. */
    private static final double PHASE_REENTRY = 0.90;

    // ── Waypoint ranges (indices into ArtemisRenderer.TRAJECTORY) ──────
    // These map trajectory segments to the phase boundaries above.

    /** Waypoints 0–1: departing Earth vicinity. */
    private static final int WP_DEPART_START = 0;
    private static final int WP_DEPART_END   = 1;

    /** Waypoints 2–14: outbound coast to Moon approach. */
    private static final int WP_OUTBOUND_START = 2;
    private static final int WP_OUTBOUND_END   = 14;

    /** Waypoints 15–20: lunar flyby (around far side). */
    private static final int WP_FLYBY_START = 15;
    private static final int WP_FLYBY_END   = 20;

    /** Waypoints 21–34: return coast back to Earth. */
    private static final int WP_RETURN_START = 21;
    private static final int WP_RETURN_END   = 34;

    /**
     * Returns the current waypoint index based on real elapsed mission time,
     * or -1 if no mission is active (LAUNCH_EPOCH_MILLIS == 0 or mission
     * hasn't started yet or has already ended).
     */
    public static int getCurrentWaypoint() {
        if (LAUNCH_EPOCH_MILLIS == 0L) return -1;

        long now = System.currentTimeMillis();
        double elapsedHours = (now - LAUNCH_EPOCH_MILLIS) / 3_600_000.0;
        if (elapsedHours < 0 || elapsedHours > MISSION_DURATION_HOURS) return -1;

        double progress = elapsedHours / MISSION_DURATION_HOURS;

        if (progress < PHASE_TLI) {
            return lerp(WP_DEPART_START, WP_DEPART_END, progress / PHASE_TLI);
        } else if (progress < PHASE_FLYBY_START) {
            double t = (progress - PHASE_TLI) / (PHASE_FLYBY_START - PHASE_TLI);
            return lerp(WP_OUTBOUND_START, WP_OUTBOUND_END, t);
        } else if (progress < PHASE_FLYBY_END) {
            double t = (progress - PHASE_FLYBY_START) / (PHASE_FLYBY_END - PHASE_FLYBY_START);
            return lerp(WP_FLYBY_START, WP_FLYBY_END, t);
        } else if (progress < PHASE_REENTRY) {
            double t = (progress - PHASE_FLYBY_END) / (PHASE_REENTRY - PHASE_FLYBY_END);
            return lerp(WP_RETURN_START, WP_RETURN_END, t);
        } else {
            return WP_RETURN_END;
        }
    }

    /**
     * Returns true when a live mission is configured and currently in progress.
     */
    public static boolean isMissionActive() {
        return getCurrentWaypoint() >= 0;
    }

    private static int lerp(int a, int b, double t) {
        return a + (int) Math.round((b - a) * Math.min(1.0, Math.max(0.0, t)));
    }
}
