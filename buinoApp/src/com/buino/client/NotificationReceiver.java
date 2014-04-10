package com.buino.client;

import java.util.Date;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.preference.PreferenceManager;
import android.util.Log;

import com.buino.client.activity.MainActivity;
import com.buino.client.data.Build;
import com.buino.client.data.Build.BuildStatus;
import com.buino.client.util.JsonUtils;

public class NotificationReceiver extends BroadcastReceiver {

	private static final String BUILD_UPDATE_ACTION = "com.buino.BUILD_UPDATE";
	private static final String BUILD_BROKEN_REMINDER_ACTION =
		"com.buino.BUILD_BROKEN_REMINDER_UPDATE";

	private static final String BUILD_DATA_PARCEL = "com.parse.Data";
	private static final String BUILD_AUDIO_MESSAGE = "message";
	private static final String BUILD_NAME = "name";
	public static final String INTENT_EXTRA_SPEECH_MESSAGE = "textToSpeak";
	private static final String TAG = "NotificationReceiver";

	/**
	 * For closures
	 */
	public final NotificationReceiver getReferenceToReceiver() {
		return this;
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		try {
			final String action = intent.getAction();
			Log.d(TAG, "Received intent with action " + action + "");

			if (action.equals(BUILD_UPDATE_ACTION)) {
				// get it
				final String buildAsJsonString = intent.getExtras().getString(BUILD_DATA_PARCEL);
				final String message =
					new JSONObject(buildAsJsonString).getString(BUILD_AUDIO_MESSAGE);
				final Build build = JsonUtils.fromJsonString(buildAsJsonString);
				build.setDateOfUpdate(new Date()); // hack?

				// update it on the memory (fake db)
				final BuinoApplication mainApplication =
					(BuinoApplication) context.getApplicationContext();
				mainApplication.updateBuild(build);

				// and try to notify the live interface
				final Intent intentLiveUpdate = new Intent(MainActivity.ACTION_LIVE_UPDATE);
				intentLiveUpdate.putExtra(MainActivity.INTENT_EXTRA_SINGLE_BUILD,
						JsonUtils.toJsonString(build));
				intentLiveUpdate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.sendBroadcast(intentLiveUpdate);

				// and play sound
				tryPlaySounds(context, build.getName(), build.getStatus(), message);
			} else if (action.equals(BUILD_BROKEN_REMINDER_ACTION)) { // play only the sound
				final String reminderData = intent.getExtras().getString(BUILD_DATA_PARCEL);
				final String message =
					new JSONObject(reminderData).getString(BUILD_AUDIO_MESSAGE);
				final String buildName = new JSONObject(reminderData).getString(BUILD_NAME);
				tryPlaySounds(context, buildName, BuildStatus.FAILURE, message);
			}

		} catch (final Exception e) {
			Log.e(TAG, "JSONException: " + e.getMessage());
		}
	}

	private void tryPlaySounds(final Context context, final String buildName,
			final BuildStatus status, final String message) {
		// is main foreground? if so, it will handle it - CHANGED - this is always handled in background
// if(((BuinoApplication)context.getApplicationContext()).isMainForeground()) {
// return;
// }
		// check for permissions
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				buildName + MainActivity.PREF_BUILD_SOUND_PREFIX, false)) {
			return;
		}

		// check for failure in order to play alarm
		if (status.equals(BuildStatus.FAILURE)) {
			final MediaPlayer mp = MediaPlayer.create(context, R.raw.short_alarm);
			mp.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(final MediaPlayer mp) {
					mp.release();
					tryStartTTS(context, message);
				}
			});
			mp.start();
		} else {
			// dont play alarm, only tts
			tryStartTTS(context, message);
		}
	}

	private void tryStartTTS(final Context context, final String message) {
		final Intent intentForTTS = new Intent(context, BuinoTTS.class);
		intentForTTS.putExtra(INTENT_EXTRA_SPEECH_MESSAGE, message);
		context.startService(intentForTTS);
	}
}
