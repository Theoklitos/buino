package com.buino.client;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.util.Log;

import com.buino.client.data.Build;
import com.buino.client.util.ParseUtils;
import com.parse.Parse;
import com.parse.ParseACL;

public class BuinoApplication extends Application {

	private static final String PARSE_CLIENT_ID = "2CuLPOYquXN2OrM4uoWYzDtuPRDlgxSaggW3vZi2";
	private static final String PARSE_APP_ID = "SM9B4TIUOgTKvRJ1qTQcYv4XC62x8ferrgcyhnPZ";

	private List<Build> buildsStoredInMemory;
	private boolean isMainForeground;
	/**
	 * Returns everything in memory (updated from the startup or notification)
	 */
	public synchronized List<Build> getAllBuildsInMemory() {
		return buildsStoredInMemory;
	}
	
	public synchronized boolean isMainForeground() {
		return isMainForeground;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		Parse.initialize(this, PARSE_APP_ID, PARSE_CLIENT_ID);
		
		buildsStoredInMemory = new ArrayList<Build>();
		
		final ParseACL defaultACL = new ParseACL();
		ParseACL.setDefaultACL(defaultACL, true);		
	}
	
	@Override
	public void onTerminate() {
		Log.d("application", "terminate called!");
		ParseUtils.unsubscribeInstallationFromAll(this);
	}
	
	
	public synchronized void setMainForeground(final boolean isForeground) {
		isMainForeground = isForeground;
	}	
	
	/**
	 * Adds the build to the list of builds in memory, or simply updates it if its there
	 * 
	 * @return true if data was changed, false otherwise
	 */
	public synchronized boolean updateBuild(final Build build) {
		final String buildName = build.getName();
		for(final Build storedBuild : buildsStoredInMemory) {
			if(storedBuild.getName().equals(buildName)) {
				// update!
				return storedBuild.merge(build);				
			}
		}
		
		// if we went up to here, create a new one
		return buildsStoredInMemory.add(build);		
	}

}
