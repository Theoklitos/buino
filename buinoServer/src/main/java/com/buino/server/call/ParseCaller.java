package com.buino.server.call;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.engine.header.Header;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import com.buino.server.Build;
import com.buino.server.Build.BuildStatus;
import com.buino.server.BuinoJSONUtils;
import com.buino.server.Frontend;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Calls the backend at Parser.com
 * 
 * @author takis
 * 
 */
public final class ParseCaller {
	
	private static Logger LOG = Logger.getLogger(ParseCaller.class);

	private static final String PARSE_REST_KEY = "9Du1OsRqwbNQ5hToMKFnuCLGzmPZb3D5cAQNORcl";
	private static final String PARSE_APP_ID_KEY = "SM9B4TIUOgTKvRJ1qTQcYv4XC62x8ferrgcyhnPZ";
	private static final String PARSE_MASTER_KEY = "TV4hHAVFQtlj8OzcqD0sg1JSHLOME9HW879E8afT";

	private static final String QUERY_RESPONSE_RESULTS_KEY = "results";
	private final static String PARSE_BUILD_CLASS_NAME = "Build";
	private static final String PARSE_OBJECT_ID_PARAM_NAME = "objectId";

	private final static String PARSE_API_BASE_URL = "https://api.parse.com/1/";
	private final static String PARSE_API_OBJECT_URL = PARSE_API_BASE_URL + "classes/"
		+ PARSE_BUILD_CLASS_NAME;
	private final static String PARSE_API_INSTALLATION_URL = PARSE_API_BASE_URL + "installations";
	private final static String PARSE_API_PUSH_URL = PARSE_API_BASE_URL + "push";

	private static final String HEADERS_KEY = "org.restlet.http.headers";

	/**
	 * For quick testing
	 * 
	 * @throws IOException
	 */
	public static final void main(final String args[]) throws ParseException {
		final ParseCaller caller = new ParseCaller(new GsonBuilder().setPrettyPrinting().create());
		// System.out.println(caller.getAllFrontends());
		caller.broadcastInformationAboutBuild(new Build("crap.com", "fitness", 1234,
				BuildStatus.SUCCESS));
	}

	private final Gson gson;
	private ClientResource objectsResource;
	private ClientResource installationsResource;
	private ClientResource pushNotificationsResource;

	public ParseCaller(final Gson gson) {
		this.gson = gson;
	}

	/**
	 * Adds the parse authentication/authorization headers *
	 */
	@SuppressWarnings("unchecked")
	private void addHeadersToClientResource(final ClientResource parseResource,
			final boolean useMasterKey) {
		final Map<String, Object> attributes = parseResource.getRequestAttributes();
		Series<Header> headers = (Series<Header>) attributes.get(HEADERS_KEY);
		if (headers == null) {
			headers = new Series<Header>(Header.class);
			attributes.put(HEADERS_KEY, headers);
		}
		headers.add("X-Parse-Application-Id", PARSE_APP_ID_KEY);
		if (useMasterKey) {
			headers.add("X-Parse-Master-Key", PARSE_MASTER_KEY);
		} else {
			headers.add("X-Parse-REST-API-Key", PARSE_REST_KEY);
		}
		headers.add("Content-Type", "application/json");
	}

	/**
	 * Nags all the subscribed installations that this build is still broken!
	 * 
	 * @throws ParseException 
	 */
	public void broadcastBrokenBuildReminder(final Build build) throws ParseException {
		if (!build.getStatus().equals(BuildStatus.FAILURE)) {
				return;
			}
			try {
				// Unfortunately we use the java's connection way because rest ClientResource is buggy
				final String reminderAsString =
					BuinoJSONUtils.createReminderNotificationObject(build).toString(5);
				LOG.debug("Sending reminder:\n" + reminderAsString);
				final URL url = new URL(PARSE_API_PUSH_URL);
				final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("X-Parse-Application-Id", PARSE_APP_ID_KEY);
				connection.setRequestProperty("X-Parse-REST-API-Key", PARSE_REST_KEY);
				connection.setRequestProperty("Content-Type", "application/json");
				final OutputStream output = connection.getOutputStream();
				output.write(reminderAsString.getBytes());
				output.flush();
				output.close();
				connection.getResponseCode(); // apparently this is needed			
				connection.disconnect();
			} catch (final Exception e) {
				throw new ParseException(e);
			}
	}

	/**
	 * Will broadcast to all the subscribed users information about this build.
	 * 
	 * @return True if the broadcast occured, false if the status was not a broadcastable one.
	 *  
	 * @throws ParseException
	 */
	public boolean broadcastInformationAboutBuild(final Build build) throws ParseException {
		if (!build.getStatus().equals(BuildStatus.SUCCESS)
			&& !build.getStatus().equals(BuildStatus.FAILURE)) {
			return false;
		}
		try {
			// Unfortunately we use the java's connection way because rest ClientResource is buggy
			final String notificationAsString =
				BuinoJSONUtils.createNotificationObject(build).toString(5);
			LOG.debug("Sending notification:\n" + notificationAsString);
			final URL url = new URL(PARSE_API_PUSH_URL);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("X-Parse-Application-Id", PARSE_APP_ID_KEY);
			connection.setRequestProperty("X-Parse-REST-API-Key", PARSE_REST_KEY);
			connection.setRequestProperty("Content-Type", "application/json");
			final OutputStream output = connection.getOutputStream();
			output.write(notificationAsString.getBytes());
			output.flush();
			output.close();
			connection.getResponseCode(); // apparently this is needed			
			connection.disconnect();
		} catch (final Exception e) {
			throw new ParseException(e);
		}
		return true;
	}

	/**
	 * Checks if one ore more builds with this name exists and returns true/false accordingly
	 * 
	 * @throws ParseException
	 */
	public boolean checkIfBuildExists(final String buildName) throws ParseException {
		final List<JSONObject> queryResult = getAllJsonBuildsWithName(buildName);
		return queryResult.size() != 0;
	}

	/**
	 * Creates a new object (type: Build) in the backend
	 */
	public void createBuildObject(final Build build) throws ParseException {
		final ClientResource objectsResource = getObjectClientResource();
		try {
			final String buildAsJson = gson.toJson(build);
			objectsResource.post(buildAsJson);
		} catch (final Exception e) {
			throw new ParseException(e);
		}
	}

	private ClientResource createInstallationResource() {
		final Client client = new Client(new Context(), Protocol.HTTPS);
		final ClientResource resource = new ClientResource(PARSE_API_INSTALLATION_URL);
		addHeadersToClientResource(resource, true);
		resource.setNext(client);
		return resource;
	}

	/**
	 * Constructs a workable {@link ClientResource}s with headers and connectors
	 */
	private ClientResource createResourceForUrl(final String Url) {
		final Client client = new Client(new Context(), Protocol.HTTPS);
		final ClientResource resource = new ClientResource(Url);
		addHeadersToClientResource(resource, false);
		resource.setNext(client);
		return resource;
	}

	/**
	 * Deletes all the builds that have the given "name" field
	 * 
	 * @throws ParseException
	 */
	public void deleteAllBuildsByName(final String buildName) throws ParseException {
		final List<JSONObject> allBuilds = getAllJsonBuildsWithName(buildName);
		if (allBuilds.size() == 0) {
			return;
		}
		try {
			for (final JSONObject buildJson : allBuilds) {
				final ClientResource deleteResource =
					createResourceForUrl(PARSE_API_OBJECT_URL + "/"
						+ buildJson.getString(PARSE_OBJECT_ID_PARAM_NAME));
				deleteResource.delete();
			}
		} catch (final JSONException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Returns all the builds from the database that have the given name
	 */
	public List<Build> getAllBuildsWithName(final String buildName) throws ParseException {
		final List<Build> result = new ArrayList<Build>();
		for (final JSONObject buildJson : getAllJsonBuildsWithName(buildName)) {
			try {
				result.add(BuinoJSONUtils.parseFromParse(buildJson));
			} catch (final JSONException e) {
				throw new ParseException(e);
			}
		}
		return result;
	}

	/**
	 * Returns all the "installations" of the buino client.
	 */
	public List<Frontend> getAllFrontends() throws ParseException {		
		final ClientResource getAllResource = getInstallationClientResource();
		final List<Frontend> result = new ArrayList<Frontend>();
		try {
			final String resultAsString = getAllResource.get().getText();
			final JSONArray queryResult =
				new JSONObject(resultAsString).getJSONArray(QUERY_RESPONSE_RESULTS_KEY);
			for (int i = 0; i < queryResult.length(); i++) {
				final JSONObject frontendAsJson = queryResult.getJSONObject(i);
				result.add(BuinoJSONUtils.getFrontendFromJson(frontendAsJson));
			}
		} catch (final Exception e) {
			throw new ParseException("Error while creating Frontend object: " + e);
		}
		return result;
	}

	/**
	 * Returns all the installations that are subscribed to the channel's name
	 * 
	 * @throws ParseException
	 */
	public List<Frontend> getAllFrotnedsSubscribedTo(final String buildName) throws ParseException {
		final List<Frontend> result = new ArrayList<Frontend>();
		final List<Frontend> allFrontends = getAllFrontends();		
		for (final Frontend installation : allFrontends) {
			if (installation.getSubscriptions().contains(buildName)) {
				result.add(installation);
			}
		}
		return result;
	}

	/**
	 * Internal method, returns the builds as jsons
	 */
	private List<JSONObject> getAllJsonBuildsWithName(final String buildName) throws ParseException {
		final ClientResource getAllResource = getObjectClientResource();
		getAllResource.setQueryValue("where", "{\"name\":\"" + buildName + "\"}");
		try {
			final String resultAsString = getAllResource.get().getText();
			final JSONArray queryResult =
				new JSONObject(resultAsString).getJSONArray(QUERY_RESPONSE_RESULTS_KEY);
			final List<JSONObject> result = new ArrayList<JSONObject>();
			for (int i = 0; i < queryResult.length(); i++) {
				result.add(queryResult.getJSONObject(i));
			}
			return result;
		} catch (final Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Lazy initialization
	 */
	private ClientResource getInstallationClientResource() {
		if (installationsResource == null) {
			installationsResource = createInstallationResource();
		}
		return installationsResource;
	}

	/**
	 * Lazy initialization
	 */
	private ClientResource getObjectClientResource() {
		if (objectsResource == null) {
			objectsResource = createResourceForUrl(PARSE_API_OBJECT_URL);
		}
		return objectsResource;
	}

	/**
	 * Lazy initialization. Not used! Using the java connection way.
	 */
	protected ClientResource getPushNotificationResource() {
		if (pushNotificationsResource == null) {
			pushNotificationsResource = createResourceForUrl(PARSE_API_PUSH_URL);
		}
		return pushNotificationsResource;
	}

	/**
	 * Updates the build thta has the same name with the given one, with the data of the given object
	 * 
	 * @return boolean true if the object was indeed modified or false if it needed no changes
	 * @throws ParseException
	 *             There must be only 1 build with such a name in the database
	 */
	public boolean updateBuild(final Build build) throws ParseException {
		// first get it
		final List<JSONObject> allBuilds = getAllJsonBuildsWithName(build.getName());
		if (allBuilds.size() > 1) {
			throw new ParseException("Tried to update build with name \"" + build.getName()
				+ "\" but many such builds exist!");
		}
		String objectId = "";
		try {
			objectId = allBuilds.get(0).getString(PARSE_OBJECT_ID_PARAM_NAME);
			// check for change
			if (BuinoJSONUtils.matchParseWithBean(allBuilds.get(0), build)) {
				return false;
			} else {
				final ClientResource updateResource =
					createResourceForUrl(PARSE_API_OBJECT_URL + "/" + objectId);
				final String buildAsJson = gson.toJson(build);
				updateResource.put(buildAsJson);
				return true;
			}
		} catch (final JSONException e) {
			throw new ParseException(e);
		}
	}

}
