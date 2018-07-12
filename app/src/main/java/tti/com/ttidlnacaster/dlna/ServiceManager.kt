package tti.com.ttidlnacaster.dlna

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.fourthline.cling.android.AndroidUpnpService
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.service.SystemService
import tti.com.ttidlnacaster.dlna.service.UpnpService
import tti.com.ttidlnacaster.util.Constants

/**
 * Created by dylan_liang on 2018/2/12.
 */
class ServiceManager {

    fun startService() {
        TTIDLNACaster.context.bindService(Intent(TTIDLNACaster.context, UpnpService::class.java),
                upnpServiceConnection, Context.BIND_AUTO_CREATE)
        TTIDLNACaster.context.bindService(Intent(TTIDLNACaster.context, SystemService::class.java),
                systemServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private val upnpServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            SystemManager.instance.upnpService = service as AndroidUpnpService
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    private val systemServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SystemService.SystemBinder
            SystemManager.instance.systemService = binder.systemService
            SystemManager.instance.castState = Constants.CastState.STOPPED
        }

        override fun onServiceDisconnected(className: ComponentName) {

        }
    }
}