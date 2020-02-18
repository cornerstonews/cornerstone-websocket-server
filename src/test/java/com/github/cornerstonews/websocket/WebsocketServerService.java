package com.github.cornerstonews.websocket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebsocketServerService {

    private final static Logger LOG = LogManager.getLogger(WebsocketServerService.class);

    private final static WebsocketServerService websocketServerService = new WebsocketServerService();

    private WebsocketServer websocketServer;

    private WebsocketServerService() {
    }

    private static WebsocketServerService getInstance() {
        return websocketServerService;
    }

    public void start() throws DeploymentException {
        LOG.info("Starting Websocket Server.");
        this.addShutdownHook();
        this.websocketServer = new WebsocketServer("localhost", 8888, "/", new HashMap<String, Object>(), WebsocketServerEndpoint.class);
        this.websocketServer.start();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (websocketServer != null) {
                        websocketServerService.shutdown();
                        LOG.debug("Done running shutdown hook.");
                    }
                } catch (Exception e) {
                    LOG.error("Exception stopping Websocket Server", e);
                }
            }
        }));
    }

    public void shutdown() {
        LOG.info("Shutting down Websocket Server...");
        this.websocketServer.stop();
    }

    public static void main(String[] args) throws Exception {
        WebsocketServerService websocketServerService = WebsocketServerService.getInstance();
        websocketServerService.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        System.out.print("Please enter 'stop' to shutdown the WebsocketServerService.");

        String line;
        while ((line = reader.readLine()) != null) {
            if ("stop".equalsIgnoreCase(line.trim().toLowerCase())) {
                websocketServerService.shutdown();
                break;
            }
//            System.out.print("Please enter 'stop' to shutdown the WebsocketServerService.");
        }
    }
}
