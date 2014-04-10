package com.buino.server;

import java.util.List;

import org.apache.log4j.Logger;

import com.buino.server.Build.BuildStatus;
import com.buino.server.call.ParseCaller;
import com.buino.server.call.ParseException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Does most of the work
 * 
 */
public final class BuildNotificationManager {

	/**
	 * Reminders are sent out every half an hour
	 */
	private static final int SECONDS_TO_REMIND = 60 * 60;

	private static Logger LOG = Logger.getLogger(BuildNotificationManager.class);

	private final Gson gson;
	private final ParseCaller parseCaller;
	private final MemoryLogStorage memoryLog;
	private final AlarmScheduler alarmScheduler;

	public BuildNotificationManager(final MemoryLogStorage storage) {
		gson = new GsonBuilder().setPrettyPrinting().create();
		this.memoryLog = storage;
		this.parseCaller = new ParseCaller(gson);
		this.alarmScheduler = new AlarmScheduler(SECONDS_TO_REMIND, parseCaller, memoryLog);
		memoryLog.setReminderIntervalSeconds(SECONDS_TO_REMIND);
	}

	/**
	 * Checks if there was a status update
	 * 
	 * @throws ParseException
	 */
	private boolean didStatusUpdate(final String buildName, final BuildStatus oldStatus)
			throws ParseException {
		final List<Build> allBuilds = parseCaller.getAllBuildsWithName(buildName);
		if (allBuilds.size() != 1) {
			throw new ParseException(
					"Tried to check for status update but somehow there exist more (or less) than one builds named \""
						+ buildName + "\". Broadcast process aborted");
		} else if (oldStatus == null) {
			// new build TODO handle?
			return false;
		}
		return !oldStatus.equals(allBuilds.get(0).getStatus());
	}

	public void updateBuild(final Build newBuild) throws ParseException {
		LOG.info("Updating Parse API backend...");

		// Synchronize build objects
		final String buildName = newBuild.getName();
		boolean wasBuildUpdated = false;
		BuildStatus oldStatus = null;
		final List<Build> allBuilds = parseCaller.getAllBuildsWithName(buildName);
		if (allBuilds.size() == 1) {
			oldStatus = allBuilds.get(0).getStatus();
			wasBuildUpdated = parseCaller.updateBuild(newBuild);
			if (wasBuildUpdated) {
				memoryLog
						.logInfo("Succesfully updated build \"" + buildName + "\" on the backend.");
			} else {
				memoryLog.logInfo("Build \"" + buildName
					+ "\" information was up-to-date, no backed update required.");
			}
		} else {
			boolean manyExisted = false;
			if (allBuilds.size() > 1) {
				manyExisted = true;
				parseCaller.deleteAllBuildsByName(buildName);
				memoryLog.logWarn("Apparently many builds named \"" + buildName
					+ "\" existed. They were all delated and the new one replaced them all.");
			}
			parseCaller.createBuildObject(newBuild);
			if (!manyExisted) {
				memoryLog.logInfo("Build \"" + buildName + "\" did not exist and is now created.");
			}
		}

		// Broadcast message!
		if (didStatusUpdate(buildName, oldStatus)
			&& parseCaller.broadcastInformationAboutBuild(newBuild)) {
			final int deviceNumber = parseCaller.getAllFrotnedsSubscribedTo(buildName).size();
			memoryLog.logInfo("Message has been broadcasted to " + deviceNumber
				+ " intallation(s).");
			// check if should start scheduling
			if (newBuild.getStatus().equals(BuildStatus.FAILURE)) {
				alarmScheduler.startForBuild(newBuild);
			}
		} else {
			memoryLog.logInfo("No messages are to be broadcast.");
		}
		memoryLog.logInfo("Build report handling completed.");
		memoryLog.logNewLine();
	}

}
