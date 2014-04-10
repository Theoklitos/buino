package com.buino.client.util;

import java.util.List;

import android.content.Context;
import android.widget.CheckBox;

import com.buino.client.BuinoApplication;
import com.buino.client.R;
import com.buino.client.activity.MainActivity;
import com.buino.client.activity.RegisterActivity;
import com.buino.client.data.Build;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.PushService;
import com.parse.SaveCallback;

/**
 * Misc utils when dealing with parse.com
 * 
 * @author takis
 * 
 */
public final class ParseUtils {

	private static void disableAllCheckboxes(final Context context) {
		final MainActivity mainActivity = (MainActivity) context;
		mainActivity.setAllCheckboxesEnabled(false);
	}

	private static void enableAllCheckboxes(final Context context) {
		final MainActivity mainActivity = (MainActivity) context;
		mainActivity.setAllCheckboxesEnabled(true);
	}

	/**
	 * Subscribes the user to the given build and the installation also
	 */
	public static void subscribeToBuildAndSync(final String buildName, final Context context,
			final CheckBox checkbox) {
		disableAllCheckboxes(context);
		final ParseUser currentUser = ParseUser.getCurrentUser();
		final List<String> allBuilds =
			currentUser.getList(RegisterActivity.PARSE_USER_BUILDS_FIELD);
		if (allBuilds == null) {
			// anonymous
			PushService.subscribe(context, buildName, MainActivity.class, R.drawable.small_icon);
			enableAllCheckboxes(context);
			return;
		}
		if (!allBuilds.contains(buildName)) {
			currentUser.add(RegisterActivity.PARSE_USER_BUILDS_FIELD, buildName);
			currentUser.saveInBackground(new SaveCallback() {
				@Override
				public void done(final ParseException e) {
					if (e == null) {
						PushService.subscribe(context, buildName, MainActivity.class,
								R.drawable.small_icon);
						enableAllCheckboxes(context);
					} else {
						PopUpUtils.showErrorMessage(context,
								"Could not subscribe: " + e.getMessage());
						if (checkbox != null) {
							checkbox.setChecked(false);
						}
					}
				}
			});
		}
	}

	public static void syncSubscriptionsFromUserData(final Context context) {
		final List<String> allBuilds =
			ParseUser.getCurrentUser().getList(RegisterActivity.PARSE_USER_BUILDS_FIELD);
		if (allBuilds != null) { // user could be anonymous
			for (final String build : allBuilds) {
				PushService.subscribe(context, build, MainActivity.class, R.drawable.small_icon);
			}
		}
	}

	// Removes all the subscriptions in this installation
	public static void unsubscribeInstallationFromAll(final Context context) {
		final BuinoApplication buinoApp = (BuinoApplication) context.getApplicationContext();
		for (final Build existingBuild : buinoApp.getAllBuildsInMemory()) {
			PushService.unsubscribe(context, existingBuild.getName());
		}
	}

	/**
	 * Unsubscribes the user from the given build and the installation also
	 */
	public static void unsubscribeToBuildAndSync(final String buildName, final Context context,
			final CheckBox checkbox) {
		disableAllCheckboxes(context);
		final ParseUser currentUser = ParseUser.getCurrentUser();
		final List<String> allBuilds =
			currentUser.getList(RegisterActivity.PARSE_USER_BUILDS_FIELD);
		if (allBuilds == null) {
			// anonymous
			PushService.unsubscribe(context, buildName);
			enableAllCheckboxes(context);
			return;
		}
		if (allBuilds.remove(buildName)) {
			currentUser.remove(RegisterActivity.PARSE_USER_BUILDS_FIELD);
			currentUser.addAll(RegisterActivity.PARSE_USER_BUILDS_FIELD, allBuilds);
			currentUser.saveInBackground(new SaveCallback() {
				@Override
				public void done(final ParseException e) {
					if (e == null) {
						PushService.unsubscribe(context, buildName);
						enableAllCheckboxes(context);
					} else {
						PopUpUtils.showErrorMessage(context,
								"Could not unsubscribe: " + e.getMessage());
						if (checkbox != null) {
							checkbox.setChecked(true);
						}
					}
				}
			});
		}

	}
}
