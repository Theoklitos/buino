package com.buino.client;

import java.util.Locale;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

/**
 * Handles the TTS stuff
 * 
 * @author takis
 * 
 */
public final class BuinoTTS extends Service implements TextToSpeech.OnInitListener,
		OnUtteranceCompletedListener {

	private TextToSpeech mTts;
	private String spokenText;

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// nothing here
	}

	@Override
	public void onDestroy() {
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		super.onDestroy();
	}

	public void onInit(final int status) {
		if (status == TextToSpeech.SUCCESS) {
			final int result = mTts.setLanguage(Locale.US);
			mTts.setSpeechRate(0.9F);
			mTts.setPitch(0.9F);
			if (result != TextToSpeech.LANG_MISSING_DATA
				&& result != TextToSpeech.LANG_NOT_SUPPORTED) {
				mTts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		spokenText = intent.getExtras().getString(NotificationReceiver.INTENT_EXTRA_SPEECH_MESSAGE);
		mTts = new TextToSpeech(this, this);
		return Service.START_NOT_STICKY;
	}

	public void onUtteranceCompleted(final String uttId) {
		stopSelf();
	}
}
