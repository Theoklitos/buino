package com.buino.client.activity;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.buino.client.BuinoApplication;
import com.buino.client.R;
import com.buino.client.data.Build;
import com.buino.client.data.Build.BuildStatus;
import com.buino.client.util.JsonUtils;
import com.buino.client.util.PopUpUtils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Displays data about a single build
 * 
 * @author takis
 * 
 */
public final class BuildInfoActivity extends Activity {

	private Build receivedBuild;

	/**
	 * For use inside closures
	 */
	public Activity getReferenceToThisActivity() {
		return this;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.build_info_activity);

		final Bundle receivedBundle = getIntent().getExtras();
		if (receivedBundle != null) {
			final String buildAsJsonString =
				receivedBundle.getString(MainActivity.INTENT_EXTRA_SINGLE_BUILD);
			receivedBuild = JsonUtils.fromJsonString(buildAsJsonString);
			updateActivityViewsFromBuild(receivedBuild);
		}

		final Button updateButton = (Button) findViewById(R.id.updateBuild);
		updateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				updateThisBuild();
			}
		});

		final TextView goToBuildUrl = (TextView) findViewById(R.id.goToBuildUrl);
		final Build buildForLink = receivedBuild;
		goToBuildUrl.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				if (buildForLink != null) {
					final String jenkinsUrl = buildForLink.getUrl();
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(jenkinsUrl));
					startActivity(intent);
				}
			}
		});
	}

	@SuppressLint("NewApi")
	private void trySetActionBar(final BuildStatus status) {
		final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (status.equals(BuildStatus.FAILURE)) {
				getActionBar().setIcon(R.drawable.red_lamp);
			} else if (status.equals(BuildStatus.SUCCESS)) {
				getActionBar().setIcon(R.drawable.green_lamp);
			} else {
				getActionBar().setIcon(R.drawable.grey_lamp);
			}
		}
	}

	/**
	 * Refreshes the gui
	 */
	private void updateActivityViewsFromBuild(final Build build) {
		final String buildName = build.getName();
		setTitle(buildName);

		final TextView nameField = (TextView) findViewById(R.id.name);
		nameField.setText(buildName);

		final TextView statusField = (TextView) findViewById(R.id.status);
		final BuildStatus status = build.getStatus();
		statusField.setText(StringUtils.capitalize(StringUtils.lowerCase(status.name())));

		trySetActionBar(status);

// final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
// if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
// if (status.equals(BuildStatus.FAILURE)) {
// getActionBar().setIcon(R.drawable.red_lamp);
// } else if (status.equals(BuildStatus.SUCCESS)) {
// getActionBar().setIcon(R.drawable.green_lamp);
// } else {
// getActionBar().setIcon(R.drawable.grey_lamp);
// }
// // Do something for froyo and above versions
// } else {
// // Don't mess with the action bar in earlier versions
// }
		final TextView authorField = (TextView) findViewById(R.id.author);
		authorField.setText(build.getAuthor());

		final TextView numberField = (TextView) findViewById(R.id.number);
		numberField.setText(Integer.toString(build.getNumber()));
	}

	/**
	 * Updates the buidl that this view represents
	 */
	public void updateThisBuild() {
		if (receivedBuild != null) {
			final ProgressDialog loadingDialog =
				PopUpUtils.showLoadingMessage(this, null,
						"Updating build \"" + receivedBuild.getName() + "\"...");
			final ParseQuery query = new ParseQuery(MainActivity.BUILD_CLASS_NAME_IN_BACKEND);
			query.whereEqualTo(WelcomeActivity.PARSE_OBJECT_NAME_ATTRIBUTE, receivedBuild.getName());
			query.findInBackground(new FindCallback() {
				@Override
				public void done(final List<ParseObject> resultList, final ParseException e) {
					loadingDialog.dismiss();
					if (resultList.size() != 1) {
						PopUpUtils
								.showErrorMessage(getReferenceToThisActivity(),
										"Error matching this build in the database, could not update. Check the database!");
					}
					if (e == null) {
						final BuinoApplication buinoApp =
							(BuinoApplication) getApplicationContext();
						final ParseObject parseObject = resultList.get(0);
						try {
							final Build updatedBuild = JsonUtils.parseFromParseObject(parseObject);
							buinoApp.updateBuild(updatedBuild);
							if (receivedBuild.merge(updatedBuild)) {
								Toast.makeText(getReferenceToThisActivity(), "No changes",
										Toast.LENGTH_SHORT).show();
							} else {
								// and refresh the gui
								updateActivityViewsFromBuild(updatedBuild);
								receivedBuild = updatedBuild;
							}
						} catch (final JSONException jsonException) {
							PopUpUtils
									.showErrorMessage(
											getReferenceToThisActivity(),
											"Error while reading build \""
												+ parseObject
														.getString(WelcomeActivity.PARSE_OBJECT_NAME_ATTRIBUTE)
												+ "\"  from the server. Check the database!");
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
}
