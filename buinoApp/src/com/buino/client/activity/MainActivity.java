package com.buino.client.activity;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.buino.client.BuildArrayAdapter;
import com.buino.client.BuinoApplication;
import com.buino.client.R;
import com.buino.client.data.Build;
import com.buino.client.util.JsonUtils;
import com.buino.client.util.ParseUtils;
import com.buino.client.util.PopUpUtils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.PushService;

public class MainActivity extends Activity {

	public static final String PREF_BUILD_SOUND_PREFIX = "_SOUND";
	public static final String ACTION_LIVE_UPDATE = "com.buino.LIVE_UPDATE";
	public static final String BUILD_CLASS_NAME_IN_BACKEND = "Build";
	public static final String INTENT_EXTRA_SINGLE_BUILD = "build";

	/**
	 * To reiceve notifications and update immediately
	 */
	private final BroadcastReceiver liveUpdater = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {

			// basically update it all, its ok.
			onStart();
		}
	};
	private IntentFilter liveIntentFilter;

	/**
	 * Used inside closures
	 */
	private MainActivity getReferenceToThisActivity() {
		return this;
	}

	/**
	 * Ask the user to log out
	 */
	@Override
	public void onBackPressed() {
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Sign Out");
		alertDialogBuilder
				.setMessage(
						"Are you sure you want to sign out? You won't receive any notifications!")
				.setCancelable(true)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						ParseUser.logOut();
						// also unsubscribe this device
						ParseUtils.unsubscribeInstallationFromAll(getReferenceToThisActivity());
						getReferenceToThisActivity().finish();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.dismiss();
					}
				});

		final AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate()");
		setContentView(R.layout.main_activity);
		setTitle(R.string.main_activity_label);

		final Bundle extras = getIntent().getExtras();

		final ListView buildList = (ListView) findViewById(R.id.buildList);
		final List<Build> allBuilds = new ArrayList<Build>();

		final List<String> allBuildsAsStringList =
			extras.getStringArrayList(WelcomeActivity.INTENT_EXTRA_ALL_BUILDS);
		if (allBuildsAsStringList != null) {
			for (final String buildAsString : allBuildsAsStringList) {
				allBuilds.add(JsonUtils.fromJsonString(buildAsString));
			}
		} else {
			allBuilds.addAll(((BuinoApplication) getApplicationContext()).getAllBuildsInMemory());
		}

		buildList.setAdapter(new BuildArrayAdapter(this, allBuilds, PushService
				.getSubscriptions(this)));
		buildList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(final AdapterView<?> parent, final View view,
					final int position, final long id) {
				if (id != R.id.notifyCheckbox) {
					final Build build = (Build) buildList.getItemAtPosition(position);
					final String buildAsJsonString = JsonUtils.toJsonString(build);
					final Intent intent =
						new Intent(getReferenceToThisActivity(), BuildInfoActivity.class);
					intent.putExtra(INTENT_EXTRA_SINGLE_BUILD, buildAsJsonString);
					startActivity(intent);
				}
			}
		});
		registerForContextMenu(buildList);

		liveIntentFilter = new IntentFilter();
		liveIntentFilter.addAction(ACTION_LIVE_UPDATE);

		onStart(); // hack..
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.buildList) {
			// get the selected one
			final AdapterView.AdapterContextMenuInfo info =
				(AdapterView.AdapterContextMenuInfo) menuInfo;
			final BuildArrayAdapter buildAdapter =
				(BuildArrayAdapter) ((ListView) findViewById(R.id.buildList)).getAdapter();
			final Build selectedBuild = buildAdapter.getItem(info.position);

			// create the menu
			final SharedPreferences preferences =
				PreferenceManager.getDefaultSharedPreferences(getReferenceToThisActivity());
			final boolean shouldPlay =
				preferences.getBoolean(selectedBuild.getName() + PREF_BUILD_SOUND_PREFIX, false);

			final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setTitle("Audio Warnings");
			String message = "";
			if (shouldPlay) {
				message =
					"Disable audio warnings on build \"" + selectedBuild.getName() + "\" update?";
			} else {
				message =
					"Enable audio warnings on build \"" + selectedBuild.getName()
						+ "\" update? Beware: This is annoying and disruptive.";
			}
			alertDialogBuilder.setMessage(message).setCancelable(true)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog, final int id) {
							preferences
									.edit()
									.putBoolean(selectedBuild.getName() + PREF_BUILD_SOUND_PREFIX,
											!shouldPlay).commit();
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog, final int id) {
							dialog.dismiss();
						}
					});

			final AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("MainActivity", "onDestroy()");
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.update) {
			updateBuilds();
			return true;
		} else if (item.getItemId() == R.id.signout) {
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(liveUpdater);
		((BuinoApplication) getApplicationContext()).setMainForeground(false);
		Log.d("MainActivity", "onPause()");
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(liveUpdater, liveIntentFilter);
		((BuinoApplication) getApplicationContext()).setMainForeground(true);
		Log.d("MainActivity", "onResume()");
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("MainActivity", "onStart()");
		// do a merge on startup
		final ListView buildList = (ListView) findViewById(R.id.buildList);

		final List<Build> allBuilds = ((BuildArrayAdapter) buildList.getAdapter()).getAllBuilds();
		final List<Build> updatedInMemory =
			((BuinoApplication) getApplicationContext()).getAllBuildsInMemory();
		for (final Build buildInMemory : updatedInMemory) {
			// find it in the existing ones
			boolean doesBuildExist = false;
			for (final Build existingBuild : allBuilds) {
				if (buildInMemory.getName().equals(existingBuild.getName())) {
					existingBuild.merge(buildInMemory);
					doesBuildExist = true;
					break;
				}
			}
			if (!doesBuildExist) { // if it does not exist just add it
				allBuilds.add(buildInMemory);
			}
		}

		// set the build
		buildList.setAdapter(new BuildArrayAdapter(this, allBuilds, PushService
				.getSubscriptions(this)));
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d("MainActivity", "onStop()");
	}

	/**
	 * Sets the enabled state of all the checkboxes in the build list
	 */
	public void setAllCheckboxesEnabled(final boolean shouldEnabled) {
		final ListView buildList = (ListView) findViewById(R.id.buildList);
		for (int i = 0; i < buildList.getAdapter().getCount(); i++) {
			final CheckBox checkBox =
				(CheckBox) buildList.getChildAt(i).findViewById(R.id.notifyCheckbox);
			checkBox.setEnabled(shouldEnabled);
		}
	}

	/**
	 * Contacts parse and updates the build list
	 */
	public void updateBuilds() {
		final ProgressDialog loadingDialog =
			PopUpUtils.showLoadingMessage(this, null, "Updating build list...");
		final ParseQuery query = new ParseQuery(MainActivity.BUILD_CLASS_NAME_IN_BACKEND);
		query.findInBackground(new FindCallback() {
			@Override
			public void done(final List<ParseObject> resultList, final ParseException e) {
				loadingDialog.dismiss();
				if (e == null) {
					boolean anyChanges = false;
					final BuinoApplication buinoApp = (BuinoApplication) getApplicationContext();
					for (final ParseObject parseObject : resultList) {
						try {
							anyChanges |=
								buinoApp.updateBuild(JsonUtils.parseFromParseObject(parseObject));
						} catch (final JSONException jsonException) {
							PopUpUtils
									.showErrorMessage(
											getReferenceToThisActivity(),
											"Error while reading build \""
												+ parseObject
														.getString(WelcomeActivity.PARSE_OBJECT_NAME_ATTRIBUTE)
												+ "\"  from the server. Check the database!");
						}
					}
					if (!anyChanges) {
						Toast.makeText(getReferenceToThisActivity(), "No changes",
								Toast.LENGTH_SHORT).show();
					} else {
						onStart(); // redraw everything
					}
				} else {
					PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
							"Error while communicating with server: " + e.getMessage()
								+ ". Please try again later or contact the app creator.");
				}
			}
		});
	}

}
