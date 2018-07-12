package tti.com.ttidlnacaster.dlna.server

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.bio.SocketConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.ExecutorThreadPool
import org.fourthline.cling.transport.spi.ServletContainerAdapter
import java.io.IOException
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.Servlet
import javax.servlet.http.HttpServletRequest

/**
 * Created by dylan_liang on 2018/2/9.
 */
class ServletContainer private constructor(): ServletContainerAdapter {

    private lateinit var server: Server

    init {
        resetServer()
    }

    @Synchronized override fun setExecutorService(executorService: ExecutorService) {
        if (INSTANCE.server.threadPool == null) {
            INSTANCE.server.threadPool = object: ExecutorThreadPool(executorService) {
                @Throws(Exception::class)
                override fun doStop() {
                    // Do nothing, don't shut down the Cling ExecutorService when Jetty stops!
                }
            }
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun addConnector(host: String, port: Int): Int {
        val connector = SocketConnector()
        connector.host = host
        connector.port = port

        // Open immediately so we can get the assigned local port
        connector.open()

        // Only add if open() succeeded
        server.addConnector(connector)

        // stats the connector if the server is started (server starts all connectors when started)
        if (server.isStarted) {
            try {
                connector.start()
            } catch (ex: Exception) {
                log.severe("Couldn't start connector: $connector $ex")
                throw RuntimeException(ex)
            }

        }
        return connector.localPort
    }

    @Synchronized override fun removeConnector(host: String, port: Int) {
        val connectors = server.connectors ?: return

        for (connector in connectors) {
            //Fix getPort()
            if (connector.host == host && connector.localPort == port) {
                if (connector.isStarted || connector.isStarting) {
                    try {
                        connector.stop()
                    } catch (ex: Exception) {
                        log.severe("Couldn't stop connector: $connector $ex")
                        throw RuntimeException(ex)
                    }

                }
                server.removeConnector(connector)
                if (connectors.size == 1) {
                    log.info("No more connectors, stopping Jetty server")
                    stopIfRunning()
                }
                break
            }
        }
    }

    @Synchronized override fun registerServlet(contextPath: String?, servlet: Servlet) {
        if (server.handler != null) {
            return
        }
        log.info("Registering UPnP servlet under context path: " + contextPath!!)
        val servletHandler = ServletContextHandler(ServletContextHandler.NO_SESSIONS)
        if (contextPath.isNotEmpty())
            servletHandler.contextPath = contextPath
        val s = ServletHolder(servlet)
        servletHandler.addServlet(s, "/*")
        server.handler = servletHandler
    }

    @Synchronized override fun startIfNotRunning() {
        if (!server.isStarted && !server.isStarting) {
            log.info("Starting Jetty server... ")
            try {
                server.start()
            } catch (ex: Exception) {
                log.severe("Couldn't start Jetty server: " + ex)
                throw RuntimeException(ex)
            }

        }
    }

    @Synchronized override fun stopIfRunning() {
        if (!server.isStopped && !server.isStopping) {
            log.info("Stopping Jetty server...")
            try {
                server.stop()
            } catch (ex: Exception) {
                log.severe("Couldn't stop Jetty server: " + ex)
                throw RuntimeException(ex)
            } finally {
                resetServer()
            }
        }
    }

    private fun resetServer() {
        server = Server() // Has its own QueuedThreadPool
        server.gracefulShutdown = 1000 // Let's wait a second for ongoing transfers to complete
    }

    companion object {
        private val log = Logger.getLogger(ServletContainer::class.java.name)

        // Singleton
        val INSTANCE = ServletContainer()

        @JvmOverloads
        fun isConnectionOpen(request: HttpServletRequest, heartbeat: ByteArray = " ".toByteArray()): Boolean {
            val jettyRequest = request as Request
            val connection = jettyRequest.connection
            val socket = connection.endPoint.transport as Socket
            if (log.isLoggable(Level.FINE))
                log.fine("Checking if client connection is still open: " + socket.remoteSocketAddress)
            return try {
                socket.getOutputStream().write(heartbeat)
                socket.getOutputStream().flush()
                true
            } catch (ex: IOException) {
                if (log.isLoggable(Level.FINE))
                    log.fine("Client connection has been closed: " + socket.remoteSocketAddress)
                false
            }

        }
    }
}