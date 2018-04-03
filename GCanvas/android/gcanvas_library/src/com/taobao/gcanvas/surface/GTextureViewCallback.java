package com.taobao.gcanvas.surface;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;

import com.taobao.gcanvas.GCanvasJNI;
import com.taobao.gcanvas.util.GLog;

import java.util.ArrayList;

/**
 * @author ertong
 *         create at 2017/8/1
 */

public class GTextureViewCallback implements TextureView.SurfaceTextureListener {
    private final String mKey;
    private String mBackgroundColor = "#ffffff";
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private TextureView mTextureview;
    private static boolean INITIALIZED = false;
    private Integer mContextType = null;
    private Boolean mHiQuality = null;
    private Double mDevicePixelRatio = null;

    private ArrayList<TextureView.SurfaceTextureListener> mDelegateLists;

    static {
        if (!INITIALIZED) {
            try {
                System.loadLibrary("gcanvas");
                System.loadLibrary("freetype");
                GCanvasJNI.setFontFamilies();
                INITIALIZED = true;
            } catch (Throwable throwable) {
                GLog.e("GTextureViewCallback", "error when load library", throwable);
            }
        }
    }

    public GTextureViewCallback(TextureView v, String id) {
        this.mKey = id;
        this.mTextureview = v;
    }


    public void addSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        if (null == mDelegateLists) {
            mDelegateLists = new ArrayList<>(1);
        }

        if (!mDelegateLists.contains(listener)) {
            mDelegateLists.add(listener);
        }
    }

    public void setBackgroundColor(String color) {
        if (!TextUtils.isEmpty(color)) {
            mBackgroundColor = color;
        }
    }

    public void setContextType(int contextType) {
        mContextType = contextType;
    }


    public void setHiQuality(boolean hiQuality) {
        mHiQuality = hiQuality;
    }

    public void setDevicePixelRatio(double devicePixelRatio) {
        mDevicePixelRatio = devicePixelRatio;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        GLog.d("on surfaceTexture Available.");
        if (mSurfaceTexture == null) {
            mSurface = new Surface(surface);
            mSurfaceTexture = surface;
        } else {
            mTextureview.setSurfaceTexture(mSurfaceTexture);
        }

        if (mContextType != null) {
            GCanvasJNI.setContextType(mKey, mContextType);
        }
        if (mHiQuality != null) {
            GCanvasJNI.setHiQuality(mKey, mHiQuality);
        }
        if (mDevicePixelRatio != null) {
            GCanvasJNI.setDevicePixelRatio(mKey, mDevicePixelRatio);
        }

        onSurfaceChanged(this.mKey, mSurface, 0, width, height, mBackgroundColor);


        if (GCanvasJNI.sendEvent(mKey)) {
            if (mTextureview instanceof GTextureView) {
                GLog.d("start to send event in GSurfaceCallback.");
                ((GTextureView) mTextureview).sendEvent();
            }
        }

        if (null != mDelegateLists) {
            for (TextureView.SurfaceTextureListener listener : mDelegateLists) {
                listener.onSurfaceTextureAvailable(surface, width, height);
            }
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        GLog.d("on surfaceTexture changed.");

        if (mSurface == null) {
            mSurface = new Surface(surface);
            mSurfaceTexture = surface;
        }

        onSurfaceChanged(this.mKey, mSurface, 0, width, height, mBackgroundColor);


        if (null != mDelegateLists) {
            for (TextureView.SurfaceTextureListener listener : mDelegateLists) {
                listener.onSurfaceTextureSizeChanged(surface, width, height);
            }
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        GLog.d("on surfaceTexture destroyed.");

        if (mSurfaceTexture == null || mSurface == null) {
            return true;
        }

        if (null != mDelegateLists) {
            mSurfaceTexture = null;
            for (TextureView.SurfaceTextureListener listener : mDelegateLists) {
                listener.onSurfaceTextureDestroyed(surface);
            }
        }
        onSurfaceDestroyed(this.mKey, mSurface);
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onRequestExit() {
        GLog.d("on RequestExit");

        if (mSurface != null) {
//                onSurfaceDestroyed(this.mKey, mSurface);
            mSurface.release();
            mSurface = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GLog.d("start to release surface textureview and surface in onRequestExit");
            if (mSurfaceTexture != null) {
//            onSurfaceTextureDestroyed(mSurfaceTexture);

                mSurfaceTexture.release();

                mSurfaceTexture = null;
            }
        }

        onRenderExit(this.mKey);

        if (null != mDelegateLists) {
            mDelegateLists.clear();
        }
    }

    public String getKey() {
        return mKey;
    }

    private native void onSurfaceCreated(String key, Surface surface);

    private native void onSurfaceChanged(String key, Surface surface, int format, int width, int height, String color);

    private native void onSurfaceDestroyed(String key, Surface surface);

    private native void onRenderExit(String key);

}