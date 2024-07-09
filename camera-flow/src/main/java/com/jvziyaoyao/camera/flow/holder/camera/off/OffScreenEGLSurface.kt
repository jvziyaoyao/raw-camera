package com.jvziyaoyao.camera.flow.holder.camera.off

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.DisplayMetrics
import android.view.WindowManager

open class BaseEGLSurface {
    private var mEGLDisplay: EGLDisplay? = null
    private var mEGLConfig: EGLConfig? = null
    private var mEGLContext: EGLContext? = null
    private var mEGLSurface: EGLSurface? = null
    private var mContext: Context
    private var mRenderer: Renderer? = null
    private var mEglStatus = EglStatus.INVALID
    private var mWidth: Int
    private var mHeight: Int

    constructor(context: Context) {
        mContext = context
        val mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getRealMetrics(displayMetrics)
        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels
    }

    constructor(context: Context, width: Int, height: Int) {
        mContext = context
        mWidth = width
        mHeight = height
    }

    // 设置渲染器
    fun setRenderer(renderer: Renderer?) {
        mRenderer = renderer
    }

    // EGLDisplay宽高发生变化
    fun onSurfaceChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
        createSurface()
        mEglStatus = EglStatus.CREATED
    }

    // 请求渲染
    fun requestRender() {
        if (mEglStatus == EglStatus.INVALID) {
            return
        }
        if (mEglStatus == EglStatus.INITIALIZED) {
            mRenderer!!.onSurfaceCreated()
            mRenderer!!.onSurfaceChanged(mWidth, mHeight)
            mEglStatus = EglStatus.CREATED
        }
        if (mEglStatus == EglStatus.CREATED || mEglStatus == EglStatus.DRAW) {
            mRenderer!!.onDrawFrame()
            mEglStatus = EglStatus.DRAW
        }
    }

    // 创建EGL环境
    fun createEGLEnv() {
        createDisplay()
        createConfig()
        createContext()
        createSurface()
        makeCurrent()
    }

    // 销毁EGL环境
    fun destroyEGLEnv() {
        // 与显示设备解绑
        EGL14.eglMakeCurrent(
            mEGLDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        // 销毁 EGLSurface
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
        // 销毁EGLContext
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
        // 销毁EGLDisplay(显示设备)
        EGL14.eglTerminate(mEGLDisplay)
        mEGLContext = null
        mEGLSurface = null
        mEGLDisplay = null
    }

    // 1.创建EGLDisplay
    private fun createDisplay() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val versions = IntArray(2)
        EGL14.eglInitialize(mEGLDisplay, versions, 0, versions, 1)
    }

    // 2.创建EGLConfig
    private fun createConfig() {
        val configs = arrayOfNulls<EGLConfig>(1)
        val configNum = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, mEGLConfigAttrs, 0, configs, 0, 1, configNum, 0)
        if (configNum[0] > 0) {
            mEGLConfig = configs[0]
        }
    }

    // 3.创建EGLContext
    private fun createContext() {
        if (mEGLConfig != null) {
            mEGLContext = EGL14.eglCreateContext(
                mEGLDisplay,
                mEGLConfig,
                EGL14.EGL_NO_CONTEXT,
                mEGLContextAttrs,
                0
            )
        }
    }

    // 4.创建EGLSurface
    private fun createSurface() {
        if (mEGLContext != null && mEGLContext !== EGL14.EGL_NO_CONTEXT) {
            val eglSurfaceAttrs =
                intArrayOf(EGL14.EGL_WIDTH, mWidth, EGL14.EGL_HEIGHT, mHeight, EGL14.EGL_NONE)
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, eglSurfaceAttrs, 0)
        }
    }

    // 5.绑定EGLSurface和EGLContext到显示设备（EGLDisplay）
    private fun makeCurrent() {
        if (mEGLSurface != null && mEGLSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
            mEglStatus = EglStatus.INITIALIZED
        }
    }

    // EGLConfig参数
    private val mEGLConfigAttrs = intArrayOf(
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_DEPTH_SIZE, 8,
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_NONE
    )

    // EGLContext参数
    private val mEGLContextAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)

    // EGL状态
    enum class EglStatus {
        INVALID,
        INITIALIZED,
        CREATED,
        DRAW
    }
}

class OffScreenEGLSurface : BaseEGLSurface {
    constructor(context: Context) : super(context)
    constructor(context: Context, width: Int, height: Int) : super(context, width, height)

    fun init(renderer: Renderer) {
        setRenderer(renderer)
        createEGLEnv()
    }
}