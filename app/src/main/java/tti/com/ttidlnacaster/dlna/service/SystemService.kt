package tti.com.ttidlnacaster.dlna.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.controlpoint.SubscriptionCallback
import org.fourthline.cling.model.gena.CancelReason
import org.fourthline.cling.model.gena.GENASubscription
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.lastchange.LastChange
import org.fourthline.cling.support.model.TransportState
import tti.com.ttidlnacaster.TTIDLNACaster
import tti.com.ttidlnacaster.dlna.SystemManager
import tti.com.ttidlnacaster.dlna.server.ResourceServer
import tti.com.ttidlnacaster.dlna.action.Intents
import tti.com.ttidlnacaster.util.Constants
import tti.com.ttidlnacaster.util.PlayBackCommand
import java.util.concurrent.Executors

/**
 * Created by dylan_liang on 2018/2/9.
 */
class SystemService: Service() {

    var eventHandler: Handler? = null
    var selectedDevice: Device<*, *, *>? = null
    private var avTransportSubscriptionCallback: AVTransportSubscriptionCallback? = null

    private val threadPool by lazy { Executors.newCachedThreadPool() }
    private val jettyResourceServer by lazy { ResourceServer() }

    override fun onCreate() {
        super.onCreate()
        threadPool.execute(jettyResourceServer)
    }

    override fun onDestroy() {
        avTransportSubscriptionCallback?.end()
        jettyResourceServer.stopIfRunning()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return SystemBinder()
    }

    inner class SystemBinder: Binder() {
        val systemService: SystemService
            get() = this@SystemService
    }

    fun setSelectedDevice(selectedDevice: Device<*, *, *>?, controlPoint: ControlPoint?) {
        if (this.selectedDevice != selectedDevice) {
            if (this.selectedDevice != null) {
                PlayBackCommand.stop(null)

                if (SystemManager.instance.castState == Constants.CastState.STARTED) {
                    val castService = Intent(TTIDLNACaster.context, CastService::class.java)
                    castService.action = Constants.CAST_ACTION_STOP
                    TTIDLNACaster.context.startService(castService)
                }
                else if (SystemManager.instance.renderState == Constants.RenderState.STARTED) {
                    val mediaService = Intent(TTIDLNACaster.context, MediaService::class.java)
                    mediaService.action = Constants.RENDER_ACTION_CONTROL
                    mediaService.putExtra(Constants.RENDER_INTENT_COMMAND, Constants.ControlCommand.STOP)
                    TTIDLNACaster.context.startService(mediaService)
                }
            }

            this.selectedDevice = selectedDevice
            avTransportSubscriptionCallback?.end()
            avTransportSubscriptionCallback = AVTransportSubscriptionCallback(this.selectedDevice?.findService(SystemManager.AV_TRANSPORT_SERVICE))
            controlPoint?.execute(avTransportSubscriptionCallback)
        }
    }

    private inner class AVTransportSubscriptionCallback (service: org.fourthline.cling.model.meta.Service<*, *>?): SubscriptionCallback(service) {

        override fun failed(subscription: GENASubscription<*>, responseStatus: UpnpResponse, exception: Exception, defaultMsg: String) {
            Log.e(TAG, "AVTransportSubscriptionCallback failed.")
        }

        override fun established(subscription: GENASubscription<*>) {}

        override fun ended(subscription: GENASubscription<*>, reason: CancelReason?, responseStatus: UpnpResponse) {
            Log.i(TAG, "AVTransportSubscriptionCallback ended.")
        }

        override fun eventReceived(subscription: GENASubscription<*>) {
            val values = subscription.currentValues
            if (values != null && values.containsKey("LastChange")) {
                val lastChangeValue = values["LastChange"].toString()
                Log.i(TAG, "LastChange:" + lastChangeValue)
                val lastChange: LastChange
                try {
                    lastChange = LastChange(AVTransportLastChangeParser(), lastChangeValue)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }

                //Parse TransportState value.
                val transportState = lastChange.getEventedValue(0, AVTransportVariable.TransportState::class.java)
                if (transportState != null && eventHandler != null) {
                    val message =
                            Message.obtain(eventHandler, SystemManager.instance.MESSAGE_RENDER_EVENT, transportState)
                    message.sendToTarget()
                }

                //Parse CurrentTrackMetaData value.
                val currentTrackMetaData = lastChange.getEventedValue(0, AVTransportVariable.CurrentTrackMetaData::class.java)
                if (currentTrackMetaData != null && currentTrackMetaData.value != null) {
                    val didlParser = DIDLParser()
                    var lastChangeIntent: Intent?
                    try {
                        val content = didlParser.parse(currentTrackMetaData.value)
                        val item = content.items[0]
                        val creator = item.creator
                        val title = item.title

                        lastChangeIntent = Intent(Intents.ACTION_UPDATE_LAST_CHANGE)
                        lastChangeIntent.putExtra("creator", creator)
                        lastChangeIntent.putExtra("title", title)
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse CurrentTrackMetaData error.")
                        lastChangeIntent = null
                    }

                    if (lastChangeIntent != null)
                        sendBroadcast(lastChangeIntent)
                }
            }
        }

        override fun eventsMissed(subscription: GENASubscription<*>, numberOfMissedEvents: Int) {}
    }

    companion object {
        private val TAG = SystemService::class.java.simpleName
    }
}