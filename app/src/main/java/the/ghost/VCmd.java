package the.ghost;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.provider.ContactsContract;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VCmd extends Service {

	private SpeechRecognizer speechRecognizer;
	private Intent speechRecognizerIntent;
	private CameraManager cameraManager;
	private String cameraId;
	private boolean isFlashlightOn = false;

	@Override
	public void onCreate() {
		super.onCreate();
		initSpeechRecognizer();
		initFlashlight();
		startListening();
	}

	private void initSpeechRecognizer() {
		speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

		// Attach the listener
		speechRecognizer.setRecognitionListener(new RecognitionListener() {
			@Override
			public void onReadyForSpeech(Bundle params) {
				// Ready to listen
			}

			@Override
			public void onBeginningOfSpeech() {
				// User started speaking
			}

			@Override
			public void onRmsChanged(float rmsdB) {
				// Sound level change
			}

			@Override
			public void onBufferReceived(byte[] buffer) {
				// Received more sound data
			}

			@Override
			public void onEndOfSpeech() {
				// User stopped speaking
			}

			@Override
			public void onError(int error) {
				startListening(); // Restart listening on error
			}

			@Override
			public void onResults(Bundle results) {
				// Handle voice command
				ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				if (matches != null && matches.size() > 0) {
					String command = matches.get(0).toLowerCase(Locale.getDefault());
					handleVoiceCommand(command);
				}
				startListening(); // Restart listening after handling
			}

			@Override
			public void onPartialResults(Bundle partialResults) {
			}

			@Override
			public void onEvent(int eventType, Bundle params) {
			}
		});
	}

	private void initFlashlight() {
		cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		try {
			cameraId = cameraManager.getCameraIdList()[0]; // Get the first camera ID
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	public void startListening() {
		speechRecognizer.startListening(speechRecognizerIntent);
	}

	public void stopListening() {
		speechRecognizer.stopListening();
	}

	private void handleVoiceCommand(String command) {
		Upa.showToast(this, command);
		if (command.contains("call")) {
			makePhoneCall(command);
			Beep();
		} else if (command.contains("open")) {
			openSettings();
			Beep();
		} else if (command.contains("flashlight on")) {
			toggleFlashlight(true);
			Beep();
		} else if (command.contains("flashlight off")) {
			toggleFlashlight(false);
			Beep();
		} else {
			Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show();
		}
	}

	private void Beep() {
		ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
		toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 500); // Beep for 500 milliseconds
	}

	private void toggleFlashlight(boolean turnOn) {
		try {
			if (cameraManager != null) {
				cameraManager.setTorchMode(cameraId, turnOn);
				isFlashlightOn = turnOn;
				String message = turnOn ? "Flashlight turned on" : "Flashlight turned off";
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error accessing flashlight", Toast.LENGTH_SHORT).show();
		}
	}

	private static final int REQUEST_CALL_PERMISSION = 100;
	private static final int REQUEST_CONTACT_PERMISSION = 101;

	private void makePhoneCall(String command) {
		if (checkCallPermission() && checkContactPermission()) {
			initiatePhoneCall(command);
		}
	}

	private boolean checkCallPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
				///Lul.requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
				return false;
			}
		}
		return true;
	}

	private boolean checkContactPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
				//Lul.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT_PERMISSION);
				return false;
			}
		}
		return true;
	}

	private void initiatePhoneCall(String command) {
		command = command.toLowerCase(Locale.getDefault()).trim();

		if (command.matches(".*\\d+.*")) {
			callNumber(command);
		} else {
			String phoneNumber = getPhoneNumberFromContactName(command);
			if (phoneNumber != null) {
				callNumber(phoneNumber);
			} else {
				Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private String getPhoneNumberFromContactName(String contactName) {
		ContentResolver contentResolver = getContentResolver();
		Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

				if (displayName != null && displayName.toLowerCase(Locale.getDefault()).contains(contactName)) {
					int hasPhoneNumber = Integer.parseInt(
							cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
					if (hasPhoneNumber > 0) {
						Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
								null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
								new String[] { contactId }, null);

						if (phoneCursor != null && phoneCursor.moveToNext()) {
							String phoneNumber = phoneCursor.getString(
									phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							phoneCursor.close();
							cursor.close();
							return phoneNumber;
						}
						phoneCursor.close();
					}
				}
			}
			cursor.close();
		}
		return null;
	}

	private void callNumber(String phoneNumber) {
		if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
			Intent intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse("tel:" + phoneNumber));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);

			// Enable speakerphone after call starts
			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager != null) {
				audioManager.setMode(AudioManager.MODE_IN_CALL);
				audioManager.setSpeakerphoneOn(true); // Turn on speaker
			}
		} else {
			Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show();
		}
	}

	private void openSettings() {
		Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		speechRecognizer.destroy();
	}
}

/*





@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
	super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	if (requestCode == REQUEST_CALL_PERMISSION || requestCode == REQUEST_CONTACT_PERMISSION) {
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, "Permission granted, please repeat the command.", Toast.LENGTH_SHORT).show();
			} else {
			Toast.makeText(this, "Permission is required to make calls.", Toast.LENGTH_SHORT).show();
		}
	}
}

*/