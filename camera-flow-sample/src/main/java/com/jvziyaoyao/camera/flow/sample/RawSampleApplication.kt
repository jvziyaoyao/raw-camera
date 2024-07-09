package com.jvziyaoyao.camera.flow.sample

import android.app.Application
import com.jvziyaoyao.camera.flow.sample.page.main.CameraRawViewModel
import com.jvziyaoyao.camera.flow.sample.page.sensor.SensorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class RawSampleApplication : Application(), CoroutineScope by MainScope() {

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