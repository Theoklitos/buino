package com.buino.client.activity;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.buino.client.R;
import com.buino.client.util.PopUpUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

/**
 * Handles new user creation
 * 
 * @author takis
 * 
 */
public final class RegisterActivity extends Activity {

	public static final String PARSE_USER_BUILDS_FIELD = "builds";
	public static final String INTENT_ACTION_SIGN_IN_AFTER_REGISTER =
		"com.buino.SIGN_IN_AFTER_REGISTER";
	public static final String INTENT_EXTRA_USERNAME = "com.buino.client.Username";
	public static final String INTENT_EXTRA_PASSWORD = "com.buino.client.Password";

	private String getEmail() {
		final EditText emailTextField = (EditText) findViewById(R.id.email);
		final String email = emailTextField.getText().toString();
		return email;
	}

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
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_activity);
		setTitle(R.string.register_activity_label);

		final Button register = (Button) findViewById(R.id.registerButton);
		register.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				try {
					final ParseUser user = new ParseUser();
					final String username = getUsername();
					final String password = getPassword();
					user.setUsername(username);
					user.setPassword(password);
					final String email = getEmail();
					if (!StringUtils.isBlank(email)) {
						user.setEmail(email);
					}

					// initial builds field is empty
					user.addAll(PARSE_USER_BUILDS_FIELD, new HashSet<String>());
					final ProgressDialog loadingDialog =
						PopUpUtils.showLoadingMessage(getReferenceToThisActivity(), null,
								"Registering...");
					user.signUpInBackground(new SignUpCallback() {
						@Override
						public void done(final ParseException e) {
							loadingDialog.dismiss();
							if (e == null) {
								final AlertDialog.Builder alertDialogBuilder =
									new AlertDialog.Builder(getReferenceToThisActivity());
								alertDialogBuilder.setTitle("New User");
								alertDialogBuilder
										.setMessage(
												"Welcome, " + username
													+ ". Would you like to sign in now?")
										.setCancelable(true)
										.setPositiveButton("Yes",
												new DialogInterface.OnClickListener() {
													public void onClick(
															final DialogInterface dialog,
															final int id) {
														// make intent to log in
														final Intent intent =
															new Intent();														
														intent.setAction(INTENT_ACTION_SIGN_IN_AFTER_REGISTER);
														intent.putExtra(INTENT_EXTRA_USERNAME,
																username);
														intent.putExtra(INTENT_EXTRA_PASSWORD,
																password);
														setResult(WelcomeActivity.SIGN_IN_AFTER_REGISTER, intent);																												
														finish();
													}
												})
										.setNegativeButton("No",
												new DialogInterface.OnClickListener() {
													public void onClick(
															final DialogInterface dialog,
															final int id) {
														dialog.dismiss();
														finish();
													}
												});

								final AlertDialog alertDialog = alertDialogBuilder.create();
								alertDialog.show();

								// Hooray! Let them use the app now.
							} else {
								PopUpUtils.showErrorMessage(getReferenceToThisActivity(),
										"Could not register: " + e.getMessage());
							}
						}
					});
				} catch (final IllegalArgumentException e) {
					return;
				}
			}
		});
	}
}
