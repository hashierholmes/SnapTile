package com.snaptile.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Standalone toolbar and drawing-canvas helper.
 *
 * This class encapsulates an older/reusable editor implementation: it builds a
 * toolbar, manages paint-mode input, and can flatten recorded strokes onto a bitmap.
 * The current {SnipActivity} editor builds its UI directly, so this helper is
 * kept independent rather than being part of the active capture path.
 */
public class PaintToolbar {

    /** Callback invoked when the toolbar's Save action is selected. */
    public interface SaveListener { void onSave(); }

    private final Context mCtx;
    private final FrameLayout mRoot;       // caller's root layout
    private final SaveListener mSaveListener;

    // Toolbar views
    private LinearLayout mToolbar;
    private View         mPaintBtn;
    private boolean      mPaintMode = false;

    // Drawing canvas
    private DrawingView  mDrawingView;

    // Current paint settings
    private int   mColor     = Color.RED;
    private float mStrokeWidth;

    private static final int[] PALETTE = {
            0xFFE53935, // red
            0xFFFF9800, // orange
            0xFFFFEB3B, // yellow
            0xFF4CAF50, // green
            0xFF2196F3, // blue
            0xFF9C27B0, // purple
            0xFFFFFFFF, // white
            0xFF212121, // black
    };

    public PaintToolbar(Context ctx, FrameLayout root, SaveListener saveListener) {
        mCtx          = ctx;
        mRoot         = root;
        mSaveListener = saveListener;
        mStrokeWidth  = dp(4);
        buildToolbar();
        buildDrawingView();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Shows or hides the toolbar and disables paint input when hidden.
     */
    public void setVisible(boolean visible) {
        mToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            mPaintMode = false;
            mDrawingView.setActive(false);
        }
    }

    /**
     * Flatten drawing strokes onto the given bitmap (in place).
     * Call this before saving.
     */
    public void flattenOnto(Bitmap bmp) {
        mDrawingView.flattenOnto(bmp);
    }

    /** Removes all recorded strokes from the drawing canvas. */
    public void clearDrawing() {
        mDrawingView.clear();
    }

    // -------------------------------------------------------------------------
    // Build toolbar
    // -------------------------------------------------------------------------

    private void buildToolbar() {
        mToolbar = new LinearLayout(mCtx);
        mToolbar.setOrientation(LinearLayout.HORIZONTAL);
        mToolbar.setGravity(Gravity.CENTER_VERTICAL);
        mToolbar.setPadding(dp(8), dp(6), dp(8), dp(6));
        mToolbar.setBackgroundColor(0xF0101820);

        // --- Save button ---
        View saveBtn = makeBtn("Save", true, () -> mSaveListener.onSave());
        mToolbar.addView(saveBtn);

        mToolbar.addView(spacer(dp(8)));

        // --- Undo ---
        View undoBtn = makeIconBtn("<", () -> mDrawingView.undo());
        mToolbar.addView(undoBtn);

        // --- Redo ---
        View redoBtn = makeIconBtn(">", () -> mDrawingView.redo());
        mToolbar.addView(redoBtn);

        mToolbar.addView(spacer(dp(8)));

        // --- Paint toggle ---
        mPaintBtn = makePaintIcon();
        mPaintBtn.setOnClickListener(v -> togglePaint());
        mToolbar.addView(mPaintBtn);

        mToolbar.addView(spacer(dp(8)));

        // --- Color palette (scrollable) ---
        HorizontalScrollView scroll = new HorizontalScrollView(mCtx);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout paletteRow = new LinearLayout(mCtx);
        paletteRow.setOrientation(LinearLayout.HORIZONTAL);
        paletteRow.setGravity(Gravity.CENTER_VERTICAL);
        paletteRow.setPadding(0, 0, dp(4), 0);

        for (int c : PALETTE) {
            View swatch = makeColorSwatch(c);
            paletteRow.addView(swatch);
        }
        scroll.addView(paletteRow);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        scroll.setLayoutParams(scrollLp);
        mToolbar.addView(scroll);

        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        mRoot.addView(mToolbar, tlp);
    }

    // -------------------------------------------------------------------------
    // Build drawing view
    // -------------------------------------------------------------------------

    private void buildDrawingView() {
        mDrawingView = new DrawingView(mCtx);
        mDrawingView.setActive(false);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mRoot.addView(mDrawingView, dlp);
    }

    // -------------------------------------------------------------------------
    // Toggle paint mode
    // -------------------------------------------------------------------------

    private void togglePaint() {
        mPaintMode = !mPaintMode;
        mDrawingView.setActive(mPaintMode);
        // Highlight the paint button when active
        GradientDrawable bg = (GradientDrawable) mPaintBtn.getTag();
        if (bg != null) {
            bg.setColor(mPaintMode ? 0xFF1565C0 : 0xFF263238);
        }
    }

    // -------------------------------------------------------------------------
    // View factories
    // -------------------------------------------------------------------------

    private View makeBtn(String label, boolean primary, Runnable action) {
        android.widget.TextView tv = new android.widget.TextView(mCtx);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(14), dp(7), dp(14), dp(7));
        tv.setClickable(true);
        tv.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(10));
        bg.setColor(primary ? 0xFF1565C0 : 0xFF263238);
        tv.setBackground(bg);
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    private View makeIconBtn(String symbol, Runnable action) {
        android.widget.TextView tv = new android.widget.TextView(mCtx);
        tv.setText(symbol);
        tv.setTextColor(0xFFCFD8DC);
        tv.setTextSize(16);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(10), dp(6), dp(10), dp(6));
        tv.setClickable(true);
        tv.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(8));
        bg.setColor(0xFF263238);
        tv.setBackground(bg);
        tv.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(4), 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** Brush icon: a small custom-drawn view */
    private View makePaintIcon() {
        View v = new View(mCtx) {
            @Override protected void onDraw(Canvas c) {
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float r  = Math.min(cx, cy) - dp(2);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(0xFFCFD8DC);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                // Draw a simple brush shape: circle handle + line
                c.drawCircle(cx, cy - r * 0.2f, r * 0.55f, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(mColor);
                c.drawCircle(cx, cy - r * 0.2f, r * 0.35f, p);
                p.setColor(0xFFCFD8DC);
                p.setStrokeWidth(dp(2.5f));
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx, cy + r * 0.3f, cx, cy + r, p);
            }
        };
        int sz = dp(36);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.setMargins(0, 0, 0, 0);
        v.setLayoutParams(lp);
        v.setClickable(true);
        v.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(8));
        bg.setColor(0xFF263238);
        v.setBackground(bg);
        v.setTag(bg);
        return v;
    }

    private View makeColorSwatch(int color) {
        View v = new View(mCtx) {
            @Override protected void onDraw(Canvas c) {
                float r = Math.min(getWidth(), getHeight()) / 2f - dp(2);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(color);
                c.drawCircle(getWidth() / 2f, getHeight() / 2f, r, p);
                if (color == mColor) {
                    p.setColor(Color.WHITE);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(dp(2));
                    c.drawCircle(getWidth() / 2f, getHeight() / 2f, r - dp(1), p);
                }
            }
        };
        int sz = dp(28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.setMargins(dp(3), 0, dp(3), 0);
        v.setLayoutParams(lp);
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(view -> {
            mColor = color;
            mDrawingView.setColor(color);
            // Redraw all swatches to show new selection ring
            ViewGroup row = (ViewGroup) view.getParent();
            for (int i = 0; i < row.getChildCount(); i++) row.getChildAt(i).invalidate();
            mPaintBtn.invalidate();
        });
        return v;
    }

    private View spacer(int w) {
        View v = new View(mCtx);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
        return v;
    }

    private int dp(int v)   { return Math.round(v * mCtx.getResources().getDisplayMetrics().density); }
    private int dp(float v) { return Math.round(v * mCtx.getResources().getDisplayMetrics().density); }

    // =========================================================================
    // DrawingView — the transparent paint canvas
    // =========================================================================

    static class DrawingView extends View {

        private boolean mActive = false;

        // Current stroke being drawn
        private Path  mCurrentPath  = null;
        private Paint mCurrentPaint = null;

        // Undo / Redo stacks: each entry is (path, paint snapshot)
        private static class Stroke {
            Path  path;
            Paint paint;
            Stroke(Path p, Paint pt) { path = p; paint = pt; }
        }
        private final Deque<Stroke> mUndoStack = new ArrayDeque<>();
        private final Deque<Stroke> mRedoStack = new ArrayDeque<>();

        private int   mColor       = Color.RED;
        private float mStrokeWidth = 8f;

        DrawingView(Context ctx) {
            super(ctx);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            setBackgroundColor(Color.TRANSPARENT);
        }

        void setActive(boolean active) {
            mActive = active;
            // When inactive, pass touches through
            setClickable(active);
            setFocusable(active);
        }

        void setColor(int color)       { mColor = color; }
        void setStrokeWidth(float w)   { mStrokeWidth = w; }

        void undo() {
            if (!mUndoStack.isEmpty()) {
                mRedoStack.push(mUndoStack.pop());
                invalidate();
            }
        }

        void redo() {
            if (!mRedoStack.isEmpty()) {
                mUndoStack.push(mRedoStack.pop());
                invalidate();
            }
        }

        void clear() {
            mUndoStack.clear();
            mRedoStack.clear();
            mCurrentPath = null;
            invalidate();
        }

        /** Draw all strokes onto the given bitmap (used before saving). */
        void flattenOnto(Bitmap bmp) {
            Canvas c = new Canvas(bmp);
            // Scale strokes from view coords to bitmap coords
            float sx = (float) bmp.getWidth()  / Math.max(1, getWidth());
            float sy = (float) bmp.getHeight() / Math.max(1, getHeight());
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(sx, sy);
            for (Stroke s : mUndoStack) {
                Path scaled = new Path();
                s.path.transform(m, scaled);
                Paint sp = new Paint(s.paint);
                sp.setStrokeWidth(s.paint.getStrokeWidth() * ((sx + sy) / 2f));
                c.drawPath(scaled, sp);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Draw committed strokes
            for (Stroke s : mUndoStack) {
                canvas.drawPath(s.path, s.paint);
            }
            // Draw in-progress stroke
            if (mCurrentPath != null && mCurrentPaint != null) {
                canvas.drawPath(mCurrentPath, mCurrentPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!mActive) return false;
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mCurrentPath = new Path();
                    mCurrentPath.moveTo(x, y);
                    mCurrentPaint = makePaint();
                    mRedoStack.clear(); // new stroke clears redo
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mCurrentPath != null) {
                        mCurrentPath.lineTo(x, y);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mCurrentPath != null) {
                        mUndoStack.push(new Stroke(mCurrentPath, mCurrentPaint));
                        mCurrentPath  = null;
                        mCurrentPaint = null;
                        invalidate();
                    }
                    return true;
            }
            return false;
        }

        private Paint makePaint() {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(mColor);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(mStrokeWidth);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            return p;
        }
    }
}
