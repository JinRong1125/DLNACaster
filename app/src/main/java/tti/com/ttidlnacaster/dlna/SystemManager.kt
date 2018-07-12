package tti.com.ttidlnacaster.dlna

import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDAServiceType
import tti.com.ttidlnacaster.dlna.service.SystemService
import tti.com.ttidlnacaster.mvp.contract.CastContract
import tti.com.ttidlnacaster.mvp.contract.MediaContract
import tti.com.ttidlnacaster.util.Constants

/**
 * Created by dylan_liang on 2018/2/9.
 */
class SystemManager {

    lateinit var upnpService: AndroidUpnpService
    lateinit var systemService: SystemService

    var castView: CastContract.View? = null
    var mediaView: MediaContract.View? = null

    var castState: Constants.CastState? = null
    var renderState: Constants.RenderState? = null

    var sentLength: Long = 0

    val MESSAGE_RENDER_EVENT by lazy { 501 }

    val dmcDevices: Collection<Device<*, *, *>>
        get() {
            return upnpService.registry.getDevices(CONTENT_DIRECTORY_SERVICE)
        }

    val dmrDevices: Collection<Device<*, *, *>>
        get() {
            controlPoint.search()
            return upnpService.registry.getDevices(TYPE_DMR_DEVICE)
        }

    val controlPoint: ControlPoint
        get() = upnpService.controlPoint

    var selectedDevice: Device<*, *, *>?
        set(selectedDevice) {
            systemService.setSelectedDevice(selectedDevice, controlPoint)
        }
        get() = systemService.selectedDevice

    companion object {
        val CONTENT_DIRECTORY_SERVICE: ServiceType = UDAServiceType("ContentDirectory")
        val AV_TRANSPORT_SERVICE: ServiceType = UDAServiceType("AVTransport")
        val RENDERING_CONTROL_SERVICE: ServiceType = UDAServiceType("RenderingControl")
        val TYPE_DMR_DEVICE = UDADeviceType("MediaRenderer")

        var instance: SystemManager = SystemManager()
    }
}