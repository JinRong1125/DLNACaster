package tti.com.ttidlnacaster

import android.app.Application
import android.content.Context
import kotlin.properties.Delegates

/**
 * Created by dylan_liang on 2018/2/8.
 */
class TTIDLNACaster: Application() {

    companion object {
        var context: Context by Delegates.notNull()
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}