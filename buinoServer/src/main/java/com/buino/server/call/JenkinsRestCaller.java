package com.buino.server.call;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.buino.server.Build;

/**
 * Communicates with jenkins via rest to extract information
 * 
 */
public final class JenkinsRestCaller {

	/**
	 * Query with xpath
	 */
	private static final String JENKINS_API_SUFFIX =
		"api/xml?xpath=/*/changeSet/item[1]/author/fullName";

	private static final String JENKINS_API_USERNAME = "share";
	private static final String JENKINS_API_TOKEN = "6b622ca8e6221a10f39ae2518f197920";

	/**
	 * Gets the name of the (last) contributor of the build
	 * 
	 * @throws ResourceException
	 */
	public static String getAuthorName(final Build newBuild) throws ResourceException {
		try {
			// first, get the URL out
			final String jenkinsApiUrl = newBuild.getUrl() + JENKINS_API_SUFFIX;
			// then call the API
			final ClientResource buildResourcejenkinsApi = new ClientResource(jenkinsApiUrl);
			buildResourcejenkinsApi.setReferrerRef("http://theo.servebeer.com");
			buildResourcejenkinsApi.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
					JENKINS_API_USERNAME, JENKINS_API_TOKEN);
			final String response = StringUtils.trim(buildResourcejenkinsApi.get().getText());
			// get and return the name
			final String name = StringUtils.substringBetween(response, ">", "<");
			return name;
		} catch (final IOException e) {
			throw new ResourceException(e);
		}

	}

}
