package com.buino.server;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Is a sort of a storage in-memory for various statistics and loging output.
 * 
 * @author takis
 * 
 */
public final class MemoryLogStorage {

	private static Logger LOG = Logger.getLogger(MemoryLogStorage.class);

	private final StringBuffer logOutput;

	private String dateTimeOfStartup;
	private int numberOfBuildUpdates;
	private int secondsToRemind;

	public MemoryLogStorage() {
		logOutput = new StringBuffer();
		numberOfBuildUpdates = 0;
	}

	/**
	 * Returns everything logged so far
	 */
	public String getAllLogOutput() {
		return logOutput.toString();
	}

	/**
	 * Self-explanatory
	 */
	public String getDateTimeOfStartup() {
		return dateTimeOfStartup;
	}

	/**
	 * Self-explanatory
	 */
	public int getNumberOfBuildUpdatesReceived() {
		return numberOfBuildUpdates;
	}

	/**
	 * Returns a new DateTime printed in a nice format
	 */
	private String getPrettyDateTimeNow() {
		final DateTime dt = new DateTime();
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MMM, HH:mm:ss");
		final String dateTimeString = formatter.print(dt);
		return dateTimeString;
	}

	/**
	 * Returns (in seconds) the interval every after the broken build reminder is broadcast, if any
	 * 
	 */
	public int getReminderIntervalSeconds() {
		return secondsToRemind;
	}

	/**
	 * log a line of type ERROR
	 */
	public void logError(final String error) {
		LOG.error(error);
		logOutput.append(getPrettyDateTimeNow() + " [ERROR] : " + error + "<br/>");
		logNewLine();
	}

	/**
	 * Log a line of type INFO
	 */
	public void logInfo(final String message) {
		LOG.info(message);
		logOutput.append(getPrettyDateTimeNow() + " [INFO] : " + message + "<br/>");
	}

	/**
	 * Adds a line (with an endline in the end) to the log kept in memory
	 */
	public void logNewIncomingBuild(final Build build) {
		final String logLine = "New report arived! " + build.toString();
		LOG.info(logLine);
		logOutput.append(getPrettyDateTimeNow() + " [INFO] : " + logLine + "<br/>");
		numberOfBuildUpdates++;
	}

	/**
	 * Self-explanatory
	 */
	public void logNewLine() {
		logOutput.append("<br/>");
	}

	/**
	 * Logs and stores the startup time
	 */
	public void logServletStartup() {
		final String message = "Buino notification servlet startup.";
		LOG.info(message);
		final String serverStartupTime = getPrettyDateTimeNow();
		logOutput.append(serverStartupTime + " [INFO] : " + message + "<br/>");
		logNewLine();
		dateTimeOfStartup = serverStartupTime;
	}

	/**
	 * Log a line of type WARN
	 */
	public void logWarn(final String message) {
		LOG.warn(message);
		logOutput.append(getPrettyDateTimeNow() + " [WARN] : " + message + "<br/>");
	}

	/**
	 * Defines every how many seconds the reminder will trigger
	 */
	public void setReminderIntervalSeconds(final int secondsToRemind) {
		this.secondsToRemind = secondsToRemind;
	}

}
