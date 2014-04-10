package com.buino.server;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.buino.server.Build.BuildStatus;
import com.buino.server.call.ParseCaller;

/**
 * Repeats alarms every
 * 
 * @author takis
 * 
 */
public final class AlarmScheduler {

	private final long millisecondsInterval;
	private final ParseCaller parseCaller;
	private final MemoryLogStorage log;

	public AlarmScheduler(final int secondsInterval, final ParseCaller parseCaller,
			final MemoryLogStorage log) {
		this.log = log;
		this.millisecondsInterval = secondsInterval * 1000;
		this.parseCaller = parseCaller;
	}

	/**
	 * Starts a timer that will alert/remind everyone every INTERVAL about the broken build until it is fixed
	 */
	public void startForBuild(final Build build) {
		final String buildName = build.getName();
		if (!build.getStatus().equals(BuildStatus.FAILURE)) {
			return;
		} else {
			log.logInfo("Broken build reminder started for build \"" + buildName + "\".");
		}
		final Timer timer = new Timer(buildName);		
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				log.logInfo("Reminder! Time interval passed for build \"" + buildName + "\", checking if should alert...");
				try {
					// check if broken
					final List<Build> targetBuild = parseCaller.getAllBuildsWithName(buildName);
					if (targetBuild.size() != 1) {
						throw new Exception("Wrong number of builds named \"" + buildName
							+ "\" found: " + targetBuild);
					}
					final Build currentBuild = targetBuild.get(0);

					// if broken, broadcast
					if (currentBuild.getStatus().equals(BuildStatus.FAILURE)) {
						log.logInfo("Build is still broken, reminder alert triggered.");
						parseCaller.broadcastBrokenBuildReminder(currentBuild);
					} else if (currentBuild.getStatus().equals(BuildStatus.SUCCESS)) {
						log.logInfo("Build \"" + buildName + "\" was repaired, reminder terminated.");
						timer.cancel();
					} else {
						log.logWarn("Build reminder-timer triggered with weird build status: "
							+ currentBuild.getStatus());
					}
				} catch (final Exception e) {
					log.logError("Error while performing broken build reminder, timer canceled: "
						+ e.getMessage());
					timer.cancel();
				}
			}
		}, millisecondsInterval, millisecondsInterval);		
	}
}
