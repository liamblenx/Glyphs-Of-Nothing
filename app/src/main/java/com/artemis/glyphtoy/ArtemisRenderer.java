package com.artemis.glyphtoy;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Renders the Artemis mission trajectory on a 25x25 LED matrix.
 *
 * Layout:
 *   - Earth: top-left (center ~5,5) with varying brightness for a realistic look
 *   - Moon:  bottom-right (center ~20,20) darker with crater texture
 *   - Outbound path curves above the diagonal from Earth to Moon
 *   - Return path curves below the diagonal from Moon back to Earth
 *   - One bright/pulsing dot marks the current spacecraft position
 */
public class ArtemisRenderer {

    public static final int MATRIX_SIZE = 25;

    // Glyph Matrix hardware uses 0–4095 per pixel; authoring values are 0–255
    // (matching simulator.html), so scale by 16 before calling setMatrixFrame(int[]).
    // The Bitmap path (GlyphMatrixObject) applies this same multiplier internally.
    private static final int HARDWARE_BRIGHTNESS_MULTIPLIER = 16;
    private static final int HARDWARE_MAX_BRIGHTNESS = 4095;

    private static final int B_PATH = 50;
    private static final int B_CRAFT_MAX = 255;
    private static final int B_CRAFT_MIN = 140;

    // The Phone (3) Glyph Matrix is a 25x25 grid masked to an inscribed disc
    // (cells whose centre is within ~12.5 of (12,12) are lit). All art must stay
    // inside that disc; corner cells never light up on hardware.

    // ── Earth (top-left, center ~6.5,6.5) ─────────────────────────
    // [x, y, brightness] — varying brightness for land/ocean/limb effect
    private static final int[][] EARTH = {
        // Row 4 (top edge — polar ice hint)
        {5,4,180}, {6,4,180}, {7,4,255}, {8,4,255},
        // Row 5 (polar ice hint)
        {4,5,180}, {5,5,180}, {6,5,180}, {7,5,180}, {8,5,255}, {9,5,255},
        // Row 6 (land mass upper)
        {4,6,180}, {5,6,180}, {6,6,180}, {7,6,180}, {8,6,180}, {9,6,255},
        // Row 7 (land mass lower)
        {4,7,255}, {5,7,180}, {6,7,180}, {7,7,180}, {8,7,180}, {9,7,180},
        // Row 8
        {4,8,255}, {5,8,255}, {6,8,200}, {7,8,200}, {8,8,200}, {9,8,200},
        // Row 9 (bottom edge)
        {5,9,255}, {6,9,255}, {7,9,180}, {8,9,180},
    };

    // ── Moon (bottom-right, center ~18,18) ────────────────────────
    // Darker overall with crater variation
    private static final int[][] MOON = {
        // Row 16
        {17,16,50}, {18,16,60}, {19,16,55},
        // Row 17
        {16,17,40}, {17,17,75}, {18,17,100}, {19,17,80}, {20,17,55},
        // Row 18
        {16,18,55}, {17,18,95}, {18,18,110}, {19,18,90}, {20,18,65},
        // Row 19
        {16,19,45}, {17,19,85}, {18,19,95}, {19,19,105}, {20,19,50},
        // Row 20
        {17,20,45}, {18,20,55}, {19,20,50},
    };

    // ── Trajectory waypoints (ordered path for spacecraft) ────────
    private static final int[][] TRAJECTORY = {
        // Earth Slingshot Rotation 1
        {5, 2},
        {4, 3},
        {2, 5},
        {2, 7},
        {3, 9},
        {5, 11},
        {8, 11},
        {10, 9},
        {11, 7},
        {11, 5},
        {10, 3},
        {8, 2},
        // Earth Slingshot Rotation 2
        {5, 2},
        {4, 3},
        {2, 5},
        {2, 7},
        {3, 9},
        {5, 11},
        // Depart Earth Slingshot
        {7, 12},
        {10, 14},
        {13, 15},
        // Arrive Moon
        {21, 16},
        {22, 18},
        {21, 20},
        {20, 21},
        {18, 21},
        {16, 20},
        {14, 16},
        // Depart Moon
        {11, 12},
        {10, 8},
    };

    public static int getWaypointCount() {
        return TRAJECTORY.length;
    }

    /**
     * Render one frame as a flat int[625] brightness array.
     *
     * @param craftIndex  which waypoint the spacecraft is at (0 .. getWaypointCount()-1)
     * @param pulsePhase  0.0 – 1.0 controls the brightness pulse of the craft dot
     * @return int[625] row-major brightness values for setMatrixFrame()
     */
    public static int[] renderFrame(int craftIndex, float pulsePhase) {
        int[] frame = new int[MATRIX_SIZE * MATRIX_SIZE];

        // 1. Draw Earth (per-pixel brightness)
        for (int[] p : EARTH) {
            frame[p[1] * MATRIX_SIZE + p[0]] = p[2];
        }

        // 2. Draw Moon (per-pixel brightness)
        for (int[] p : MOON) {
            frame[p[1] * MATRIX_SIZE + p[0]] = p[2];
        }

        // 3. Draw trajectory + spacecraft
        for (int i = 0; i < TRAJECTORY.length; i++) {
            int x = TRAJECTORY[i][0];
            int y = TRAJECTORY[i][1];
            int idx = y * MATRIX_SIZE + x;

            if (i == craftIndex) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(pulsePhase * 2 * Math.PI));
                int brightness = (int) (B_CRAFT_MIN + pulse * (B_CRAFT_MAX - B_CRAFT_MIN));
                frame[idx] = brightness;
            } else {
                frame[idx] = B_PATH;
            }
        }

        return frame;
    }

    /**
     * Render one frame scaled for the Glyph Matrix hardware (0–4095 per pixel).
     * Use this for {@code GlyphMatrixManager.setMatrixFrame(int[])}.
     */
    public static int[] renderHardwareFrame(int craftIndex, float pulsePhase) {
        int[] frame = renderFrame(craftIndex, pulsePhase);
        for (int i = 0; i < frame.length; i++) {
            int scaled = frame[i] * HARDWARE_BRIGHTNESS_MULTIPLIER;
            frame[i] = scaled > HARDWARE_MAX_BRIGHTNESS ? HARDWARE_MAX_BRIGHTNESS : scaled;
        }
        return frame;
    }

    /**
     * Render one frame as a 25x25 Bitmap for GlyphMatrixObject.setImageSource().
     */
    public static Bitmap renderBitmap(int craftIndex, float pulsePhase) {
        int[] frame = renderFrame(craftIndex, pulsePhase);
        Bitmap bmp = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < MATRIX_SIZE; y++) {
            for (int x = 0; x < MATRIX_SIZE; x++) {
                int b = frame[y * MATRIX_SIZE + x];
                bmp.setPixel(x, y, b > 0 ? Color.argb(255, b, b, b) : Color.TRANSPARENT);
            }
        }
        return bmp;
    }

    public static int[] getWaypoint(int index) {
        return TRAJECTORY[index];
    }
}
