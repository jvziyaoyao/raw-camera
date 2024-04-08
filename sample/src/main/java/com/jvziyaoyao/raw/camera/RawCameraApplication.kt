package com.jvziyaoyao.raw.camera

import android.app.Application
import com.jvziyaoyao.raw.camera.page.main.CameraRawViewModel
import com.jvziyaoyao.raw.camera.page.sensor.SensorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class RawCameraApplication : Application(), CoroutineScope by MainScope() {

    override fun onCreate() {
        super.onCreate()

        GlobalContext.startKoin {
            printLogger()
            modules(injectionModel)
        }
    }

    private val injectionModel = module {
        viewModelOf(::SensorViewModel)
        viewModelOf(::CameraRawViewModel)
    }

}