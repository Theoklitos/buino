package com.buino.server.main;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.buino.server.servlet.LiveLogServlet;
import com.buino.server.servlet.NotificationServlet;

/**
 * Main class that starts up everything
 */
public final class ServerStartup {
		
	public static final void main(final String args[]) throws Exception {		
		final Server server = new Server(20667);
        final ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/"); 
        handler.addServlet(NotificationServlet.class, "/buino");
        handler.addServlet(LiveLogServlet.class, "/buino/log");
        server.setHandler(handler);
        server.start();		
	}

}
