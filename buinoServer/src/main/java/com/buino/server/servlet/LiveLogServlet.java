package com.buino.server.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.buino.server.MemoryLogStorage;

public final class LiveLogServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(LiveLogServlet.class);

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		LOG.info("Live check request from " + request.getRemoteAddr() + ".");
		final MemoryLogStorage log = (MemoryLogStorage) getServletContext().getAttribute(NotificationServlet.STORAGE_CONTEXT_ATTRIBUTE_NAME);
		
		response.getWriter().append("<html><h2>Buino server is up and running!</h2><hr/>");
		response.getWriter().append("<br/>");
		if(log == null) {
			return;
		}
		response.getWriter().append("<div><b>Server live since:</b> " + log.getDateTimeOfStartup() + ".<br/>");
		response.getWriter().append("<b>Build updates tracked:</b> " + log.getNumberOfBuildUpdatesReceived() + ".<br/>");
		response.getWriter().append("<b>Reminder for broken build interval (seconds):</b> " + log.getReminderIntervalSeconds() + "s.<br/></div>");
				
		try {			
			response.getWriter().append("<div id=\"log\"><h3>Log output:</h3><div style=\"width:1000px;height:600px;overflow-y:scroll;overflow-x:hidden;border:1px solid #ccc;\">" + log.getAllLogOutput() + "</div></div></html>");
		} catch (final Exception e) {
			response.getWriter().append(
					"<span>Apparently there was an error getting the log and the stats: "
						+ e.getMessage() + ".</span>");
		}		
	}

}
