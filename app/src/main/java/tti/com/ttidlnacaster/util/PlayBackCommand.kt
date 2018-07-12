package tti.com.ttidlnacaster.util

import android.os.Handler
import android.os.Message
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.avtransport.callback.*
import org.fourthline.cling.support.model.PositionInfo
import org.fourthline.cling.support.model.TransportInfo
import tti.com.ttidlnacaster.dlna.SystemManager

/**
 * Created by dylan_liang on 2018/2/27.
 */
object PlayBackCommand {

    val PLAY_NEW_MEDIA_SUCCEEDED = 901
    val PLAY_NEW_MEDIA_FAILED = 902
    val SEND_URL_SUCCEEDED = 903
    val SEND_URL_FAILED = 904
    val PLAY_SUCCEEDED = 905
    val PLAY_FAILED = 906
    val PAUSE_SUCCEEDED = 907
    val PAUSE_FAILED = 908
    val STOP_SUCCEEDED = 909
    val STOP_FAILED = 910
    val SEEK_SUCCEEDED = 911
    val SEEK_FAILED = 912
    val GET_POSITION_SUCCEEDED = 913
    val GET_POSITION_FAILED = 914
    val GET_TRANSPORT_SUCCEEDED = 915
    val GET_TRANSPORT_FAILED = 916

    fun sendMessage(message: Message) {
        message.sendToTarget()
    }

    fun getService(): Service<out Device<*, *, *>, out Service<*, *>>? {
        return SystemManager.instance.selectedDevice?.findService(SystemManager.AV_TRANSPORT_SERVICE)
    }

    fun playNewMedia(handler: Handler?, mediaUrl: String, metaData: String) {
        val avtService = getService()
        if (avtService != null) {
            val controlPoint = SystemManager.instance.controlPoint
            controlPoint.execute(object: Stop(avtService) {
                override fun success(invocation: ActionInvocation<*>) {
                    controlPoint.execute(object: SetAVTransportURI(
                            avtService, mediaUrl, metaData) {
                        override fun success(invocation: ActionInvocation<*>) {
                            sendMessage(Message.obtain(handler, PLAY_NEW_MEDIA_SUCCEEDED))
                            play(null)
                        }
                        override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                            sendMessage(Message.obtain(handler, PLAY_NEW_MEDIA_FAILED))
                        }
                    })
                }
                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    controlPoint.execute(object: SetAVTransportURI(
                            avtService, mediaUrl, metaData) {
                        override fun success(invocation: ActionInvocation<*>) {
                            sendMessage(Message.obtain(handler, PLAY_NEW_MEDIA_SUCCEEDED))
                            play(null)
                        }
                        override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                            sendMessage(Message.obtain(handler, PLAY_NEW_MEDIA_FAILED))
                        }
                    })
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, PLAY_NEW_MEDIA_FAILED))
    }

    fun sendUrl(handler: Handler?, mediaUrl: String, metaData: String) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object: SetAVTransportURI(
                    avtService, mediaUrl, metaData) {
                override fun success(invocation: ActionInvocation<*>) {
                    sendMessage(Message.obtain(handler, SEND_URL_SUCCEEDED))
                }
                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, SEND_URL_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, SEND_URL_FAILED))
    }

    fun play(handler: Handler?) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object: Play(avtService) {
                override fun success(invocation: ActionInvocation<*>) {
                    sendMessage(Message.obtain(handler, PLAY_SUCCEEDED))
                }
                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, PLAY_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, PLAY_FAILED))
    }

    fun pause(handler: Handler?) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object: Pause(avtService) {
                override fun success(invocation: ActionInvocation<*>) {
                    sendMessage(Message.obtain(handler, PAUSE_SUCCEEDED))
                }
                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, PAUSE_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, PAUSE_FAILED))
    }

    fun stop(handler: Handler?) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object: Stop(avtService) {
                override fun success(invocation: ActionInvocation<*>) {
                    sendMessage(Message.obtain(handler, STOP_SUCCEEDED))
                }
                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, STOP_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, STOP_FAILED))
    }

    fun seek(handler: Handler?, relativeTimeTarget: String) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object : Seek(avtService, relativeTimeTarget) {
                override fun success(invocation: ActionInvocation<*>?) {
                    sendMessage(Message.obtain(handler, SEEK_SUCCEEDED))
                }

                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, SEEK_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, SEEK_FAILED))
    }

    fun getPositionInfo(handler: Handler?) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object : GetPositionInfo(avtService) {
                override fun received(invocation: ActionInvocation<*>, positionInfo: PositionInfo) {
                    sendMessage(Message.obtain(handler, GET_POSITION_SUCCEEDED, positionInfo))
                }

                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, GET_POSITION_FAILED))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, GET_POSITION_FAILED))
    }

    fun getTransportInfo(handler: Handler?, flag: Int) {
        val avtService = getService()
        if (avtService != null) {
            SystemManager.instance.controlPoint.execute(object : GetTransportInfo(avtService) {
                override fun received(invocation: ActionInvocation<*>, transportInfo: TransportInfo) {
                    sendMessage(Message.obtain(handler, GET_TRANSPORT_SUCCEEDED, flag, 0, transportInfo))
                }

                override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                    sendMessage(Message.obtain(handler, GET_TRANSPORT_FAILED, flag, 0))
                }
            })
        }
        else
            sendMessage(Message.obtain(handler, GET_TRANSPORT_FAILED, flag, 0))
    }
}