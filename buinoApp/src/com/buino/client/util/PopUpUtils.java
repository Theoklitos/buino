package com.buino.client.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.buino.client.R;

/**
 * Various popups such as loading screen, error messages etc
 * 
 * @author takis
 * 
 */
public final class PopUpUtils {
		
	public static void showErrorMessage(final Context context, final String message) {
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setTitle("Error").setIcon(R.drawable.error_icon);
		alertDialogBuilder.setMessage(message).setCancelable(true)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.dismiss(); // not necessary
					}
				});

		final AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	public static ProgressDialog showLoadingMessage(final Context context, final String title, final String message) {
		final ProgressDialog dialog = ProgressDialog.show(context, title, message, true);
		dialog.setCancelable(true);
		return dialog;
	}

}
