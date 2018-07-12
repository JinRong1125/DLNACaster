package tti.com.ttidlnacaster.dlna.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.resource.Resource
import tti.com.ttidlnacaster.util.Constants
import java.net.URL
import java.util.logging.Logger

/**
 * Created by dylan_liang on 2018/2/9.
 */
class ResourceServer: Runnable {

    private val server: Server by lazy { Server(JETTY_SERVER_PORT) }

    val serverState: String
        get() = server.state

    init {
//        server = Server(JETTY_SERVER_PORT) // Has its own QueuedThreadPool
        server.gracefulShutdown = 1000 // Let's wait a second for ongoing transfers to complete
    }

    @Synchronized
    private fun startIfNotRunning() {
        if (!server.isStarted && !server.isStarting) {
            log.info("Starting JettyResourceServer")
            try {
                server.start()
            } catch (ex: Exception) {
                log.severe("Couldn't start Jetty server: " + ex)
                throw RuntimeException(ex)
            }

        }
    }

    @Synchronized
    fun stopIfRunning() {
        if (!server.isStopped && !server.isStopping) {
            log.info("Stopping JettyResourceServer")
            try {
                server.stop()
            } catch (ex: Exception) {
                log.severe("Couldn't stop Jetty server: " + ex)
                throw RuntimeException(ex)
            }
        }
    }

    override fun run() {
        val context = ServletContextHandler()
        context.contextPath = "/"
        context.setInitParameter("org.eclipse.jetty.servlet.Default.gzip", "false")
        server.handler = context

        context.addServlet(StreamServlet::class.java, "/dlna/*")

        startIfNotRunning()
    }

    companion object {
        private val log = Logger.getLogger(ResourceServer::class.java.name)

        val JETTY_SERVER_PORT = 8181
    }

    class StreamServlet: DefaultServlet() {
        override fun getResource(pathInContext: String): Resource {
            return StreamResource(URL("file://" + Constants.CAST_FILE_PATH))
        }
    }
}
