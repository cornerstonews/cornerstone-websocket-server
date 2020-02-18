package com.github.cornerstonews.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.server.Server;

public class WebsocketServer {

    private final static Logger LOG = LogManager.getLogger(WebsocketServer.class);
    
    private String host;
    private int serverPort;
    private String contextPath;
    private Map<String, Object> serverProperties;
    private Class<?>[] endpointClasses;

    // https://github.com/tyrus-project/tyrus/blob/master/server/src/main/java/org/glassfish/tyrus/server/Server.java
    private Server server;

    public WebsocketServer() {
        this("localhost", 8888, "/", new HashMap<String, Object>(), new Class<?>[] {});
    }

    public WebsocketServer(String host, int serverPort, String contextPath, Map<String, Object> serverProperties, Class<?>... endpointClasses) {
        this.host = host;
        this.serverProperties = serverProperties;
        this.serverPort = serverPort;
        this.contextPath = contextPath;
        this.endpointClasses = endpointClasses;
    }

    public void start() throws DeploymentException {
        this.server = new Server(host, serverPort, contextPath, serverProperties, endpointClasses);
        this.server.start();
        LOG.debug("Websocket Server Started.");
    }

    public void stop() {
        if (this.server != null) {
            LOG.debug("Websocket Server Stopped.");
            this.server.stop();
        }
    }

    public static void main(String[] args) throws DeploymentException, IOException {
        WebsocketServer websocketServer = new WebsocketServer();
        websocketServer.start();
    }
}
