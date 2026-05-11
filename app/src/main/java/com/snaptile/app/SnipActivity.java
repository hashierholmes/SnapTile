package com.snaptile.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SnipActivity extends Activity {

    public static final String EXTRA_FULL_PAINT = "full_paint";

    private boolean     mFullPaint;
    private SnipView    mSnipView;
    private View        mSnipSaveBtn;
    private PaintCanvas mPaintCanvas;
    private Bitmap      mEditBitmap;

    // Brush size cycle: 3 steps
    private static final float[] BRUSH_SIZES   = {4f, 10f, 22f};
    private static final int[]   BRUSH_DRAWABLES = {
        R.drawable.ic_brush_sm, R.drawable.ic_brush_md, R.drawable.ic_brush_lg
    };
    private int mBrushIdx = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CaptureActivity.pendingBitmap == null || CaptureActivity.pendingBitmap.isRecycled()) {
            Toast.makeText(this, "No screenshot available", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        mFullPaint = getIntent().getBooleanExtra(EXTRA_FULL_PAINT, false);
        setupFullscreen();

        FrameLayout root = new FrameLayout(this);

        if (mFullPaint) {
            mEditBitmap = CaptureActivity.pendingBitmap.copy(Bitmap.Config.ARGB_8888, true);
            CaptureActivity.pendingBitmap.recycle();
            CaptureActivity.pendingBitmap = null;
            buildPaintPhase(root);
        } else {
            buildSnipPhase(root);
        }

        setContentView(root);
    }

    // =========================================================================
    // Snip phase
    // =========================================================================

    private void buildSnipPhase(FrameLayout root) {
        mSnipView = new SnipView(this, CaptureActivity.pendingBitmap);
        root.addView(mSnipView, mp());

        mSnipSaveBtn = makeIconBtn(R.drawable.ic_save, 0xCCFFFFFF, 0xFF1D4ED8, () -> confirmSnip(root));
        mSnipSaveBtn.setVisibility(View.GONE);

        FrameLayout.LayoutParams saveLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        saveLp.setMargins(dp(14), dp(52), 0, 0);
        root.addView(mSnipSaveBtn, saveLp);

        mSnipView.setOnSelectionChangedListener(has ->
                mSnipSaveBtn.setVisibility(has ? View.VISIBLE : View.GONE));
    }

    private void confirmSnip(FrameLayout root) {
        Rect sel = mSnipView.getSelection();
        Bitmap src = CaptureActivity.pendingBitmap;
        if (sel == null || src == null) return;

        int l = clamp(sel.left,   0, src.getWidth()  - 1);
        int t = clamp(sel.top,    0, src.getHeight() - 1);
        int r = clamp(sel.right,  l + 1, src.getWidth());
        int b = clamp(sel.bottom, t + 1, src.getHeight());
        mEditBitmap = Bitmap.createBitmap(src, l, t, r - l, b - t);
        src.recycle();
        CaptureActivity.pendingBitmap = null;

        mSnipView.setVisibility(View.GONE);
        mSnipSaveBtn.setVisibility(View.GONE);
        buildPaintPhase(root);
    }

    // =========================================================================
    // Paint phase
    // =========================================================================

    private void buildPaintPhase(FrameLayout root) {
        View bg = new View(this);
        bg.setBackgroundColor(0xFF0D1117);
        root.addView(bg, mp());

        mPaintCanvas = new PaintCanvas(this, mEditBitmap);
        mPaintCanvas.setColor(0xFFEF4444);
        mPaintCanvas.setBrushSize(BRUSH_SIZES[mBrushIdx]);
        root.addView(mPaintCanvas, mp());

        root.addView(buildPaintToolbar(), topBar());
    }

    // =========================================================================
    // Modern glass toolbar
    // =========================================================================

    private View buildPaintToolbar() {
        // Outer glass bar — full width, blurred dark semi-transparent
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(46), dp(10), dp(10));

        // Frosted glass background
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(0xCC0D1117);   // 80% opaque near-black
        barBg.setCornerRadius(0);
        bar.setBackground(barBg);

        // -- Save --
        bar.addView(makeIconBtn(R.drawable.ic_save, 0xFFFFFFFF, 0xCC1D4ED8, this::doSave));
        bar.addView(vSpace(6));

        // -- Divider --
        bar.addView(vDivider());
        bar.addView(vSpace(6));

        // -- Undo --
        bar.addView(makeIconBtn(R.drawable.ic_undo, 0xFFFFFFFF, 0x33FFFFFF, () -> mPaintCanvas.undo()));
        bar.addView(vSpace(4));

        // -- Redo --
        bar.addView(makeIconBtn(R.drawable.ic_redo, 0xFFFFFFFF, 0x33FFFFFF, () -> mPaintCanvas.redo()));
        bar.addView(vSpace(6));

        // -- Divider --
        bar.addView(vDivider());
        bar.addView(vSpace(6));

        // -- Brush size (icon cycles through S/M/L) --
        final ImageView[] brushBtn = {null};
        ImageView bBtn = makeIconBtnImg(BRUSH_DRAWABLES[mBrushIdx], 0xFFFFFFFF, 0x33FFFFFF, null);
        brushBtn[0] = bBtn;
        bBtn.setOnClickListener(v -> {
            mBrushIdx = (mBrushIdx + 1) % 3;
            bBtn.setImageResource(BRUSH_DRAWABLES[mBrushIdx]);
            mPaintCanvas.setBrushSize(BRUSH_SIZES[mBrushIdx]);
        });
        bar.addView(bBtn);
        bar.addView(vSpace(6));

        // -- Divider --
        bar.addView(vDivider());
        bar.addView(vSpace(6));

        // -- Color palette (scrollable) --
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        scroll.setLayoutParams(slp);

        LinearLayout palette = new LinearLayout(this);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.setGravity(Gravity.CENTER_VERTICAL);
        palette.setPadding(dp(2), 0, dp(2), 0);

        int[] colors = {
            0xFFEF4444, 0xFFF97316, 0xFFEAB308, 0xFF22C55E,
            0xFF06B6D4, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899,
            0xFFFFFFFF, 0xFFD1D5DB, 0xFF6B7280, 0xFF111827
        };

        View[] dots = new View[colors.length];
        for (int i = 0; i < colors.length; i++) {
            final int col = colors[i];
            final int idx = i;
            View dot = new View(this);
            dot.setTag(col);
            updateDot(dot, col, false);

            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(24), dp(24));
            dlp.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(dlp);

            dot.setOnClickListener(v -> {
                mPaintCanvas.setColor(col);
                for (View d : dots) updateDot(d, (int) d.getTag(), false);
                updateDot(dot, col, true);
            });
            dots[i] = dot;
            palette.addView(dot);
        }

        // Select red by default
        mPaintCanvas.setColor(colors[0]);
        updateDot(dots[0], colors[0], true);

        scroll.addView(palette);
        bar.addView(scroll);

        return bar;
    }

    private void updateDot(View dot, int color, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(selected ? dp(2) : dp(1),
                     selected ? 0xFFFFFFFF : 0x55FFFFFF);
        dot.setBackground(gd);
        dot.setScaleX(selected ? 1.2f : 1f);
        dot.setScaleY(selected ? 1.2f : 1f);
    }

    // =========================================================================
    // Toolbar component helpers
    // =========================================================================

    /** ImageView-based icon button with rounded glass bg */
    private ImageView makeIconBtnImg(int drawableRes, int tint, int bgColor, Runnable action) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(drawableRes);
        iv.setColorFilter(tint);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setPadding(dp(7), dp(7), dp(7), dp(7));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(10));
        iv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        iv.setLayoutParams(lp);
        if (action != null) iv.setOnClickListener(v -> action.run());
        return iv;
    }

    /** View-based icon button that wraps an ImageView */
    private View makeIconBtn(int drawableRes, int tint, int bgColor, Runnable action) {
        return makeIconBtnImg(drawableRes, tint, bgColor, action);
    }

    private View vDivider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(22));
        lp.gravity = Gravity.CENTER_VERTICAL;
        v.setLayoutParams(lp);
        v.setBackgroundColor(0x44FFFFFF);
        return v;
    }

    private View vSpace(int dpW) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(dpW), 1));
        return v;
    }

    // =========================================================================
    // Save
    // =========================================================================

    private void doSave() {
        if (mEditBitmap == null || mEditBitmap.isRecycled()) return;
        Bitmap flat = mEditBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(flat);
        mPaintCanvas.drawStrokesOnto(c);
        ImageSaver.save(this, flat);
        flat.recycle();
        finish();
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override public void onBackPressed() { cleanup(); super.onBackPressed(); }
    @Override protected void onDestroy()  { cleanup(); super.onDestroy(); }

    private void cleanup() {
        if (CaptureActivity.pendingBitmap != null && !CaptureActivity.pendingBitmap.isRecycled()) {
            CaptureActivity.pendingBitmap.recycle();
            CaptureActivity.pendingBitmap = null;
        }
        if (mEditBitmap != null && !mEditBitmap.isRecycled()) {
            mEditBitmap.recycle();
            mEditBitmap = null;
        }
    }

    private void setupFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        win.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private FrameLayout.LayoutParams mp() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private FrameLayout.LayoutParams topBar() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // =========================================================================
    // SnipView
    // =========================================================================

    interface OnSelectionChangedListener { void onChanged(boolean has); }

    static class SnipView extends View {
        private static final int NONE=-1,TL=0,TR=1,BL=2,BR=3;
        private static final int M_NONE=0,M_DRAW=1,M_MOVE=2,M_RESIZE=3;
        private final Bitmap bmp;
        private float mL,mT,mR,mB;
        private boolean mHas=false;
        private int mode=M_NONE,corner=NONE;
        private float dX,dY,sL,sT,sR,sB;
        private float scX=1,scY=1;
        private OnSelectionChangedListener cb;
        private final float HIT,HR;
        private final Paint dim=new Paint(),border=new Paint(),
                handle=new Paint(),lbg=new Paint(),ltxt=new Paint();

        SnipView(Context ctx,Bitmap b){
            super(ctx);bmp=b;
            float d=ctx.getResources().getDisplayMetrics().density;
            HIT=30*d;HR=7*d;
            dim.setColor(0x88000000);
            border.setStyle(Paint.Style.STROKE);border.setColor(0xFFFFFFFF);
            border.setStrokeWidth(2f);border.setAntiAlias(true);
            handle.setColor(0xFF42A5F5);handle.setAntiAlias(true);
            lbg.setColor(0xDD1565C0);lbg.setAntiAlias(true);
            ltxt.setColor(Color.WHITE);ltxt.setTextSize(12*d);ltxt.setAntiAlias(true);
            setLayerType(LAYER_TYPE_SOFTWARE,null);
        }
        void setOnSelectionChangedListener(OnSelectionChangedListener l){cb=l;}
        Rect getSelection(){
            if(!mHas)return null;
            return new Rect((int)(Math.min(mL,mR)*scX),(int)(Math.min(mT,mB)*scY),
                           (int)(Math.max(mL,mR)*scX),(int)(Math.max(mT,mB)*scY));
        }
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            scX=(float)bmp.getWidth()/Math.max(1,w);
            scY=(float)bmp.getHeight()/Math.max(1,h);
        }
        @Override protected void onDraw(Canvas c){
            c.drawBitmap(bmp,null,new Rect(0,0,getWidth(),getHeight()),null);
            if(!mHas){c.drawRect(0,0,getWidth(),getHeight(),dim);return;}
            float l=Math.min(mL,mR),t=Math.min(mT,mB),r=Math.max(mL,mR),b=Math.max(mT,mB);
            c.drawRect(0,0,getWidth(),t,dim);c.drawRect(0,b,getWidth(),getHeight(),dim);
            c.drawRect(0,t,l,b,dim);c.drawRect(r,t,getWidth(),b,dim);
            c.drawRect(l,t,r,b,border);
            c.drawCircle(l,t,HR,handle);c.drawCircle(r,t,HR,handle);
            c.drawCircle(l,b,HR,handle);c.drawCircle(r,b,HR,handle);
            String lb=((int)((r-l)*scX))+" × "+((int)((b-t)*scY));
            float tw=ltxt.measureText(lb),p=6f;
            c.drawRoundRect(l+4,b+4,l+4+tw+p*2,b+4+ltxt.getTextSize()+p,6,6,lbg);
            c.drawText(lb,l+4+p,b+4+ltxt.getTextSize(),ltxt);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY();
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    dX=x;dY=y;sL=mL;sT=mT;sR=mR;sB=mB;
                    if(mHas){int co=hc(x,y);if(co!=NONE){mode=M_RESIZE;corner=co;}
                    else if(ins(x,y)){mode=M_MOVE;}
                    else{mode=M_DRAW;mHas=false;mL=x;mT=y;mR=x;mB=y;if(cb!=null)cb.onChanged(false);}}
                    else{mode=M_DRAW;mL=x;mT=y;mR=x;mB=y;}
                    invalidate();break;
                case MotionEvent.ACTION_MOVE:
                    float dx=x-dX,dy=y-dY;
                    if(mode==M_DRAW){mR=x;mB=y;
                        if(!mHas&&(Math.abs(mR-mL)>dp(8)||Math.abs(mB-mT)>dp(8))){mHas=true;if(cb!=null)cb.onChanged(true);}}
                    else if(mode==M_MOVE){float w=sR-sL,h=sB-sT;
                        float nl=Math.max(0,Math.min(sL+dx,getWidth()-w));
                        float nt=Math.max(0,Math.min(sT+dy,getHeight()-h));
                        mL=nl;mT=nt;mR=nl+w;mB=nt+h;}
                    else if(mode==M_RESIZE){switch(corner){case TL:mL=x;mT=y;break;case TR:mR=x;mT=y;break;case BL:mL=x;mB=y;break;case BR:mR=x;mB=y;break;}}
                    invalidate();break;
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                    float tl=Math.min(mL,mR),tt=Math.min(mT,mB),tr=Math.max(mL,mR),tb=Math.max(mT,mB);
                    mL=tl;mT=tt;mR=tr;mB=tb;mode=M_NONE;corner=NONE;invalidate();break;
            }
            return true;
        }
        private int hc(float x,float y){
            float l=Math.min(mL,mR),t=Math.min(mT,mB),r=Math.max(mL,mR),b=Math.max(mT,mB);
            if(d(x,y,l,t)<HIT)return TL;if(d(x,y,r,t)<HIT)return TR;
            if(d(x,y,l,b)<HIT)return BL;if(d(x,y,r,b)<HIT)return BR;return NONE;
        }
        private boolean ins(float x,float y){
            float l=Math.min(mL,mR),t=Math.min(mT,mB),r=Math.max(mL,mR),b=Math.max(mT,mB);
            return x>=l&&x<=r&&y>=t&&y<=b;
        }
        private float d(float x1,float y1,float x2,float y2){float a=x1-x2,b=y1-y2;return(float)Math.sqrt(a*a+b*b);}
        private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    }

    // =========================================================================
    // PaintCanvas
    // =========================================================================

    static class PaintCanvas extends View {
        private static class Stroke { Path path=new Path(); Paint paint=new Paint(); }

        private final Bitmap       mBase;
        private final List<Stroke> mDone = new ArrayList<>();
        private final List<Stroke> mRedo = new ArrayList<>();
        private Stroke mCurrent;
        private int   mColor     = 0xFFEF4444;
        private float mBrushSize = 10f;
        private float mOffX,mOffY,mScale;
        private final RectF mDst = new RectF();

        PaintCanvas(Context ctx,Bitmap base){
            super(ctx);mBase=base;setLayerType(LAYER_TYPE_SOFTWARE,null);
        }
        void setColor(int c){mColor=c;}
        void setBrushSize(float s){mBrushSize=s;}
        void undo(){if(!mDone.isEmpty()){mRedo.add(0,mDone.remove(mDone.size()-1));invalidate();}}
        void redo(){if(!mRedo.isEmpty()){mDone.add(mRedo.remove(0));invalidate();}}

        void drawStrokesOnto(Canvas c){
            float inv=1f/mScale;
            c.save();
            c.translate(-mOffX*inv,-mOffY*inv);
            c.scale(inv,inv);
            for(Stroke s:mDone)c.drawPath(s.path,s.paint);
            c.restore();
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            float sw=(float)w/mBase.getWidth(),sh=(float)h/mBase.getHeight();
            mScale=Math.min(sw,sh);
            float dw=mBase.getWidth()*mScale,dh=mBase.getHeight()*mScale;
            mOffX=(w-dw)/2f;mOffY=(h-dh)/2f;
            mDst.set(mOffX,mOffY,mOffX+dw,mOffY+dh);
        }

        @Override protected void onDraw(Canvas c){
            c.drawBitmap(mBase,null,mDst,null);
            for(Stroke s:mDone)c.drawPath(s.path,s.paint);
            if(mCurrent!=null)c.drawPath(mCurrent.path,mCurrent.paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY();
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    mRedo.clear();
                    mCurrent=new Stroke();
                    mCurrent.paint.setAntiAlias(true);
                    mCurrent.paint.setStyle(Paint.Style.STROKE);
                    mCurrent.paint.setStrokeWidth(mBrushSize);
                    mCurrent.paint.setStrokeCap(Paint.Cap.ROUND);
                    mCurrent.paint.setStrokeJoin(Paint.Join.ROUND);
                    mCurrent.paint.setColor(mColor);
                    mCurrent.path.moveTo(x,y);
                    mCurrent.path.lineTo(x+0.1f,y+0.1f);
                    invalidate();break;
                case MotionEvent.ACTION_MOVE:
                    if(mCurrent!=null){mCurrent.path.lineTo(x,y);invalidate();}break;
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                    if(mCurrent!=null){mDone.add(mCurrent);mCurrent=null;invalidate();}break;
            }
            return true;
        }
    }
}
