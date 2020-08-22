package com.github.cornerstonews.websocket;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.container.grizzly.server.CornerstoneGrizzlyServerContainer;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.ServerContainer;

import com.github.cornerstonews.websocket.ssl.CornerstoneKeyStore;
import com.github.cornerstonews.websocket.ssl.CornerstoneSSLContext;

public class WebsocketServer {

    // https://github.com/tyrus-project/tyrus/blob/master/server/src/main/java/org/glassfish/tyrus/server/Server.java

    private final static Logger LOGGER = LogManager.getLogger(WebsocketServer.class);

    private static final int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST_NAME = "localhost";
    private static final String DEFAULT_CONTEXT_PATH = "/";

    private final Map<String, Object> properties;
    private final Set<Class<?>> endpointClasses;
    private final String hostName;
    private volatile int port;
    private final String contextPath;
    private final CornerstoneKeyStore keyStore;

    private ServerContainer server;

    public WebsocketServer() {
        this("localhost", 8888, null, "/", new HashMap<String, Object>(), new Class<?>[] {});
    }

    public WebsocketServer(String hostName, int serverPort, CornerstoneSSLContext sslContext, String contextPath, Map<String, Object> properties,
            Class<?>... endpointClasses) {
        this(hostName, serverPort, sslContext, contextPath, properties, new HashSet<Class<?>>(Arrays.asList(endpointClasses)));
    }

    public WebsocketServer(String hostName, int serverPort, CornerstoneSSLContext sslContext, String contextPath, Map<String, Object> properties,
            Set<Class<?>> endpointClasses) {
        this.hostName = hostName == null ? DEFAULT_HOST_NAME : hostName;
        if (port <= 0) {
            this.port = DEFAULT_PORT;
        } else {
            this.port = port;
        }
        this.keyStore = sslContext == null ? null : sslContext.getKeyStore();
        this.contextPath = contextPath == null ? DEFAULT_CONTEXT_PATH : contextPath;
        this.properties = properties == null ? null : new HashMap<String, Object>(properties);
        this.endpointClasses = endpointClasses;
    }

    public void start() throws DeploymentException {
        try {
            if (server == null) {
                server = new CornerstoneGrizzlyServerContainer().createContainer(properties, keyStore);

                for (Class<?> clazz : endpointClasses) {
                    server.addEndpoint(clazz);
                }

                server.start(contextPath, port);

                if (server instanceof TyrusServerContainer) {
                    this.port = ((TyrusServerContainer) server).getPort();
                }

                LOGGER.info("WebSocket server started.");
                LOGGER.info("WebSocket URLs start with " + this.keyStore == null ? "ws" : "wss" + "://" + this.hostName + ":" + this.port);

            }
        } catch (IOException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop();
            this.server = null;
            LOGGER.debug("Websocket Server Stopped.");
        }
    }

    public static void main(String[] args) throws DeploymentException, IOException {
        WebsocketServer websocketServer = new WebsocketServer();
        websocketServer.start();
    }
}
