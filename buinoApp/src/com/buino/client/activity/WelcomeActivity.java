package com.buino.client.activity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.buino.client.BuinoApplication;
import com.buino.client.R;
import com.buino.client.util.JsonUtils;
import com.buino.client.util.ParseUtils;
import com.buino.client.util.PopUpUtils;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class WelcomeActivity extends Activity {

	public static final int SIGN_IN_AFTER_REGISTER = 1;
	private static final int REGISTER_USER_CODE = 1;

	public static final String PARSE_OBJECT_NAME_ATTRIBUTE = "name";

	public static final String INTENT_EXTRA_ALL_BUILDS = "allBuilds";
	public static final String INTENT_EXTRA_IS_ANONYMOUS = "isAnonymous";

	private static final String PREFERENCES_USERNAME = "username";
	private static final String PREFERENCES_PASSWORD = "password";

	private String getPassword() {
		final EditText passwordTextField = (EditText) findViewById(R.id.password);
		final String password = passwordTextField.getText().toString();
		if (password.trim().length() == 0) {
			passwordTextField.setError("Please enter a password");
			throw new IllegalArgumentException();
		}
		return password;
	}

	/**
	 * For use in closures
	 */
	public Activity getReferenceToThisActivity() {
		return this;
	}

	private String getUsername() {
		final EditText usernameTextField = (EditText) findViewById(R.id.username);
		final String username = usernameTextField.getText().toString();
		if (username.trim().length() == 0) {
			usernameTextField.setError("Please enter a username");
			throw new IllegalArgumentException();
		}
		return username;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REGISTER_USER_CODE) {
			if (resultCode == SIGN_IN_AFTER_REGISTER) {
				try {
					final String username =
						data.getExtras().getString(RegisterActivity.INTENT_EXTRA_USERNAME);
					final String password =
						data.getExtras().getString(RegisterActivity.INTENT_EXTRA_PASSWORD);
					performLogIn(username, password);
				} catch (final Exception e) {
					PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
							"Weird error while signing in: " + e.getMessage()
								+ ", please try again.");
				}
			} else {
				// do nothing, just chill
			}
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_activity);
		setTitle(R.string.welcome_activity_label);

		// try to set the prefs
		trySetFieldsFromPreferences();

		final TextView registerLink = (TextView) findViewById(R.id.registerLink);
		registerLink.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				final Intent intent =
					new Intent(getReferenceToThisActivity(), RegisterActivity.class);
				startActivityForResult(intent, REGISTER_USER_CODE);
			}
		});

		final Button logInButton = (Button) findViewById(R.id.loginButton);
		logInButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				try {
					performLogIn(getUsername(), getPassword());
				} catch (final IllegalArgumentException e) {
					return;
				}
			}
		});

		final Button anonymousButton = (Button) findViewById(R.id.anonymousButton);
		anonymousButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				final ProgressDialog loadingDialog =
					PopUpUtils.showLoadingMessage(getReferenceToThisActivity(), null,
							"Signing in...");
				ParseAnonymousUtils.logIn(new LogInCallback() {
					@Override
					public void done(final ParseUser user, final ParseException e) {
						loadingDialog.dismiss();
						if (e != null) {
							PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
									"Login error: " + e.getMessage() + ".");
						} else {
							startMainActivity(true);
						}
					}
				});
			}
		});
	}

	/**
	 * Performs the log in process via Parse.com
	 */
	protected void performLogIn(final String username, final String password) {
		final ProgressDialog loadingDialog =
			PopUpUtils.showLoadingMessage(this, null, "Signing in...");
		ParseUser.logInInBackground(username, password, new LogInCallback() {
			@Override
			public void done(final ParseUser user, final ParseException e) {
				loadingDialog.dismiss();
				if (user != null) {
					// correct log in! check if to store the prefs
					final CheckBox rememberMe = (CheckBox) findViewById(R.id.rememberMeCheckbox);
					if (rememberMe.isChecked()) {
						PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
								.edit().putString(PREFERENCES_USERNAME, username)
								.putString(PREFERENCES_PASSWORD, password).commit();
					} else { // in this case clear them
						PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
								.edit().putString(PREFERENCES_USERNAME, "")
								.putString(PREFERENCES_PASSWORD, "").commit();
					}
					loadingDialog.dismiss();
					startMainActivity(false);
				} else {
					PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
							"Login error: " + e.getMessage() + ".");
				}
			}
		});
	}

	/**
	 * Starts the {@link MainActivity} with a simple intent
	 */
	private void startMainActivity(final boolean isAnonymous) {
		final ProgressDialog loadingDialog =
			PopUpUtils.showLoadingMessage(this, null, "Retrieving build list...");
		final ParseQuery query = new ParseQuery(MainActivity.BUILD_CLASS_NAME_IN_BACKEND);
		query.findInBackground(new FindCallback() {
			@Override
			public void done(final List<ParseObject> resultList, final ParseException e) {
				if (e == null) {
					final ArrayList<String> allBuildsAsStringList = new ArrayList<String>();
					final BuinoApplication buinoApp = (BuinoApplication) getApplicationContext();
					for (final ParseObject parseObject : resultList) {
						try {
							buinoApp.updateBuild(JsonUtils.parseFromParseObject(parseObject));
							allBuildsAsStringList.add(JsonUtils.toJsonString(parseObject));
						} catch (final JSONException jsonException) {
							PopUpUtils.showErrorMessage(
									getReferenceToThisActivity(),
									"Error while reading build \""
										+ parseObject.getString(PARSE_OBJECT_NAME_ATTRIBUTE)
										+ "\"  from the server. Check the database!");
						}
					}
					loadingDialog.dismiss();
					final ProgressDialog syncDialog = syncInstallationData(isAnonymous);
					final Intent intent =
						new Intent(getReferenceToThisActivity(), MainActivity.class);
					intent.putStringArrayListExtra(INTENT_EXTRA_ALL_BUILDS, allBuildsAsStringList);
					startActivityAfterOneSecond(intent, syncDialog);
				} else {
					PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
							"Error while communicating with server: " + e.getMessage()
								+ ". Please try again later or contact the app creator.");
				}
			}

			private void startActivityAfterOneSecond(final Intent intent,
					final ProgressDialog syncDialog) {
				final Handler handler = new Handler();
				final Runnable action = new Runnable() {
					public void run() {
						syncDialog.dismiss();
						startActivity(intent);
					}
				};
				handler.postDelayed(action, 1000);
			}
		});
	}

	private ProgressDialog syncInstallationData(final boolean isAnonymous) {
		final ProgressDialog loadingDialog =
			PopUpUtils.showLoadingMessage(this, null, "Syncing user preferences...");
		ParseUtils.unsubscribeInstallationFromAll(getReferenceToThisActivity());
		if (!isAnonymous) {
			ParseUtils.syncSubscriptionsFromUserData(getReferenceToThisActivity());
		}
		return loadingDialog;
	}

	private void trySetFieldsFromPreferences() {
		final SharedPreferences preferences =
			PreferenceManager.getDefaultSharedPreferences(getReferenceToThisActivity());
		final String username = preferences.getString(PREFERENCES_USERNAME, null);
		final String password = preferences.getString(PREFERENCES_PASSWORD, null);
		boolean wasRememberMeSet = false;
		if (StringUtils.isNotEmpty(username)) {
			final EditText usernameField = (EditText) findViewById(R.id.username);
			usernameField.setText(username);
			wasRememberMeSet = true;
		}
		if (StringUtils.isNotEmpty(password)) {
			final EditText passwordField = (EditText) findViewById(R.id.password);
			passwordField.setText(password);
			wasRememberMeSet = true;
		}
		final CheckBox rememberMeCheckBox = (CheckBox) findViewById(R.id.rememberMeCheckbox);
		rememberMeCheckBox.setChecked(wasRememberMeSet);
	}

}
