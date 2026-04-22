package com.artemis.glyphtoy;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphMatrixManager;
import com.nothing.ketchum.GlyphToy;

/**
 * Glyph Toy service that displays the Artemis mission trajectory.
 *
 * Behaviour:
 *   - On bind: starts an animation loop that advances the spacecraft dot
 *     along the trajectory and pushes frames to the Glyph Matrix.
 *   - If a real mission epoch is set in MissionTracker, the dot tracks
 *     actual mission elapsed time.
 *   - Otherwise it runs a smooth demo loop.
 *   - Long-press (EVENT_CHANGE) pauses / resumes the animation.
 *   - EVENT_AOD renders a single static frame (current position, no pulse).
 */
public class ArtemisGlyphToyService extends Service {

    private static final String TAG = "ArtemisGlyph";

    // Demo animation timing
    private static final long FRAME_INTERVAL_MS = 120;   // ~8 fps for pulse
    private static final long STEP_INTERVAL_MS  = 2000;  // move dot every 2 s

    private GlyphMatrixManager mGM;
    private Handler mAnimHandler;
    private boolean mBound = false;
    private boolean mPaused = false;

    // Animation state
    private int   mCraftIndex = 0;
    private long  mLastStepTime = 0;
    private long  mPulseTime = 0;

    // ── Service lifecycle ──────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        mGM = GlyphMatrixManager.getInstance(this);
        mGM.init(new GlyphMatrixManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName name) {
                mGM.register(Glyph.DEVICE_23112); // Phone (3)
                Log.i(TAG, "GlyphMatrix service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w(TAG, "GlyphMatrix service disconnected");
            }
        });
        mAnimHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        stopAnimation();
        mGM.unInit();
        super.onDestroy();
    }

    // ── Toy binding (start / stop animation) ───────────────────────────

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        mPaused = false;
        mCraftIndex = 0;
        mLastStepTime = System.currentTimeMillis();
        mPulseTime = System.currentTimeMillis();
        startAnimation();
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        stopAnimation();
        return false;
    }

    // ── Event handling ─────────────────────────────────────────────────

    private final Messenger mMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                String event = msg.getData().getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                if (event == null) return;

                switch (event) {
                    case GlyphToy.EVENT_CHANGE:
                        // Long-press toggles pause
                        mPaused = !mPaused;
                        Log.i(TAG, "Animation " + (mPaused ? "paused" : "resumed"));
                        break;

                    case GlyphToy.EVENT_AOD:
                        // Always-on display: render a single static frame
                        renderStaticFrame();
                        break;

                    case GlyphToy.EVENT_ACTION_DOWN:
                        // Could add touch interaction later
                        break;

                    case GlyphToy.EVENT_ACTION_UP:
                        break;
                }
            }
        }
    });

    // ── Animation loop ─────────────────────────────────────────────────

    private final Runnable mAnimRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mBound) return;

            long now = System.currentTimeMillis();

            if (!mPaused) {
                // Advance spacecraft position
                if (MissionTracker.isMissionActive()) {
                    // Real-time mission tracking
                    mCraftIndex = MissionTracker.getCurrentWaypoint();
                } else {
                    // Demo mode: step through waypoints in a loop
                    if (now - mLastStepTime >= STEP_INTERVAL_MS) {
                        mCraftIndex = (mCraftIndex + 1) % ArtemisRenderer.getWaypointCount();
                        mLastStepTime = now;
                    }
                }
            }

            // Pulse phase: cycles 0..1 over ~1.5 seconds
            float pulsePhase = ((now - mPulseTime) % 1500) / 1500f;

            // Render and push frame (scaled to hardware 0–4095 brightness range)
            int[] frame = ArtemisRenderer.renderHardwareFrame(mCraftIndex, pulsePhase);
            try {
                mGM.setMatrixFrame(frame);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set matrix frame", e);
            }

            // Schedule next frame
            mAnimHandler.postDelayed(this, FRAME_INTERVAL_MS);
        }
    };

    private void startAnimation() {
        mAnimHandler.post(mAnimRunnable);
    }

    private void stopAnimation() {
        mAnimHandler.removeCallbacks(mAnimRunnable);
    }

    private void renderStaticFrame() {
        int waypointIndex = MissionTracker.isMissionActive()
                ? MissionTracker.getCurrentWaypoint()
                : mCraftIndex;
        int[] frame = ArtemisRenderer.renderHardwareFrame(waypointIndex, 0.75f);
        try {
            mGM.setMatrixFrame(frame);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set AOD frame", e);
        }
    }
}
