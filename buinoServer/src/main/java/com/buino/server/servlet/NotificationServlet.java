package com.buino.server.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.buino.server.Build;
import com.buino.server.Build.BuildStatus;
import com.buino.server.BuildNotificationManager;
import com.buino.server.BuinoJSONUtils;
import com.buino.server.MemoryLogStorage;
import com.buino.server.call.JenkinsRestCaller;
import com.buino.server.call.ParseException;

public final class NotificationServlet extends HttpServlet {

	public static final String STORAGE_CONTEXT_ATTRIBUTE_NAME = "storage";

	private static final long serialVersionUID = 1L;

	private BuildNotificationManager manager;
	private MemoryLogStorage memoryLog;

	/**
	 * Returns a reference to the {@link MemoryLogStorage}
	 */
	public MemoryLogStorage getStorage() {
		if (memoryLog == null) {
			memoryLog = new MemoryLogStorage();
			getServletContext().setAttribute(STORAGE_CONTEXT_ATTRIBUTE_NAME, memoryLog);
		}
		return memoryLog;
	}
	
	@Override
	public void init() {
		memoryLog = new MemoryLogStorage();
		manager = new BuildNotificationManager(memoryLog);
		getServletContext().setAttribute(STORAGE_CONTEXT_ATTRIBUTE_NAME, memoryLog);
		memoryLog.logServletStartup();
	}

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		final String requestText = request.getParameterNames().nextElement();

		// we receive the message from jenkins
		Build newBuild = null;
		try {
			final JSONObject jenkinsReportdAsJson = BuinoJSONUtils.getJsonFromRequest(requestText);
			newBuild = BuinoJSONUtils.parseFromJenkins(jenkinsReportdAsJson);
		} catch (final JSONException e) {
			memoryLog.logError("Error while parsing jenkins message, build not updated. Error: "
				+ e.getMessage() + " Request was:\n" + tryGetClearRequest(requestText));
			e.printStackTrace();
			throw new IOException(e);
		}

		memoryLog.logNewIncomingBuild(newBuild);		

		if (newBuild.getStatus().equals(BuildStatus.SUCCESS)
			|| newBuild.getStatus().equals(BuildStatus.FAILURE)) {
			// we get the name of the author from jenkins
			String authorName = "";
			try {
				authorName = JenkinsRestCaller.getAuthorName(newBuild);
				if (StringUtils.isBlank(authorName)) {
					throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
							"Author's name was apparently blank.");
				}
				memoryLog.logInfo("Jenkins informs us that the author of this build version was "
					+ authorName + ".");
			} catch (final ResourceException e) {
				memoryLog.logWarn("Could not get author's name: " + e.getMessage() + ".");
			}

			newBuild.setAuthor(authorName);
			try {
				manager.updateBuild(newBuild);
			} catch (final ParseException e) {
				memoryLog.logError("Error while communicating with the Parse API: " + e.getMessage() + ".");
				e.printStackTrace();
				throw new IOException(e);
			}
		} else {
			memoryLog.logInfo("No update scheduled for this build.");
		}
	}

	/**
	 * Tries to clean the request string
	 */
	private String tryGetClearRequest(final String requestText) {
		try {
		return BuinoJSONUtils.getJsonFromRequest(requestText).toString(5);
		} catch(final JSONException e) {
			return BuinoJSONUtils.replaceUglyCharacters(requestText);
		}
	}

}
