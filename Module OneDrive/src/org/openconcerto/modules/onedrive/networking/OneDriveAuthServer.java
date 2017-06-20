package org.openconcerto.modules.onedrive.networking;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;

import org.openconcerto.modules.onedrive.exceptions.OneDriveInternalException;

import com.sun.istack.internal.Nullable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.google.common.base.Charsets;


enum CallbackResponse { code, error };

public final class OneDriveAuthServer {
	private final Semaphore authLock;
	private HttpServer server = null;
	private int port = 4444;
	private String host = "localhost";
	private String authCode;

	public OneDriveAuthServer(Semaphore authLock) {
		this.authLock = authLock;

		try {
			authLock.acquire();
		}
		catch (InterruptedException e) {
			throw new OneDriveInternalException("Lock error in AuthServer constructor.", e);
		}
		
		try {
			server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/", new  OneDriveAuthHandler());
			server.setExecutor(null);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		if (server != null) server.start();
	}

	@Nullable
	public String close() {
		if (server != null) {
			server.stop(0);
			return authCode;
		}
		return null;
	}

	public String getCallbackURL() {
		return "http://" + host + ":" + port + "/";
	}
	
	private class  OneDriveAuthHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			byte[] response =
					"<script type='text/javascript'>window.close();</script><style>.wrapper { width:300px; height:50px; position: fixed; top: 50%; left: 50%; margin-top: -25px; margin-left: -150px; text-align: center;}</style><div class=\"wrapper\">Vous pouvez fermer cette fenêtre et poursuivre sur OpenConcerto.</div>".getBytes(Charsets.UTF_8);
			httpExchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			httpExchange.sendResponseHeaders(200, response.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(response);
			os.close();
			System.out.println(httpExchange.getRequestURI().getQuery());
			String[] query = httpExchange.getRequestURI().getQuery().split("=");

			switch (CallbackResponse.valueOf(query[0])) {
				case code:
					authCode = query[1];
					break;
				case error:
					authLock.release();
					throw new IOException("Wrong Login Info");
				default:
					authLock.release();
					throw new IOException("Unrecognized OneDrive Server Error");
			}

			authLock.release();
		}
	}
}