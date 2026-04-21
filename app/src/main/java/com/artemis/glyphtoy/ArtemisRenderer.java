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

    private static final int B_PATH = 50;
    private static final int B_CRAFT_MAX = 255;
    private static final int B_CRAFT_MIN = 140;

    // ── Earth (top-left, center ~5,5, radius ~3.5) ────────────────
    // [x, y, brightness] — varying brightness for land/ocean/limb effect
    private static final int[][] EARTH = {
        // Row 1 (top edge — polar ice hint)
        {4,3,180}, {5,3,180}, {6,3,255}, {7,3,255},
        // Row 2 (polar ice hint)
        {3,4,180}, {4,4,180}, {5,4,180}, {6,4,180}, {7,4,255}, {8,4,255},
        // Row 3 (land mass upper)
        {3,5,180}, {4,5,180}, {5,5,180}, {6,5,180}, {7,5,180}, {8,5,255},
        // Row 4 (land mass lower)
        {3,6,255}, {4,6,180}, {5,6,180}, {6,6,180}, {7,6,180}, {8,6,180},
        // Row 5
        {3,7,255}, {4,7,255}, {5,7,200}, {6,7,200}, {7,7,200}, {8,7,200},
        // Row 6 (bottom edge)
        {4,8,255}, {5,8,255}, {6,8,180}, {7,8,180},
    };

    // ── Moon (bottom-right, center ~20,20, radius ~2.5) ───────────
    // Darker overall with crater variation
    private static final int[][] MOON = {
        // Row 18
        {19,18,50}, {20,18,60}, {21,18,55},
        // Row 19
        {18,19,40}, {19,19,75}, {20,19,100}, {21,19,80}, {22,19,55},
        // Row 20
        {18,20,55}, {19,20,95}, {20,20,110}, {21,20,90}, {22,20,65},
        // Row 21
        {18,21,45}, {19,21,85}, {20,21,95}, {21,21,105}, {22,21,50},
        // Row 22
        {19,22,45}, {20,22,55}, {21,22,50},
    };

    // ── Trajectory waypoints (ordered path for spacecraft) ────────
    private static final int[][] TRAJECTORY = {
        // Earth Slingshot Rotation 1
        {4, 1},
        {2, 2},
        {1, 4},
        {1, 6},
        {2, 8},
        {4, 10},
        {7, 10},
        {9, 8},
        {10, 6},
        {10, 4},
        {9, 2},
        {7, 1},
        // Earth Slingshot Rotation 2
        {4, 1},
        {2, 2},
        {1, 4},
        {1, 6},
        {2, 8},
        {4, 10},
        // Depart Earth Slingshot
        {9, 12},
        {14, 14},
        {19, 16},
        // Arrive Moon
        {23, 18},
        {24, 20},
        {24, 22},
        {22, 24},
        {20, 24},
        {18, 23},
        {16, 19},
        // Depart Moon
        {13, 10},
        {12, 6},
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
