package com.buino.server;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.buino.server.Build.BuildStatus;
import com.google.gson.Gson;

/**
 * Used to parse and/or extract information from builds
 * 
 * @author takis
 * 
 */
public final class BuinoJSONUtils {

	private static final String BUILD_UPDATE_ACTION = "com.buino.BUILD_UPDATE";
	private static final String BUILD_BROKEN_REMINDER_ACTION =
		"com.buino.BUILD_BROKEN_REMINDER_UPDATE";

	private static final String PARSE_URL_PARAM_NAME = "url";
	public static final String JENKINS_URL_PARAM_NAME = "full_url";
	private static final String NUMBER_PARAM_NAME = "number";
	private static final String BUILD_OBJECT_NAME = "build";
	private static final String NAME_PARAM_NAME = "name";
	private static final String STATUS_PARAM_NAME = "status";
	private static final String PHASE_PARAM_NAME = "phase";
	private static final String AUTHOR_PARAM_NAME = "author";

	private static final String INSTALLATION_ID_PARAM_NAME = "installationId";
	private static final String CHANNELS_PARAM_NAME = "channels";

	/**
	 * Creates a json object that is suitable to be POSTed to the parse push notifications api
	 * 
	 * @throws JSONException
	 */
	public static JSONObject createNotificationObject(final Build build) throws JSONException {
		final JSONObject result = new JSONObject();

		// first create the "data" (build + alert)
		final String buildAsJson = new Gson().toJson(build);
		final String buildName = build.getName();
		final JSONObject data = new JSONObject(buildAsJson);
		data.put("action", BUILD_UPDATE_ACTION);
		String alertText = "";
		String message = "";
		if (build.getStatus().equals(BuildStatus.SUCCESS)) {
			if (build.isAuthorUnspecified()) {
				alertText = buildName + " build is back to normal!";
				message = alertText;
			} else {
				alertText = build.getAuthor() + " fixed the " + buildName + " build!";
				message = "A build is back to normal. " + alertText;
			}
		} else if (build.getStatus().equals(BuildStatus.FAILURE)) {
			if (build.isAuthorUnspecified()) {
				alertText = "Build " + buildName + " was broken!";
				message = "Warning. " + alertText;
			} else {
				alertText = build.getAuthor() + " broke the " + buildName + " build!";
				message = "Warning! " + alertText + ". I repeat, " + alertText + ".";
			}
		}
		data.put("alert", alertText);
		data.put("message", message);

		// then the the recipient channels
		final JSONArray channels = new JSONArray();
		channels.put(buildName);

		// address only android! Not yet
		final JSONObject target = new JSONObject();
		target.put("deviceType", "android");

		// bring it all together
		// result.put("where", target);
		result.put("channels", channels);
		result.put("data", data);
		return result;
	}

	/**
	 * Creates an alert that reminds installations that their build is broken
	 * 
	 * @throws JSONException
	 */
	public static JSONObject createReminderNotificationObject(final Build build)
			throws JSONException {
		final JSONObject result = new JSONObject();

		// create alert + (sound speech) message:
		final String buildName = build.getName();
		final JSONObject data = new JSONObject();
		data.put("name", buildName);
		data.put("action", BUILD_BROKEN_REMINDER_ACTION);
		String alertText = "";
		String message = "";
		if (build.getStatus().equals(BuildStatus.FAILURE)) {
			if (build.isAuthorUnspecified()) {
				alertText = "Build " + buildName + " is still broken!";
				message = "Warning. " + alertText + " Someone must fix it";
			} else {
				alertText = build.getAuthor() + " still hasn't fixed the " + buildName + " build!";
				message =
					"Warning! Build " + buildName + " is still broken! " + build.getAuthor()
						+ ", quickly fix the build you broke!";
			}
		} else {
			throw new JSONException("Asked to create a failure reminder for a non-failed build: "
				+ build.toString());
		}
		data.put("alert", alertText);
		data.put("message", message);

		// then the the recipient channels
		final JSONArray channels = new JSONArray();
		channels.put(buildName);
		result.put("channels", channels);
		result.put("data", data);
		return result;
	}

	/**
	 * Returns the build json nested object
	 * 
	 * @throws JSONException
	 */
	public static JSONObject getBuildObject(final JSONObject reportAsJson) throws JSONException {
		return reportAsJson.getJSONObject(BUILD_OBJECT_NAME);
	}

	/**
	 * Parses a json to a {@link Frontend}
	 * 
	 * @throws JSONException
	 */
	public static Frontend getFrontendFromJson(final JSONObject frontendAsJson)
			throws JSONException {
		final String installationId = frontendAsJson.getString(INSTALLATION_ID_PARAM_NAME);
		final String channels =
			StringUtils.substringBetween(frontendAsJson.getString(CHANNELS_PARAM_NAME), "[", "]");
		return new Frontend(installationId, channels);
	}

	/**
	 * Clears out annoying trailing and leading characters etc, so as to end up with normal json
	 * 
	 * @throws JSONException
	 */
	public static JSONObject getJsonFromRequest(final String requestText) throws JSONException {
		final String result = replaceUglyCharacters(requestText);
		return new JSONObject(result);
	}

	/**
	 * Matches the given json from the Parse API with the given bean. Returns true if they are the same. Do
	 * not enter a jenkins JSON or this won't work!
	 * 
	 * @throws JSONException
	 */
	public static boolean matchParseWithBean(final JSONObject jsonFromParse, final Build buildBean)
			throws JSONException {
		if (!buildBean.getName().equals(jsonFromParse.getString(NAME_PARAM_NAME))) {
			return false;
		}
		if (!(buildBean.getNumber() == jsonFromParse.getInt(NUMBER_PARAM_NAME))) {
			return false;
		}
		if (!buildBean.getUrl().equals(jsonFromParse.getString(PARSE_URL_PARAM_NAME))) {
			return false;
		}
		if (!buildBean.getAuthor().equals(jsonFromParse.getString(AUTHOR_PARAM_NAME))) {
			return false;
		}
		if (!buildBean.getStatus().toString().equals(jsonFromParse.getString(STATUS_PARAM_NAME))) {
			return false;
		}

		return true;
	}

	/**
	 * Creates a new build bean from a jenkins report
	 */
	public static Build parseFromJenkins(final JSONObject jenkinsReportdAsJson)
			throws JSONException {
		final JSONObject buildObject = getBuildObject(jenkinsReportdAsJson);
		final String fullUrl = buildObject.getString(JENKINS_URL_PARAM_NAME);
		final String name = jenkinsReportdAsJson.getString(NAME_PARAM_NAME);
		final int number = buildObject.getInt(NUMBER_PARAM_NAME);
		String statusString = "";
		try {
			statusString = buildObject.getString(STATUS_PARAM_NAME);
		} catch (final JSONException e) {
			statusString = buildObject.getString(PHASE_PARAM_NAME);
		}
		BuildStatus buildStatus = null;
		try {
			buildStatus = BuildStatus.valueOf(statusString);
		} catch (final IllegalArgumentException e) {
			throw new JSONException("Status \"" + statusString + " \" not understood.");
		}
		final Build result = new Build(fullUrl, name, number, buildStatus);
		return result;
	}

	/**
	 * Creates a new build bean from a Parse backend object
	 */
	public static Build parseFromParse(final JSONObject parseObject) throws JSONException {
		final String name = parseObject.getString(NAME_PARAM_NAME);
		final String url = parseObject.getString(PARSE_URL_PARAM_NAME);
		final String author = parseObject.getString(AUTHOR_PARAM_NAME);
		final int number = parseObject.getInt(NUMBER_PARAM_NAME);
		final String statusString = parseObject.getString(STATUS_PARAM_NAME);
		BuildStatus buildStatus = null;
		try {
			buildStatus = BuildStatus.valueOf(statusString);
		} catch (final IllegalArgumentException e) {
			throw new JSONException("Status \"" + statusString + " \" not understood.");
		}

		final Build result = new Build(url, name, number, buildStatus);
		result.setAuthor(author);
		return result;
	}

	/**
	 * Removes extra /s and "s
	 */
	public static String replaceUglyCharacters(final String requestText) {
		String result = requestText;
		result = StringUtils.replace(result, "\\\"", "\"");
		result = StringUtils.replace(result, "\\n", "");
		if (result.startsWith("\"")) {
			result = StringUtils.substring(result, 1);
			result = StringUtils.substring(result, 0, (result.length() - 1));
		}
		return result;
	}

}
