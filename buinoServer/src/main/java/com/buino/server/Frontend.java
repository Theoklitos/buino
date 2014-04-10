package com.buino.server;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

/**
 * Represents an installation of our device
 * 
 */
public final class Frontend {

	private final List<String> subscriptions;
	private final String installationId;

	public Frontend(final String installationId) {
		this(installationId, null);
	}

	public Frontend(final String installationId, final String channelsAsStringWithComaSeparators) {
		subscriptions = new ArrayList<String>();
		if (channelsAsStringWithComaSeparators != null) {
			setSubscription(channelsAsStringWithComaSeparators);
		}
		this.installationId = installationId;
	}

	/**
	 * Returns the installation id that uniquely identifies this frontend
	 * @return
	 */
	public String getInstallationId() {
		return installationId;
	}

	/**
	 * Returns all the channels this frontend is subscribed to
	 */
	public List<String> getSubscriptions() {
		return subscriptions;
	}

	/**
	 * A string with many values i.e. channel1,channel2 is set as the subscriptions of this frontned
	 */
	final void setSubscription(final String stringWithComaSeparators) {
		final StringTokenizer tokenizer = new StringTokenizer(stringWithComaSeparators, ",");
		while (tokenizer.hasMoreElements()) {
			final String subscription = tokenizer.nextElement().toString();
			subscriptions.add(StringUtils.substringBetween(subscription, "\""));
		}
	}
	
	@Override
	public String toString() {
		return "Frontend with installation id \"" + installationId + "\", subscribed to " + subscriptions + ".";
	}

}
