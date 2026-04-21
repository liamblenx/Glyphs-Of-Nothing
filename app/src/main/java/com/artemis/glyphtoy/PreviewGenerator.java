package com.artemis.glyphtoy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Generates the toy_preview_image drawable at runtime (for development).
 * In production, export a 200x200 PNG to res/drawable/preview_artemis.png.
 *
 * Call generatePreview() to get a scaled-up Bitmap showing the trajectory.
 */
public class PreviewGenerator {

    private static final int PREVIEW_SIZE = 200;
    private static final int SCALE = PREVIEW_SIZE / ArtemisRenderer.MATRIX_SIZE; // 8

    /**
     * Returns a 200x200 bitmap showing the trajectory at mid-flight,
     * suitable for use as the toy preview image.
     */
    public static Bitmap generatePreview() {
        // Render a frame with the spacecraft near the Moon (waypoint 15)
        int[] frame = ArtemisRenderer.renderFrame(15, 0.75f);

        Bitmap preview = Bitmap.createBitmap(PREVIEW_SIZE, PREVIEW_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(preview);
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint();
        for (int y = 0; y < ArtemisRenderer.MATRIX_SIZE; y++) {
            for (int x = 0; x < ArtemisRenderer.MATRIX_SIZE; x++) {
                int brightness = frame[y * ArtemisRenderer.MATRIX_SIZE + x];
                if (brightness > 0) {
                    paint.setColor(Color.argb(255, brightness, brightness, brightness));
                    canvas.drawRect(
                        x * SCALE, y * SCALE,
                        (x + 1) * SCALE, (y + 1) * SCALE,
                        paint
                    );
                }
            }
        }
        return preview;
    }
}
