package com.rocket.radar.loadingscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * A custom View that draws a rotating radar-like animation.
 *
 * <p>This view is the visual core of the loading screen. It uses a {@link SweepGradient}
 * and a rotating matrix to simulate a radar sweeping clockwise. It includes logic for
 * smoothly fading in the grid and the sweep arm when the animation starts.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>The color and speed constants are hardcoded; these should ideally be exposed as custom XML attributes
 *       to allow for easier theming without modifying the Java code.</li>
 *   <li>The `onDraw` method performs object allocations (e.g., implicitly in some paint operations) or matrix updates
 *       on every frame, which might impact performance on very low-end devices, though currently acceptable.</li>
 * </ul>
 * </p>
 */
public class RadarView extends View {

    private Paint gridPaint;
    private Paint sweepPaint;
    private float rotation = 200;
    private Matrix matrix;
    private int height;
    private float maxRadius;
    private boolean isAnimating = false;

    private int currentAlpha = 0;
    private static final int TARGET_ALPHA = 255;
    private static final int FADE_SPEED = 10; // How fast it fades in (higher = faster)

    // Configuration
    private static final int RADAR_COLOR = Color.parseColor("#000000"); // Neon Green
    private static final float ANIMATION_SPEED = 1.75f; // Scan speed

    /**
     * Simple constructor to use when creating a RadarView from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public RadarView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor that is called when inflating a view from XML.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute.
     *
     * @param context      The Context the view is running in, through which it can
     *                     access the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     */
    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(RADAR_COLOR);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3f);
        gridPaint.setAlpha(0);

        sweepPaint = new Paint();
        sweepPaint.setStyle(Paint.Style.FILL);
        sweepPaint.setAlpha(0);

        matrix = new Matrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
        maxRadius = (float) Math.sqrt(w * w + h * h);

        // --- CHANGE 1: Shorten the Trail ---
        // Instead of fading from 0.0 to 1.0, we keep it transparent until 0.85
        // This makes the "tail" much shorter and sharper.
        int[] colors = {Color.TRANSPARENT, Color.TRANSPARENT, RADAR_COLOR};
        float[] positions = {0.0f, 0.85f, 1.0f}; // 0.0 -> 0.85 is pure transparent

        SweepGradient shader = new SweepGradient(0, height, colors, positions);
        sweepPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 2. Handle Fade-In Logic
        if (isAnimating && currentAlpha < TARGET_ALPHA) {
            currentAlpha += FADE_SPEED;
            if (currentAlpha > TARGET_ALPHA) currentAlpha = TARGET_ALPHA;

            // Update paint alphas
            // Grid is semi-transparent at max (80), so we scale it relative to currentAlpha
            gridPaint.setAlpha((int) (currentAlpha * (80.0f / 255.0f)));
            sweepPaint.setAlpha(currentAlpha);
        }

        // 1. Draw Grid Lines
        float step = maxRadius / 4;
        for (int i = 1; i <= 4; i++) {
            canvas.drawCircle(0, height, step * i, gridPaint);
        }

        // 2. Draw the Rotating Sweep
        matrix.setRotate(rotation, 0, height);
        sweepPaint.getShader().setLocalMatrix(matrix);
        canvas.drawCircle(0, height, maxRadius, sweepPaint);

        // 3. Animation Loop
        if (isAnimating) {
            // --- CHANGE 2: Clockwise Rotation ---
            rotation += ANIMATION_SPEED; // += makes it go Clockwise

            if (rotation >= 360) {
                rotation = 0;
            }
            invalidate();
        }
    }

    /**
     * Starts the radar animation.
     * Resets the rotation and alpha values to their initial states and triggers the invalidation loop.
     */
    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            rotation = 185;
            currentAlpha = 0;
            setVisibility(VISIBLE);
            invalidate();
        }
    }

    /**
     * Stops the radar animation.
     * This stops the invalidation loop, effectively pausing the view's drawing cycle.
     */
    public void stopAnimation() {
        isAnimating = false;
    }
}
