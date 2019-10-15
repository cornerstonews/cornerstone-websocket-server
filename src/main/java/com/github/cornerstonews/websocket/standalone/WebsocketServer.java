package com.github.cornerstonews.websocket.standalone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebsocketServer {

	private ExecutorService pool;
	private int port = 8888;
	private boolean run = true;
	private ServerSocket server;

	public WebsocketServer() {
		pool = Executors.newFixedThreadPool(10);

	}

	public void start() throws IOException {
		try (ServerSocket server = new ServerSocket(port)) {
			this.server = server;
			System.out.println("AdbWebsocketServer has started on 127.0.0.1:8888.\r\nWaiting for a connection...");

			while (run) {
				Socket client = server.accept();
				System.out.println("A client connected.");

				this.pool.execute(new WesocketClientThread(client));

			}
		}
	}

	public void stop() throws IOException {
		this.run = false;
		if (!this.server.isClosed()) {
			this.server.close();
		}
		this.pool.shutdown();
	}

	public static void main(String[] args) throws IOException {
		WebsocketServer adbWebsocketServer = new WebsocketServer();
		adbWebsocketServer.start();
	}
}
