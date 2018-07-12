package tti.com.ttidlnacaster.dlna.service

import android.content.Intent
import android.os.IBinder
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.transport.impl.AsyncServletStreamServerConfigurationImpl
import org.fourthline.cling.transport.impl.AsyncServletStreamServerImpl
import org.fourthline.cling.transport.spi.NetworkAddressFactory
import org.fourthline.cling.transport.spi.StreamServer
import tti.com.ttidlnacaster.dlna.server.ServletContainer

/**
 * Created by dylan_liang on 2018/2/9.
 */
class UpnpService: AndroidUpnpServiceImpl() {

    override fun createConfiguration(): UpnpServiceConfiguration {
        return FixedAndroidUpnpServiceConfiguration()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    class FixedAndroidUpnpServiceConfiguration: AndroidUpnpServiceConfiguration() {
        override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
            // Use Jetty, start/stop a new shared instance of JettyServletContainer
            return AsyncServletStreamServerImpl(AsyncServletStreamServerConfigurationImpl(
                    ServletContainer.INSTANCE, networkAddressFactory.streamListenPort)
            )
        }
    }
}