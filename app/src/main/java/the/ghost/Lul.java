package the.ghost;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.app.admin.DeviceAdminReceiver;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.provider.ContactsContract;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONObject;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.view.Gravity;
import android.provider.MediaStore;

public class Lul extends Activity {

	private TextView terminalView, statusView, adTxt;
	private ImageView wallpaperLayout;
	private EditText hiddenInput;
	//private StringBuilder terminalContent;
	private boolean isFlashOn = false;
	private Upa upa;
	private Map<String, Command> commandMap;
	private CameraManager cameraManager;
	private String cameraId;

	public List<String> commandHistory;
	public int currentCommandIndex;
	private static final String INDENTATION = "    - ";
	private SpannableStringBuilder terminalContent = new SpannableStringBuilder();
	private ScrollView scrollView;

	private boolean isFetch = false;
	private boolean isRun = false;
	public final String FILE_NAME = "apps.json";
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	private Map<String, String> appNameToPackageMap;
	private PackageManager packageManager;

	public static int[] gradientColors = { Color.BLACK, Color.BLACK };
	public static int solidColor = Color.BLACK;
	public static float cornerRadius = 1f;
	public static int strokeWidth = 1;
	public static int[] strokeGradientColors = { Color.GREEN, Color.CYAN, Color.GREEN };
	public static float angle = 90f;

	private static final int REQUEST_MIC_PERMISSION = 100;
	private static final int REQUEST_CAMERA_PERMISSION = 101;
	private static final int PICK_IMAGE = 13;

	private IntentFilter filter = new IntentFilter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpUser();
	}

	private void setUpUser() {
		Upa.setVisi(getWindow());
		setContentView(R.layout.lul);
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}

		if (!Upa.isNetworkAvailable(this) && Upa.readFile(this, getString(R.string.isUser)).isEmpty()) {
			Upa.showToast(this, getString(R.string.no_connection));
			Upa.vibrate(this, 500);
			Upa.finishedAllDn(this);
			return;
		}

		wallpaperLayout = findViewById(R.id.wallLay);
		statusView = findViewById(R.id.statusView);
		adTxt = findViewById(R.id.adTxt);
		terminalView = findViewById(R.id.terminalView);
		hiddenInput = findViewById(R.id.hiddenInput);
		scrollView = findViewById(R.id.scrollView);
		terminalContent = new SpannableStringBuilder(getString(R.string.msg_wellcome) + "\n\n");

		initializeCamera();
		initializeCommands();
		displayTerminalContent();
		//Upa.toggleKeyboard(this, true, hiddenInput);

		findViewById(R.id.add).setOnClickListener(v -> {
			ObjectAnimator rotation = ObjectAnimator.ofFloat(findViewById(R.id.add), "rotation", 0f, 360f);
			rotation.setDuration(500);
			rotation.start();

			addCmd();
		});

		hiddenInput.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
					&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
				addCmd();
				return true;
			}
			return false;
		});

		findViewById(R.id.return_undo).setOnClickListener(v -> {
			ObjectAnimator rotation = ObjectAnimator.ofFloat(findViewById(R.id.return_undo), "rotation", 0f, 360f);
			rotation.setDuration(500);
			rotation.start();
			recallCommand();
			Upa.vibrate(this, 35);
		});

		hiddenInput.addTextChangedListener(new TextWatcher() {
			private long lastTextEditTime = 0;
			private static final long DEBOUNCE_DELAY = 10; // Adjust as necessary

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String query = s.toString().trim();
				lastTextEditTime = System.currentTimeMillis();
				new Thread(() -> {
					try {
						Thread.sleep(DEBOUNCE_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (System.currentTimeMillis() >= (lastTextEditTime + DEBOUNCE_DELAY)) {
						if (!query.isEmpty()) {
							if (query.toLowerCase().startsWith("call")) {
								// Extract the contact name or number
								String contactNameOrNumber = query.substring(4).trim();

								/*
								runOnUiThread(() -> {
									if (!hasCallPermission()) {
										requestCallPermission();
									} else {
										handleCallCommand(contactNameOrNumber);
									}
								});
								*/
							} else {
								// For regular app suggestions
								List<JSONObject> filteredApps = filterApps(query);
								runOnUiThread(() -> updateAppSuggestions(filteredApps));
							}
						} else {
							// Display all apps if the query is empty
							List<JSONObject> allApps = getSortedAppsByCount(Lul.this);
							runOnUiThread(() -> updateAppSuggestions(allApps));
						}
					}
				}).start();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// No operation
			}

			@Override
			public void afterTextChanged(Editable s) {
				// No operation
			}
		});
		
		
		/*
		hiddenInput.addTextChangedListener(new TextWatcher() {
			private long lastTextEditTime = 0;
			private static final long DEBOUNCE_DELAY = 10; // Adjust as necessary
		
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String query = s.toString().trim();
				lastTextEditTime = System.currentTimeMillis();
				new Thread(() -> {
					try {
						Thread.sleep(DEBOUNCE_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (System.currentTimeMillis() >= (lastTextEditTime + DEBOUNCE_DELAY)) {
						if (!query.isEmpty()) {
							List<JSONObject> filteredApps = filterApps(query);
							runOnUiThread(() -> updateAppSuggestions(filteredApps));
						} else {
							List<JSONObject> allApps = getSortedAppsByCount(Lul.this);
							runOnUiThread(() -> updateAppSuggestions(allApps));
						}
					}
				}).start();
			}
		
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// No operation
			}
		
			@Override
			public void afterTextChanged(Editable s) {
				// No operation
			}
		});
		*/
		

		upa = new Upa(this, statusView);
		if (!isFetch) {
			new FetchDataTask().execute(getString(R.string.ad_str));
			isFetch = true;
		}

		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		filter.addAction(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED);
		filter.addAction(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		Br brReceiver = new Br(Lul.this);
		registerReceiver(brReceiver, filter);

		//Upa.incrementUnlockCount(this);

		if ("true".equals(Upa.readFile(this, "VCmd")) && !Upa.readFile(this, "VCmd").isEmpty()) {
			if (checkMicrophonePermission()) {
				startVoiceCommandService();
				appendToTerminal("Voice Listening..... >", Color.LTGRAY, 14f, false);
			}
		}
		
		isRun = true;
	}

	private void initializeCamera() {
		cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
		try {
			cameraId = cameraManager.getCameraIdList()[0]; // Get the first camera ID
		} catch (CameraAccessException e) {
			e.printStackTrace();
			appendToTerminal(getString(R.string.err_camera));
		}
	}

	private void initializeCommands() {
		commandMap = new HashMap<>();
		commandMap.put("help", new HelpCommand());
		commandMap.put("walls", new WallpCommand());
		commandMap.put("wallr", this::removeWallpaper);
		commandMap.put("viboff", this::viboff);
		commandMap.put("vibon", this::vibon);
		commandMap.put("wifi", new WiFiCommand(this));
		commandMap.put("bt", new BluetoothCommand());
		commandMap.put("ip", new IpCommand());
		commandMap.put("call", new CallCommand(terminalContent, this));
		commandMap.put("clear", new ClearCommand(terminalContent));
		commandMap.put("whatsapp", new WhatsAppCommand(this));
		commandMap.put("exit", this::exitApplication);
		commandMap.put("voff", this::stopVoff);
		commandMap.put("von", this::stopVon);

		FileCommand fileCommand = new FileCommand(this);

		commandMap.put("ls", fileCommand);
		commandMap.put("cd", fileCommand);
		commandMap.put("flash", new FlashCommand(cameraManager, cameraId));
		commandMap.put("vib", new VibrateCommand(this));
		commandMap.put("beep", new BeepCommand());
		commandMap.put("info", new InfoCommand(this));
		commandMap.put("apps", new AppsCommand(this));
		commandMap.put("search", new SearchCommand(this));

		commandHistory = new ArrayList<>();
		currentCommandIndex = -1;
	}

	private String exitApplication(String[] parts) {
		upa.chooseGo(this);
		appendToTerminal(getString(R.string.cmd_choose_exit), Color.YELLOW, 14f, false);
		return "";
	}

	private String viboff(String[] parts) {
		Upa.writeFile(this, "isVib", "false");
		appendToTerminal("Vibration Disabled.", Color.YELLOW, 14f, false);
		return "";
	}

	private String vibon(String[] parts) {
		Upa.writeFile(this, "isVib", "true");
		appendToTerminal("Vibration Enabled.", Color.GREEN, 14f, false);
		return "";
	}

	private String stopVoff(String[] parts) {
		stopVoiceCommandService();
		appendToTerminal("Voice Commands Disabled.", Color.YELLOW, 14f, false);
		Upa.writeFile(this, "VCmd", "false");
		return "";
	}

	private String stopVon(String[] parts) {
		stopVoiceCommandService();
		appendToTerminal("Voice Commands Enabled.", Color.GREEN, 14f, false);
		Upa.writeFile(this, "VCmd", "true");
		return "";
	}

	private void executeCommand(String command) {
		String[] commandParts = command.trim().split("\\s+");

		if (commandParts.length == 0 || commandParts[0].isEmpty()) {
			appendToTerminal("Error : No command provided.");
			return;
		}

		String mainCommand = commandParts[0].toLowerCase();
		Command commandToExecute = commandMap.get(mainCommand);

		if (commandToExecute != null) {
			String result = commandToExecute.execute(commandParts);
			appendToTerminal(result);
		} else if (URLUtil.isValidUrl(command)) {
			URLCommand urlCommand = new URLCommand(this);
			String result = urlCommand.execute(commandParts);
			appendToTerminal(result);
		} else if (isAppNameValid(mainCommand)) {
			launchAppName(command);
		} else {
			appendToTerminal("Unknown command: " + command);
		}

		saveCommand(command);
	}

	//////

	private void saveCommand(String userInput) {
		if (!userInput.isEmpty()) {
			storeCommand("default_commands", userInput);
			if (commandHistory.size() >= 10) {
				commandHistory.remove(0);
			}
			commandHistory.add(userInput);
			currentCommandIndex = commandHistory.size();
		} else {
			appendToTerminal("Please enter a command to save.");
		}
	}

	private void recallCommand() {
		JSONArray commands = getStoredCommands("default_commands");

		if (commands.length() == 0) {
			return;
		}

		if (currentCommandIndex == -1) {
			currentCommandIndex = commands.length() - 1;
		} else {
			currentCommandIndex--;
			if (currentCommandIndex < 0) {
				currentCommandIndex = commands.length() - 1;
			}
		}

		if (currentCommandIndex >= 0 && currentCommandIndex < commands.length()) {
			try {
				String commandToRecall = commands.getString(currentCommandIndex);
				hiddenInput.setText(commandToRecall);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	///////////

	private void addCmd() {
		String command = hiddenInput.getText().toString().trim();
		if (!command.isEmpty()) {
			SpannableStringBuilder timestampedCommand = createSpannableText(getFDT(), Color.CYAN, 14f);
			terminalContent.append("$ ").append(timestampedCommand).append(command).append("\n");
			executeCommand(command);
			hiddenInput.setText("");
			Upa.vibrate(this, 40);
		}
	}

	public SpannableStringBuilder createSpannableText(String text, int color, float textSize) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
		spannableStringBuilder.setSpan(new ForegroundColorSpan(color), 0, spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableStringBuilder.setSpan(new AbsoluteSizeSpan((int) textSize, true), 0, spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannableStringBuilder;
	}

	public String getFDT() {
		SimpleDateFormat formatter = new SimpleDateFormat("[d MMM yy HH:mm:ss] ", Locale.ENGLISH);
		Date currentDate = new Date();
		return formatter.format(currentDate);
	}

	public void appendToTerminalWithTimestamp(String command) {
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}
		String timestamp = getFDT();
		String output = timestamp + command;
		appendToTerminal(output, Color.RED, 14f, false);
	}

	public void appendToTerminal(String text, int color, float textSize, boolean isSelectable) {
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}

		if (text == null || text.isEmpty()) {
			return;
		}

		String prefix = "└─ ";
		String fullText = prefix + text;
		SpannableString spannableString = new SpannableString(fullText);
		spannableString.setSpan(new ForegroundColorSpan(color), 0, fullText.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableString.setSpan(new AbsoluteSizeSpan((int) textSize, true), prefix.length(), fullText.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		terminalContent.append(spannableString).append("\n");
		if (isSelectable) {
			terminalView.setTextIsSelectable(false);
			displayTerminalContent();
		} else {
			terminalView.setTextIsSelectable(false);
			displayTerminalContent();
		}
	}

	public void appendToTerminal(String text) {
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}
		boolean isError = text.contains("Unknown command....") || text.contains("Error") || text.contains("Invalid");
		int color = isError ? Color.RED : Color.GREEN;
		appendToTerminal(text, color, 14f, false);
	}

	public void appendByBr(SpannableStringBuilder text) {
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}
		if (text == null) {
			return;
		}

		String prefix = "└─ ";
		String fullText = prefix + text;
		SpannableString spannableString = new SpannableString(fullText);
		terminalContent.append(spannableString).append("\n");
		displayTerminalContent();
	}

	public void displayTerminalContent() {
		terminalView.setText(terminalContent.length() > 0 ? terminalContent : "", TextView.BufferType.SPANNABLE);
		terminalView.setMovementMethod(LinkMovementMethod.getInstance());
		scrollToBottom();
	}

	private void scrollToBottom() {
		if (scrollView != null) {
			scrollView.post(() -> {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				scrollView.smoothScrollTo(0, terminalView.getBottom());
			});
		}
	}

	/////////

	public void showKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(hiddenInput, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	// Command interface
	interface Command {
		String execute(String[] commandParts);
	}

	//Whatsapp command
	class WhatsAppCommand implements Command {
		private Context context;

		public WhatsAppCommand(Context context) {
			this.context = context;
		}

		@Override
		public String execute(String[] commandParts) {
			if (commandParts.length > 1) {
				String phoneNumber = commandParts[1];
				openWhatsApp(phoneNumber);
				return "Redirecting to WhatsApp for " + phoneNumber;
			} else {
				return "Phone number missing for WhatsApp command.";
			}
		}

		private void openWhatsApp(String phoneNumber) {
			try {
				// Format the phone number correctly
				phoneNumber = phoneNumber.replaceAll("[^\\d]", ""); // Remove any non-numeric characters
				String url = "https://wa.me/" + phoneNumber;

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				intent.setPackage("com.whatsapp");

				if (intent.resolveActivity(context.getPackageManager()) != null) {
					context.startActivity(intent);
				} else {
					appendToTerminal("WhatsApp not installed.", Color.RED, 14f, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
				appendToTerminal("Failed to open WhatsApp.", Color.RED, 14f, false);
			}
		}
	}

	//Wall Command
	class WallpCommand implements Command {

		public WallpCommand() {
		}

		@Override
		public String execute(String[] commandParts) {
			openGallery();
			return "Opening Gallery to choose wallpaper...";
		}

		private void openGallery() {
			Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(intent, PICK_IMAGE);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
			Uri selectedImage = data.getData();
			try {
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
				File savedFile = saveImageLocally(bitmap);
				setToLayout(bitmap);
				Upa.writeFile(this, "wallpaper_path", savedFile.getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				appendToTerminal("Failed to load wallpaper", Color.RED, 14f, false);
			}
		}
	}

	private File saveImageLocally(Bitmap bitmap) throws Exception {
		String fileName = "wallpaper_image.jpg";
		File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Wallpapers");

		if (!storageDir.exists()) {
			storageDir.mkdirs();
		}

		File imageFile = new File(storageDir, fileName);
		FileOutputStream out = new FileOutputStream(imageFile);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		out.flush();
		out.close();

		appendToTerminal("Image saved locally at : " + imageFile.getAbsolutePath(), Color.YELLOW, 14f, false);
		return imageFile;
	}

	private void setToLayout(Bitmap bitmap) {
		wallpaperLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
		appendToTerminal("Wallpaper set in layout!", Color.YELLOW, 14f, false);
	}

	public void loadSavedWallpaper() {
		String savedPath = Upa.readFile(this, "wallpaper_path");
		if (savedPath != null) {
			File imgFile = new File(savedPath);
			if (imgFile.exists()) {
				Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
				wallpaperLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
				//appendToTerminal("Wallpaper loaded from saved file", Color.RED, 14f, false);
			}
		}
	}

	private String removeWallpaper(String[] parts) {
		wallpaperLayout.setBackground(null);
		appendToTerminal("Wallpaper cleared from layout!", Color.YELLOW, 14f, false);
		Upa.writeFile(this, "wallpaper_path", null); // Optionally clear the saved path
		return "";
	}

	//URL Command
	class URLCommand implements Command {
		private Context context;

		public URLCommand(Context context) {
			this.context = context;
		}

		@Override
		public String execute(String[] commandParts) {
			Upa.showToast(Lul.this, commandParts[0]);

			if (commandParts.length > 2) {
				return "Error : No URL provided.\n";
			}
			String url = commandParts[0];
			if (!URLUtil.isValidUrl(url)) {
				return "Error : Invalid URL format.\n";
			}
			// Generate a tone
			ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
			toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 500); // Beep for 500 milliseconds
			// Open URL in the browser
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensures it works outside of Activity
			context.startActivity(intent);
			return "Opening Successfully..!\n";
		}
	}

	class HelpCommand implements Command {

		String[] COMMANDS = { "<help>  | Guide for available cmd", "<clear>   | Clear terminal screen",
				"<voff> / <von>   | Stop start voice cmd", "<exit>   | Exit the application",
				"<flash>   | Toggle flashlight", "<vib <duration>>  | Vibrate device (ms)",
				"<beep>   | Play beep sound", "<info>   | Show device info", "<apps> / <apps list>  | List apps",
				"<call <number/contact>> | Make a call", "<search <app> <query>>  | Search within an app",
				"<app name>   | Open an app", "<wifi>  | WiFi on/off", "<bt>    | Bluetooth on/off",
				"<battery>   | Show battery status", "<ip>    | Get Ip Info.", "<storage>   | Show available storage",
				"<date>   | Show date and time", "<walls> <wallr>   | Set or Remove",
				"<viboff> <viboff> | Vibration Off\\onn" };

		@Override
		public String execute(String[] commandParts) {
			StringBuilder helpMessage = new StringBuilder(getString(R.string.cmd_help) + "\n\n");
			for (String command : COMMANDS) {
				helpMessage.append(INDENTATION).append(command).append("\n");
			}
			appendToTerminal(helpMessage.toString(), Color.GRAY, 12f, false);
			return "";
		}
	}

	// Bt Command
	class BluetoothCommand implements Command {
		private BluetoothAdapter bluetoothAdapter;

		public BluetoothCommand() {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}

		@Override
		public String execute(String[] commandParts) {
			if (bluetoothAdapter == null) {
				appendToTerminal("Bluetooth not supported on this device.", Color.RED, 14f, false);
				return "";
			}

			if (bluetoothAdapter.isEnabled()) {
				bluetoothAdapter.disable();
				appendToTerminal("Bluetooth turned off.", Color.YELLOW, 14f, false);
			} else {
				bluetoothAdapter.enable();
				appendToTerminal("Bluetooth turned on.", Color.GREEN, 14f, false);
			}

			return "";
		}
	}

	//Wifi Command
	class WiFiCommand implements Command {
		private WifiManager wifiManager;

		public WiFiCommand(Context context) {
			wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		}

		@Override
		public String execute(String[] commandParts) {
			if (wifiManager == null) {
				appendToTerminal("WiFi not supported on this device.", Color.RED, 14f, false);
				return "";
			}
			if (wifiManager.isWifiEnabled()) {
				wifiManager.setWifiEnabled(false);
				appendToTerminal("WiFi turned off.", Color.YELLOW, 14f, false);
			} else {
				wifiManager.setWifiEnabled(true);
				appendToTerminal("WiFi turned on.", Color.GREEN, 14f, false);
			}
			return "";
		}
	}

	// Location Command

	public class IpCommand implements Command {
		@Override
		public String execute(String[] commandParts) {
			String ipToCheck;

			// Check if a valid IP is provided, otherwise use local IP.
			if (commandParts.length > 1 && isValidIP(commandParts[1])) {
				ipToCheck = commandParts[1];
				new FetchIpDetailsTask().execute(ipToCheck); // Fetch IP details using the provided IP
			} else if (commandParts.length == 1 && "ip".equalsIgnoreCase(commandParts[0])) {
				// Only "ip" command is provided, fetch the local IP and then its details
				getIPAddress(ip -> new FetchIpDetailsTask().execute(ip)); // Fetch local IP asynchronously
			} else {
				appendToTerminal("Invalid command or IP provided.", Color.RED, 14f, false);
			}

			return "";
		}

		private boolean isValidIP(String ip) {
			String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
			return ip.matches(ipRegex);
		}

		private class FetchIpDetailsTask extends AsyncTask<String, Void, String> {

			@Override
			protected void onPreExecute() {
				appendToTerminal("Fetching IP details...", Color.GREEN, 14f, false);
			}

			@Override
			protected String doInBackground(String... params) {
				String ipToCheck = params[0];

				String apiUrl = String.format(getString(R.string.locApi), ipToCheck);
				try {
					HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
					connection.setRequestMethod("GET");

					StringBuilder responseBuilder = new StringBuilder();
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(connection.getInputStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							responseBuilder.append(line);
						}
					}
					return responseBuilder.toString();
				} catch (Exception e) {
					return "Error fetching IP details: " + e.getMessage();
				}
			}

			@Override
			protected void onPostExecute(String result) {
				if (result != null && !result.startsWith("Error")) {
					try {
						parseAndDisplayIpDetails(result);
					} catch (Exception e) {
						appendToTerminal("Error parsing IP details", Color.RED, 14f, false);
					}
				} else {
					appendToTerminal(result != null ? result : "Failed to fetch IP info", Color.RED, 14f, true);
				}
			}

			private void parseAndDisplayIpDetails(String jsonResponse) throws Exception {
				JSONObject jsonObject = new JSONObject(jsonResponse);
				StringBuilder detailsBuilder = new StringBuilder();
				detailsBuilder.append("IP Detail :\n").append(INDENTATION).append("IP: ")
						.append(jsonObject.getString("ip")).append("\n").append(INDENTATION).append("Country: ")
						.append(jsonObject.getString("country")).append("\n").append(INDENTATION).append("Provider: ")
						.append(jsonObject.getString("org")).append("\n").append(INDENTATION).append("City: ")
						.append(jsonObject.getString("city")).append("\n").append(INDENTATION).append("Region: ")
						.append(jsonObject.getString("region")).append("\n").append(INDENTATION).append("Postal: ")
						.append(jsonObject.getString("postal")).append("\n").append(INDENTATION).append("Timezone: ")
						.append(jsonObject.getString("timezone")).append("\n").append(INDENTATION).append("Latitude: ")
						.append(jsonObject.getString("loc").split(",")[0]).append("\n").append(INDENTATION)
						.append("Longitude: ").append(jsonObject.getString("loc").split(",")[1]).append("\n")
						.append(INDENTATION).append(jsonObject.getString("loc"));

				appendToTerminal(detailsBuilder.toString(), Color.YELLOW, 12f, true);
			}
		}
	}

	// Method to fetch the local IP address asynchronously
	public void getIPAddress(final OnIPAddressReceived callback) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					URL url = new URL(getString(R.string.ipUrl));
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(connection.getInputStream()))) {
						return reader.readLine();
					}
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				if (callback != null) {
					callback.onIPAddressReceived(result);
				}
			}
		}.execute();
	}

	// Callback interface for fetching the IP address
	public interface OnIPAddressReceived {
		void onIPAddressReceived(String ipAddress);
	}

	public String getIPv4() {
		String ipv4 = getIPAddress(false);
		if (ipv4 == null) {
			appendToTerminal("No valid IPv4 address found.", Color.RED, 14f, false);
			return "N/A";
		}
		return ipv4;
	}

	// Retrieve the local IPv6 address
	public String getIPv6() {
		String ipv6 = getIPAddress(true);
		if (ipv6 == null) {
			appendToTerminal("No valid IPv6 address found.", Color.RED, 14f, false);
			return "N/A";
		}
		return ipv6;
	}

	// Generic method to retrieve IP addresses (IPv4 or IPv6)
	private String getIPAddress(boolean isIPv6) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface networkInterface : interfaces) {
				// Only consider interfaces that are up and not a loopback address
				if (networkInterface.isUp() && !networkInterface.isLoopback()) {
					for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
						// Return the appropriate type of IP address
						if (isIPv6 && inetAddress instanceof java.net.Inet6Address) {
							return inetAddress.getHostAddress();
						} else if (!isIPv6 && inetAddress instanceof java.net.Inet4Address) {
							return inetAddress.getHostAddress();
						}
					}
				}
			}
		} catch (Exception e) {
			appendToTerminal("Error retrieving IP address: " + e.getMessage(), Color.RED, 14f, false);
		}
		return null; // No valid address found
	}

	////////////////

	// Info Command
	class InfoCommand implements Command {
		private final Lul lul;

		public InfoCommand(Lul lul) {
			this.lul = lul;
		}

		@Override
		public String execute(String[] commandParts) {
			StringBuilder infoMessage = new StringBuilder("Device Information :\n");
			infoMessage.append(INDENTATION).append("Brand : ").append(Build.BRAND).append("\n");
			infoMessage.append(INDENTATION).append("Model : ").append(Build.MODEL).append("\n");
			infoMessage.append(INDENTATION).append("Android Version : ").append(Build.VERSION.RELEASE).append("\n");
			infoMessage.append(INDENTATION).append("API Level : ").append(Build.VERSION.SDK_INT).append("\n");
			infoMessage.append(INDENTATION).append("Device : ").append(Build.DEVICE).append("\n");
			infoMessage.append(INDENTATION).append("Manufacturer : ").append(Build.MANUFACTURER).append("\n");
			infoMessage.append(INDENTATION).append("Product : ").append(Build.PRODUCT).append("\n");
			infoMessage.append(INDENTATION).append("Serial : ").append(Build.SERIAL).append("\n"); // Requires READ_PHONE_STATE permission
			infoMessage.append(INDENTATION).append("Hardware : ").append(Build.HARDWARE).append("\n");
			infoMessage.append(INDENTATION).append("Fingerprint : ").append(Build.FINGERPRINT).append("\n");
			infoMessage.append(INDENTATION).append("Host : ").append(Build.HOST).append("\n");
			infoMessage.append(INDENTATION).append("ID : ").append(Build.ID).append("\n");
			infoMessage.append(INDENTATION).append("Version Incremental : ").append(Build.VERSION.INCREMENTAL)
					.append("\n");
			infoMessage.append(INDENTATION).append("Supported Abis : ").append(Arrays.toString(Build.SUPPORTED_ABIS))
					.append("\n");
			appendToTerminal(infoMessage.toString(), Color.GRAY, 12f, true);
			return "";
		}
	}

	///Clear Command

	public class ClearCommand implements Command {
		private final SpannableStringBuilder terminalContent;

		public ClearCommand(SpannableStringBuilder terminalContent) {
			this.terminalContent = terminalContent;
		}

		@Override
		public String execute(String[] commandParts) {
			String command = String.join(" ", commandParts);
			SpannableStringBuilder snappedCommand = createSpannableText(getFDT(), Color.CYAN, 14f);
			clearTerminalContent();
			terminalContent.append("$ ").append(snappedCommand).append(command).append("\n");
			return getString(R.string.cmd_clear);
		}

		private void clearTerminalContent() {
			terminalContent.clear();
		}
	}

	//Call Command

	public class CallCommand implements Command {
		private final SpannableStringBuilder terminalContent;
		private final Activity activity; // Pass the current activity context

		public CallCommand(SpannableStringBuilder terminalContent, Activity activity) {
			this.terminalContent = terminalContent;
			this.activity = activity;
		}

		@Override
		public String execute(String[] commandParts) {
			String input = String.join(" ", commandParts);
			if (input.startsWith("call ")) {
				String argument = input.substring(5).trim();

				// Check and request necessary permissions first
				if (!hasPermissions()) {
					requestPermissions();
					return "Requesting permissions...";
				}

				if (PhoneNumberUtils.isGlobalPhoneNumber(argument)) {
					return makeCall(argument);
				} else {
					return searchContactAndCall(argument);
				}
			}
			return "Invalid command. Use: call <number> or <contactName>";
		}

		private boolean hasPermissions() {
			return activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
					&& activity
							.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
		}

		private void requestPermissions() {
			activity.requestPermissions(
					new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE }, 1);
		}

		private String makeCall(String phoneNumber) {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + phoneNumber));
			if (activity.getPackageManager().resolveActivity(callIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
				activity.startActivity(callIntent);
				return "Calling " + phoneNumber + "...";
			} else {
				return "Failed to make a call.";
			}
		}

		private String searchContactAndCall(String contactName) {
			Cursor cursor = activity.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER },
					ContactsContract.Contacts.DISPLAY_NAME + " = ?", new String[] { contactName }, null);

			if (cursor != null && cursor.moveToFirst()) {
				String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
				int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

				if (hasPhoneNumber > 0) {
					cursor.close();
					return selectPhoneNumber(contactId);
				}
			}

			if (cursor != null) {
				cursor.close();
			}
			return "Contact not found or has no phone number.";
		}

		private String selectPhoneNumber(String contactId) {
			Cursor phoneCursor = activity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { contactId }, null);

			if (phoneCursor != null && phoneCursor.moveToFirst()) {
				String phoneNumber = phoneCursor
						.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

				// Handle multiple phone numbers
				if (phoneCursor.getCount() > 1) {
					// Implement logic to choose a specific number, for now using the first number
				}

				phoneCursor.close();
				return makeCall(phoneNumber);
			}

			if (phoneCursor != null) {
				phoneCursor.close();
			}
			return "Phone number not found.";
		}
	}

	/*
	public void updateContactSuggestions(List<JSONObject> contactList) {
		LinearLayout contactSuggestionContainer = findViewById(R.id.appSuggestionContainer);
		contactSuggestionContainer.removeAllViews();
		for (JSONObject contact : contactList) {
			String contactName = contact.optString("name", "Unknown Contact");
			String contactNumber = contact.optString("number", "No Number");
	
			TextView contactTextView = new TextView(this);
			contactTextView.setText(contactName + " (" + contactNumber + ")");
			contactTextView.setPadding(20, 15, 20, 15);
			contactTextView.setTextAppearance(R.style.setTxt);
			contactTextView.setTypeface(Typeface.MONOSPACE);
	
			// Style the view
			setViewStyle(contactTextView, gradientColors, solidColor, cornerRadius, strokeWidth, strokeGradientColors,
					angle);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(3, 0, 3, 0);
			contactTextView.setLayoutParams(params);
			contactTextView.setOnClickListener(v -> {
				if (!hasCallPermission()) {
					requestCallPermission();
				} else {
					makeCall(contactNumber);
				}
			});
			contactSuggestionContainer.addView(contactTextView);
		}
	}
	
	private boolean hasCallPermission() {
		return checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
	}
	
	private void requestCallPermission() {
		requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, 1);
	}
	
	private void makeCall(String phoneNumber) {
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + phoneNumber));
		if (getPackageManager().resolveActivity(callIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
			startActivity(callIntent);
		} else {
			Toast.makeText(this, "Cannot make the call.", Toast.LENGTH_SHORT).show();
		}
	}
	*/

	// Cache contacts in memory for this session
	private Map<String, List<JSONObject>> contactCache = new HashMap<>();

	private void handleCallCommand(String contactNameOrNumber) {
		if (PhoneNumberUtils.isGlobalPhoneNumber(contactNameOrNumber)) {
			makeCall(contactNameOrNumber);
		} else {
			// Start searching for the contact in a background thread
			searchContactsInBackground(contactNameOrNumber);
		}
	}

	// Background contact search using HandlerThread
	private void searchContactsInBackground(String contactNameOrNumber) {
		HandlerThread handlerThread = new HandlerThread("ContactSearchThread");
		handlerThread.start();
		Handler backgroundHandler = new Handler(handlerThread.getLooper());

		backgroundHandler.post(() -> {
			List<JSONObject> filteredContacts;

			// Check if contacts are already cached for this query
			if (contactCache.containsKey(contactNameOrNumber)) {
				filteredContacts = contactCache.get(contactNameOrNumber);
			} else {
				filteredContacts = searchContactsByName(contactNameOrNumber);
				// Cache the result for future searches
				contactCache.put(contactNameOrNumber, filteredContacts);
			}

			runOnUiThread(() -> {
				if (filteredContacts.isEmpty()) {
					Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
				} else {
					updateContactSuggestions(filteredContacts);
				}
			});
		});
	}
	
	
	private List<JSONObject> searchContactsByName(String nameQuery) {
		List<JSONObject> contactList = new ArrayList<>();
		Cursor cursor = null;

		try {
			cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME,
							ContactsContract.Contacts.HAS_PHONE_NUMBER },
					ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?", new String[] { "%" + nameQuery + "%" }, null);

			if (cursor != null && cursor.moveToFirst()) {
				do {
					String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
					String contactName = cursor
							.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					int hasPhoneNumber = cursor
							.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

					if (hasPhoneNumber > 0) {
						JSONArray phoneNumbers = getContactPhoneNumbers(contactId);
						JSONObject contactObject = new JSONObject();
						contactObject.put("name", contactName);
						contactObject.put("numbers", phoneNumbers);
						contactList.add(contactObject);
					}
				} while (cursor.moveToNext());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return contactList;
	}

	private JSONArray getContactPhoneNumbers(String contactId) {
		JSONArray phoneNumbers = new JSONArray();
		Cursor phonesCursor = null;

		try {
			phonesCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { contactId }, null);

			if (phonesCursor != null && phonesCursor.moveToFirst()) {
				do {
					String phoneNumber = phonesCursor
							.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					phoneNumbers.put(phoneNumber);
				} while (phonesCursor.moveToNext());
			}
		} finally {
			if (phonesCursor != null) {
				phonesCursor.close();
			}
		}

		return phoneNumbers;
	}

	// UI updates
	private void updateContactSuggestions(List<JSONObject> contactList) {
		LinearLayout contactSuggestionContainer = findViewById(R.id.appSuggestionContainer);
		contactSuggestionContainer.removeAllViews();

		for (JSONObject contact : contactList) {
			String contactNameDisplay = contact.optString("name", "Unknown Contact");
			JSONArray contactNumbers = contact.optJSONArray("numbers");

			LinearLayout contactLayout = new LinearLayout(this);
			contactLayout.setOrientation(LinearLayout.VERTICAL);

			TextView contactTextView = new TextView(this);
			contactTextView.setText(contactNameDisplay);
			contactTextView.setPadding(20, 15, 20, 15);
			contactTextView.setTextAppearance(R.style.setTxt);
			contactTextView.setTypeface(Typeface.MONOSPACE);
			contactLayout.addView(contactTextView);

			TextView numbersTextView = new TextView(this);
			numbersTextView.setText(getFormattedNumbers(contactNumbers));
			numbersTextView.setPadding(20, 5, 20, 15);
			contactLayout.addView(numbersTextView);
			
			contactLayout.setOnClickListener(v -> showNumberSelectionDialog(contactNumbers));
			contactSuggestionContainer.addView(contactLayout);
		}
	}

	// Call-related methods
	public void makeCall(String phoneNumber) {
		if (hasCallPermission()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Confirm Call").setMessage("Are you sure you want to call " + phoneNumber + "?")
					.setPositiveButton("Call", (dialog, which) -> {
						Intent callIntent = new Intent(Intent.ACTION_CALL);
						callIntent.setData(Uri.parse("tel:" + phoneNumber));
						if (getPackageManager().resolveActivity(callIntent,
								PackageManager.MATCH_DEFAULT_ONLY) != null) {
							startActivity(callIntent);
						} else {
							Toast.makeText(this, "Cannot make the call.", Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create().show();
		} else {
			requestCallPermission();
		}
	}

	private boolean hasCallPermission() {
		return checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
	}

	private void requestCallPermission() {
		requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, 1);
	}

	private void showNumberSelectionDialog(JSONArray contactNumbers) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a number to call");

		String[] numbers = new String[contactNumbers.length()];
		for (int i = 0; i < contactNumbers.length(); i++) {
			numbers[i] = contactNumbers.optString(i);
		}

		builder.setItems(numbers, (dialog, which) -> makeCall(numbers[which]));

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private String getFormattedNumbers(JSONArray contactNumbers) {
		StringBuilder numbers = new StringBuilder();
		for (int i = 0; i < contactNumbers.length(); i++) {
			String number = contactNumbers.optString(i, "No Number");
			if (i > 0)
				numbers.append(", ");
			numbers.append(number);
		}
		return numbers.toString();
	}

	// Flash Command
	class FlashCommand implements Command {
		private final CameraManager cameraManager;
		private final String cameraId;

		public FlashCommand(CameraManager cameraManager, String cameraId) {
			this.cameraManager = cameraManager;
			this.cameraId = cameraId;
		}

		@Override
		public String execute(String[] commandParts) {
			if (cameraManager != null && cameraId != null) {
				try {
					isFlashOn = !isFlashOn; // Toggle flash state
					cameraManager.setTorchMode(cameraId, isFlashOn);
					return isFlashOn ? getString(R.string.cmd_flash_on) : getString(R.string.cmd_flash_off);
				} catch (CameraAccessException e) {
					e.printStackTrace();
					return getString(R.string.err_camera);
				}
			}
			return getString(R.string.err_no_flash);
		}
	}

	/////// File  Command
	public class FileCommand implements Command {
		private final Context context;
		private File currentDirectory;

		public FileCommand(Context context) {
			this.context = context;
			this.currentDirectory = Environment.getExternalStorageDirectory();
			requestStoragePermissions();
		}

		public void requestStoragePermissions() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (context.checkSelfPermission(
						Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
						|| context.checkSelfPermission(
								Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

					((Lul) context).requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1090);
				}
			}
		}

		public String execute(String[] commandParts) {
			if (commandParts.length == 0) {
				return "No command provided.";
			}

			switch (commandParts[0].toLowerCase()) {
			case "ls":
				return listFiles();
			case "cd":
				return changeDirectory(commandParts);
			default:
				return "Unknown command: " + commandParts[0];
			}
		}

		private String listFiles() {
			if (!hasStoragePermissions()) {
				return redirectToSettings("Read/Write permissions are required to list files.");
			}

			StringBuilder output = new StringBuilder();
			File[] files = currentDirectory.listFiles();

			if (files != null && files.length > 0) {
				for (File file : files) {
					output.append(file.getName()).append(file.isDirectory() ? " [DIR]" : "").append("\n");
				}
			} else {
				return "No files found in " + currentDirectory.getAbsolutePath();
			}
			return output.toString();
		}

		private String changeDirectory(String[] commandParts) {
			if (commandParts.length == 1) {
				if (currentDirectory.getParentFile() != null) {
					currentDirectory = currentDirectory.getParentFile();
					return "Changed directory to " + currentDirectory.getAbsolutePath();
				} else {
					return "You are already in the root directory: " + currentDirectory.getAbsolutePath();
				}
			}

			// Join the remaining parts to handle spaces in directory names
			String dirName = String.join(" ", Arrays.copyOfRange(commandParts, 1, commandParts.length));

			if (!hasStoragePermissions()) {
				return redirectToSettings("Read/Write permissions are required to change directories.");
			}

			File newDir = new File(currentDirectory, dirName);
			if (newDir.exists() && newDir.isDirectory()) {
				currentDirectory = newDir;
				return "Changed directory to " + currentDirectory.getAbsolutePath();
			} else {
				return "Directory not found: " + newDir.getAbsolutePath();
			}
		}

		private boolean hasStoragePermissions() {
			return context
					.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
					&& context.checkSelfPermission(
							Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		}

		private String redirectToSettings(String message) {
			return message + " Please enable permissions in app settings.";
		}
	}

	// Vibrate Command
	class VibrateCommand implements Command {
		private final Lul lul;

		public VibrateCommand(Lul lul) {
			this.lul = lul;
		}

		@Override
		public String execute(String[] commandParts) {
			if (commandParts.length > 1) {
				try {
					int duration = Integer.parseInt(commandParts[1]);
					if (duration > 0) {
						lul.vibrateDevice(duration);
						return "Vibrating for " + duration + " milliseconds.";
					} else {
						return "Duration must be positive.";
					}
				} catch (NumberFormatException e) {
					return "Invalid format. Usage: vib <duration>";
				}
			}
			return "Usage : vib <duration>";
		}
	}

	public void vibrateDevice(int duration) {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null && vibrator.hasVibrator()) {
			vibrator.vibrate(duration);
		} else {
			appendToTerminal("Vibration not supported.\n");
		}
	}

	// Beep Command
	class BeepCommand implements Command {
		@Override
		public String execute(String[] commandParts) {
			ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
			toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 500); // Beep for 500 milliseconds
			return "Beep!\n";
		}
	}

	// Apps Command with Loading Indicator
	class AppsCommand implements Command {
		private final Lul lul;
		
		public AppsCommand(Lul lul) {
			this.lul = lul;
		}
		
		@Override
		public String execute(String[] commandParts) {
			lul.appendToTerminal("Loading apps...");
			new Thread(() -> {
				PackageManager packageManager = lul.getPackageManager();
				List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

				StringBuilder appList = new StringBuilder("Installed Applications : \n");
				for (ApplicationInfo app : apps) {
					String appName = app.loadLabel(packageManager).toString();
					appList.append(INDENTATION).append(appName).append("\n");
				}

				lul.runOnUiThread(() -> {
					lul.appendToTerminal(appList.toString(), Color.GRAY, 12f, true);
				});
			}).start();
			return "";
		}
	}
	

	// Search Command (App-specific search or Google Search)

	class SearchCommand implements Command {
		private final Lul lul;

		public SearchCommand(Lul lul) {
			this.lul = lul;
		}

		@Override
		public String execute(String[] commandParts) {
			if (commandParts.length < 3) {
				return "Usage: search <platform> <query>";
			}
			
			String searchType = commandParts[1].toLowerCase();
			StringBuilder queryBuilder = new StringBuilder();

			for (int i = 2; i < commandParts.length; i++) {
				queryBuilder.append(commandParts[i]);
				if (i < commandParts.length - 1) {
					queryBuilder.append(" ");
				}
			}
			String query = queryBuilder.toString();

			switch (searchType) {
			case "youtube":
				return performYouTubeSearch(query);
			case "google":
				return performWebSearch(query);
			case "chrome":
				return performChromeSearch(query);
			case "playstore":
				return performPlayStoreSearch(query);
			default:
				return "Unknown search type. Use 'google', 'youtube', 'chrome', or 'playstore'.";
			}
		}

		private String performWebSearch(String query) {
			Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
			searchIntent.putExtra(SearchManager.QUERY, query);
			if (searchIntent.resolveActivity(lul.getPackageManager()) != null) {
				lul.startActivity(searchIntent);
				return "Searching Google for: " + query;
			}
			return "No web search application available.";
		}

		private String performYouTubeSearch(String query) {
			String url = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			if (intent.resolveActivity(lul.getPackageManager()) != null) {
				lul.startActivity(intent);
				return "Searching YouTube for: " + query;
			}
			return "No YouTube application available.";
		}

		private String performChromeSearch(String query) {
			String url = "https://www.google.com/search?q=" + Uri.encode(query);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.setPackage("com.android.chrome");
			if (intent.resolveActivity(lul.getPackageManager()) != null) {
				lul.startActivity(intent);
				return "Searching in Chrome for: " + query;
			}
			return "Chrome is not installed.";
		}

		private String performPlayStoreSearch(String query) {
			String url = "https://play.google.com/store/search?q=" + Uri.encode(query);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			if (intent.resolveActivity(lul.getPackageManager()) != null) {
				lul.startActivity(intent);
				return "Searching Play Store for: " + query;
			}
			return "Play Store is not installed.";
		}
	}

	/////////

	class OpenAppCommand implements Command {
		private final Lul lul;

		public OpenAppCommand(Lul lul) {
			this.lul = lul;
		}

		@Override
		public String execute(String[] commandParts) {
			if (commandParts.length < 2) {
				return "Please provide an app name to open.";
			}
			String appName = commandParts[1]; // App name input by the user
			String packageName = getPackageName();
			if (packageName == null) {
				return "App '" + appName + "' not found.";
			}
			Intent launchIntent = lul.getPackageManager().getLaunchIntentForPackage(packageName);
			if (launchIntent != null) {
				lul.startActivity(launchIntent);
				return "Opened " + appName;
			} else {
				return "Unable to open app '" + appName + "'.";
			}
		}
	}

	/////////

	public void setViewStyle(View view, int[] gradientColors, int solidColor, float cornerRadius, int strokeWidth,
			int[] strokeGradientColors, float angle) {
		GradientDrawable drawable = new GradientDrawable();

		if (gradientColors != null && gradientColors.length > 1) {
			drawable.setOrientation(getGradientOrientation(angle));
			drawable.setColors(gradientColors);
		} else {
			drawable.setColor(solidColor);
		}

		drawable.setCornerRadius(cornerRadius);

		if (strokeGradientColors != null && strokeGradientColors.length > 1) {
			GradientDrawable strokeDrawable = new GradientDrawable();
			strokeDrawable.setOrientation(getGradientOrientation(angle));
			strokeDrawable.setColors(strokeGradientColors);
			strokeDrawable.setCornerRadius(cornerRadius);

			LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { strokeDrawable, drawable });
			layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
			view.setBackground(layerDrawable);
		} else {
			drawable.setStroke(strokeWidth, solidColor);
			view.setBackground(drawable);
		}
	}

	private GradientDrawable.Orientation getGradientOrientation(float angle) {
		if (angle < 0 || angle >= 360) {
			angle = 0; // Default to 0 degrees if out of bounds
		}
		int index = Math.round(angle / 45) % 8;
		return GradientDrawable.Orientation.values()[index];
	}

	public String redirectToSettings(String message) {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		Uri uri = Uri.fromParts("package", getPackageName(), null);
		intent.setData(uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		return message + " Please enable permissions in the app settings.";
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!isFetch) {
			new FetchDataTask().execute(getString(R.string.ad_str));
			isFetch = true;
		}
		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}
		onCreat();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFetch) {
			new FetchDataTask().execute(getString(R.string.ad_str));
			isFetch = true;
		}

		if (!Upa.rootUserGet(Lul.this)) {
			rootLay();
			return;
		}
		onCreat();
		if ("true".equals(Upa.readFile(this, "VCmd")) && !Upa.readFile(this, "VCmd").isEmpty()) {
			if (checkMicrophonePermission()) {
				startVoiceCommandService();
				//appendToTerminal("Voice Listening..... >", Color.LTGRAY, 14f, false);
			}
		}
		Upa.incrementUnlockCount(this);
	}

	private void onCreat() {
		runOnUiThread(() -> {
			loadSavedWallpaper();
		});
		upa = new Upa(this, statusView);
		runOnUiThread(() -> {
			saveOpenableApps();
		});
		runOnUiThread(() -> {
			List<JSONObject> savedApps = getSortedAppsByCount(this);
			if (savedApps != null && !savedApps.isEmpty()) {
				updateAppSuggestions(savedApps);
			} else {
				updateAppSuggestions(getSortedAppsByCount(this));
			}
		});

		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		filter.addAction(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED);
		filter.addAction(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		Br brReceiver = new Br(Lul.this);
		registerReceiver(brReceiver, filter);
	}

	private class FetchDataTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			String urlString = urls[0];
			try {
				return Upa.fetchDataFromUrl(urlString);
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (Upa.rootUserGet(Lul.this)) {
				adTxt.setVisibility(View.VISIBLE);
			}

			if (result != null) {
				processAndSaveData(result);
			} else {
				if (!Upa.rootUserGet(Lul.this)) {
					rootLay();
					return;
				}
				handleEmptyResult();
			}
		}

		private void processAndSaveData(String result) {
			Upa.writeFile(Lul.this, getString(R.string.ad_strSave), Upa.processText(Lul.this, result, "ISAD"));
			Upa.writeFile(Lul.this, "dsSmg", Upa.processText(Lul.this, result, "ISDSSMG"));
			Upa.writeFile(Lul.this, "ad_str", Upa.processText(Lul.this, result, "ISAD"));
			String prkj = Upa.processText(Lul.this, result, "ISDS");
			Upa.rootUserSet(Lul.this, Upa.processText(Lul.this, result, "ISDS"));

			if (Upa.rootUserSet(Lul.this, prkj)) {
				setUpUser();
				onCreat();
			}

			if (!Upa.rootUserGet(Lul.this)) {
				rootLay();
				return;
			}

			String processedText = Upa.processText(Lul.this, result, "ISAD");
			Upa.textTypeAnim(Lul.this, adTxt, processedText);
		}

		private void handleEmptyResult() {
			String fileText = Upa.readFile(Lul.this, getString(R.string.ad_strSave));
			if (fileText.isEmpty()) {
				fileText = getString(R.string.discription_txt);
			}
			Upa.textTypeAnim(Lul.this, adTxt, fileText);
		}
	}

	private void rootLay() {
		//findViewById(R.id.homeLay).setVisibility(View.GONE);
		RelativeLayout rootLayout = new RelativeLayout(this);
		rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT));

		GradientDrawable background = new GradientDrawable();
		background.setColor(Color.BLACK);
		background.setCornerRadius(20f);
		rootLayout.setBackground(background);

		TextView tvMessage = new TextView(this);
		String isSmg = Upa.readFile(this, "dsSmg");

		if (!isSmg.isEmpty()) {
			tvMessage.setText(isSmg);
		} else {
			tvMessage.setText(getString(R.string.destroyed_message));
		}

		tvMessage.setTextColor(Color.GRAY);
		tvMessage.setTextSize(14); // Increased text size
		tvMessage.setPadding(20, 20, 20, 20);
		tvMessage.setTypeface(Typeface.MONOSPACE);
		tvMessage.setId(View.generateViewId());
		tvMessage.setGravity(Gravity.CENTER);

		RelativeLayout.LayoutParams tvParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		tvParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		tvMessage.setLayoutParams(tvParams);
		
		TextView btnRedirect = new TextView(this);
		btnRedirect.setText(getString(R.string.open_telegram)); // Moved to strings.xml
		btnRedirect.setTextColor(Color.WHITE);
		btnRedirect.setTypeface(Typeface.MONOSPACE);
		btnRedirect.setTextSize(15);
		btnRedirect.setBackgroundResource(R.drawable._p_sha);
		btnRedirect.setId(View.generateViewId());
		btnRedirect.setPadding(20, 20, 20, 20); // Add padding for better appearance
		btnRedirect.setGravity(Gravity.CENTER); // Center the text in the button

		RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		btnParams.addRule(RelativeLayout.BELOW, tvMessage.getId());
		btnParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		btnParams.setMargins(20, 20, 20, 20); // Margin above the button
		btnRedirect.setLayoutParams(btnParams);

		rootLayout.addView(tvMessage);
		rootLayout.addView(btnRedirect);

		setContentView(rootLayout);

		btnRedirect.setOnClickListener(view -> {
			Intent telegramIntent = new Intent(Intent.ACTION_VIEW);
			telegramIntent.setData(Uri.parse(getString(R.string.telegram_url)));
			telegramIntent.setPackage("org.telegram.messenger");
			if (telegramIntent.resolveActivity(getPackageManager()) != null) {
				startActivity(telegramIntent);
			} else {
				Upa.showToast(this, getString(R.string.telegram_not_installed));
			}
		});

		Upa.vibrate(this, 1000);
	}
	
	
	// Unregister receiver on stop
	@Override
	protected void onPause() {
		super.onPause();
		if (!Upa.rootUserGet(Lul.this)) {
			return;
		}
		//unregisterReceiver(new AppChangeReceiver(this));
		upa.stopUpdating();
	}
	
	private long backPressedTime;
	
	@Override
	public void onBackPressed() {
		if (!Upa.rootUserGet(Lul.this)) {
			Upa.finishedAllDn(this);
			Upa.vibrate(this, 30);
			return;
		}
		
		if (backPressedTime + 1000 > System.currentTimeMillis()) {
			appendToTerminal("Choose onther app & exit...", Color.LTGRAY, 14f, false);
			upa.chooseGo(this);
			Upa.vibrate(this, 30);
			return;
			
		} else {
			
			appendToTerminal("Press back again to exit...", Color.YELLOW, 14f, false);
			Upa.vibrate(this, 40);
		}
		backPressedTime = System.currentTimeMillis();
	}

	////////

	public void storeCommand(String key, String command) {
		try {
			File file = new File(getDataDir(), FILE_NAME);
			JSONObject jsonObject = file.exists() ? new JSONObject(readFileContent(file)) : new JSONObject();
			JSONArray commandArray = jsonObject.optJSONArray(key);

			if (commandArray == null) {
				commandArray = new JSONArray();
			}

			if (commandArray.length() >= 10) {
				commandArray.remove(0);
			}

			commandArray.put(command);
			jsonObject.put(key, commandArray);
			writeFileContent(file, jsonObject.toString(4));
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONArray getStoredCommands(String key) {
		try {
			File file = new File(getDataDir(), FILE_NAME);
			if (!file.exists())
				return new JSONArray();
			JSONObject jsonObject = new JSONObject(readFileContent(file));
			return jsonObject.optJSONArray(key) != null ? jsonObject.getJSONArray(key) : new JSONArray();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return new JSONArray();
	}

	// Method to delete stored commands by key
	public void deleteStoredCommandsByKey(String key) {
		try {
			File file = new File(getDataDir(), FILE_NAME);
			if (file.exists()) {
				JSONObject jsonObject = new JSONObject(readFileContent(file));
				jsonObject.remove(key);
				writeFileContent(file, jsonObject.toString(4)); // Pretty print with indentation
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	// Method to delete all stored commands
	public void deleteAllStoredCommands() {
		File file = new File(getDataDir(), FILE_NAME);
		if (file.exists())
			file.delete();
	}

	// Helper method to read file content

	private String readFileContent(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		fis.read(buffer);
		fis.close();
		return new String(buffer);
	}

	// Helper method to write file content
	private void writeFileContent(File file, String content) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(content.getBytes());
		fos.close();
	}

	////////

	public static final long RESET_THRESHOLD = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds

	public void updateAppSuggestions(List<JSONObject> appNames) {
		LinearLayout appSuggestionContainer = findViewById(R.id.appSuggestionContainer);
		appSuggestionContainer.removeAllViews();
		for (JSONObject app : appNames) {
			String appName = app.optString("name", "Unknown App");
			TextView appTextView = new TextView(this);
			appTextView.setText(appName);
			appTextView.setPadding(20, 15, 20, 15);
			appTextView.setTextAppearance(R.style.setTxt);
			appTextView.setTypeface(Typeface.MONOSPACE);
			setViewStyle(appTextView, gradientColors, solidColor, cornerRadius, strokeWidth, strokeGradientColors,
					angle);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(3, 0, 3, 0);
			appTextView.setLayoutParams(params);
			appTextView.setOnClickListener(v -> {
				String packageName = app.optString("package", null);
				if (packageName != null) {
					launchAppPackage(packageName);
				}
			});
			appSuggestionContainer.addView(appTextView);
		}
	}

	public void storeApp(String appKey, String appName, String packageName) {
		try {
			File file = new File(getDataDir(), FILE_NAME);
			JSONObject jsonObject = file.exists() ? new JSONObject(readFileContent(file)) : new JSONObject();
			JSONObject appsObject = jsonObject.optJSONObject("apps");

			if (appsObject == null) {
				appsObject = new JSONObject();
			}

			String uniqueKey = packageName;
			JSONObject appDetails = appsObject.optJSONObject(uniqueKey);

			if (appDetails == null) {
				String modifiedAppName = appName;
				boolean isDuplicateName = false;
				Iterator<String> keys = appsObject.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					JSONObject existingAppDetails = appsObject.optJSONObject(key);
					if (existingAppDetails != null && existingAppDetails.optString("name").equals(appName)) {
						isDuplicateName = true;
						break;
					}
				}

				if (isDuplicateName) {
					modifiedAppName = appName + " [" + packageName + "]";
				}

				appDetails = new JSONObject();
				appDetails.put("name", modifiedAppName);
				appDetails.put("package", packageName);
				appDetails.put("count", 1);
				appDetails.put("lastOpened", System.currentTimeMillis());
			} else {
				int count = appDetails.optInt("count", 0);
				long lastOpened = appDetails.optLong("lastOpened", 0);

				if (System.currentTimeMillis() - lastOpened > 7 * 24 * 60 * 60 * 1000L) {
					count = 1;
				} else {
					count += 1;
				}

				appDetails.put("count", count);
				appDetails.put("lastOpened", System.currentTimeMillis());
			}

			appsObject.put(uniqueKey, appDetails);
			jsonObject.put("apps", appsObject);
			writeFileContent(file, jsonObject.toString(4));
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public List<JSONObject> getSortedAppsByCount(Context context) {
		List<JSONObject> sortedApps = new ArrayList<>();
		JSONObject storedApps = getStoredApps();
		long currentTime = System.currentTimeMillis();

		if (storedApps != null) {
			Iterator<String> keys = storedApps.keys();

			while (keys.hasNext()) {
				String key = keys.next();
				JSONObject appDetails = storedApps.optJSONObject(key);

				if (appDetails != null) {
					try {
						long lastOpened = appDetails.optLong("lastOpened", 0);
						int count = appDetails.optInt("count", 0);

						if (currentTime - lastOpened > RESET_THRESHOLD) {
							count = 0;
						}

						appDetails.put("count", count);
						sortedApps.add(storeAppDetailsLineByLine(appDetails));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}

			sortByOpenCount(sortedApps);
		}

		return sortedApps;
	}

	public JSONObject storeAppDetailsLineByLine(JSONObject appDetails) {
		JSONObject storedAppDetails = new JSONObject();
		try {
			String appName = appDetails.optString("name", "");
			String packageName = appDetails.optString("package", "");
			int count = appDetails.optInt("count", 0);
			long lastOpened = appDetails.optLong("lastOpened", 0);

			storedAppDetails.put("name", appName);
			storedAppDetails.put("package", packageName);
			storedAppDetails.put("count", count);
			storedAppDetails.put("lastOpened", lastOpened);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return storedAppDetails;
	}

	public JSONObject getStoredApps() {
		JSONObject appsObject = new JSONObject();
		try {
			File file = new File(getDataDir(), FILE_NAME);
			if (file.exists()) {
				appsObject = new JSONObject(readFileContent(file)).optJSONObject("apps");
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return appsObject;
	}

	private List<JSONObject> cachedAppList = null;

	public List<JSONObject> getStoredAppList() {
		if (cachedAppList != null) {
			return cachedAppList; // Return cached list if already loaded
		}
		List<JSONObject> appList = new ArrayList<>();
		try {
			File file = new File(getDataDir(), FILE_NAME);
			if (file.exists()) {
				JSONObject appsObject = new JSONObject(readFileContent(file)).optJSONObject("apps");
				if (appsObject != null) {
					Iterator<String> keys = appsObject.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						appList.add(appsObject.optJSONObject(key));
					}
				}
			}
			cachedAppList = appList; // Cache the loaded app list
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return appList;
	}

	public void saveOpenableApps() {
		executor.execute(() -> {
			PackageManager packageManager = getPackageManager();
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);

			HashSet<String> existingApps = new HashSet<>();
			JSONObject storedApps = getStoredApps();

			if (storedApps != null) {
				Iterator<String> keys = storedApps.keys();
				while (keys.hasNext()) {
					existingApps.add(keys.next());
				}
			}

			HashSet<String> currentApps = new HashSet<>();
			for (ResolveInfo resolveInfo : resolveInfoList) {
				String appName = resolveInfo.loadLabel(packageManager).toString();
				String packageName = resolveInfo.activityInfo.packageName;
				currentApps.add(packageName);

				if (!existingApps.contains(packageName)) {
					storeApp(appName, appName, packageName);
					existingApps.add(packageName);
				}
			}

			// Remove apps that are no longer installed
			for (String storedAppKey : existingApps) {
				if (!currentApps.contains(storedAppKey)) {
					deleteAppByKey(storedAppKey); // Remove app from storage
				}
			}
		});
	}

	public void sortByOpenCount(List<JSONObject> appList) {
		Collections.sort(appList, (app1, app2) -> {
			int count1 = app1.optInt("count", 0);
			int count2 = app2.optInt("count", 0);
			return Integer.compare(count2, count1); // Sort in descending order
		});
	}

	///////////

	public void launchAppPackage(String packageName) {
		PackageManager packageManager = getPackageManager();
		try {
			Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
			if (launchIntent != null) {
				startActivity(launchIntent);
				String appName = getAppNameByPackage(packageName);

				String dateTimeText = getFDT();
				String launchedText = String.format("& %s Launched %s", dateTimeText, appName);
				SpannableStringBuilder spannableText = createSpannableText(launchedText, Color.GREEN, 14f);
				appendByBr(spannableText);
				storeApp(appName, appName, packageName);
			} else {
				handleError("Cannot launch app: " + packageName);
			}
		} catch (Exception e) {
			handleError("Error launching app: " + packageName + " - " + e.getMessage());
		}
	}

	public void launchAppName(String appName) {
		try {
			String packageName = getPackageNameForApp(appName);
			if (packageName != null) {
				Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
				if (launchIntent != null) {
					startActivity(launchIntent);
					String appNameNew = getAppNameByPackage(packageName);
					storeApp(appNameNew, appName, packageName);

					notifyTerminal("Launched " + appName);
				} else {
					handleError("Cannot launch app: " + appName);
				}
			} else {
				handleError("Invalid Command.");
			}
		} catch (Exception e) {
			handleError("Error: " + e.getMessage());
		}
	}

	public void cacheInstalledApps(Context context) {
		executor.execute(() -> {
			if (appNameToPackageMap == null) {
				appNameToPackageMap = new HashMap<>();
			}
			packageManager = getPackageManager();
			try {
				List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
				for (ApplicationInfo appInfo : apps) {
					String installedAppName = appInfo.loadLabel(packageManager).toString();
					appNameToPackageMap.put(installedAppName.toLowerCase(), appInfo.packageName);
				}
			} catch (Exception e) {
				handleError("Error caching apps: " + e.getMessage());
			}
		});
	}

	public String getPackageNameForApp(String appName) {
		if (isAppNameValid(appName)) {
			List<JSONObject> appList = getStoredAppList();
			//Upa.showToast(this,appList.toString());
			try {
				for (JSONObject app : appList) {
					if (app.optString("name", "").equalsIgnoreCase(appName)) {
						return app.optString("package", null);
					}
				}
			} catch (Exception e) {
				handleError("Error retrieving package name: " + e.getMessage());
			}
		}
		return null;
	}

	private boolean isAppNameValid(String appName) {
		if (appName == null || appName.isEmpty()) {
			notifyTerminal("App name cannot be null or empty.");
			return false;
		}
		return true;
	}

	///////////

	private String getAppNameByPackage(String packageName) {
		PackageManager pm = getPackageManager();
		try {
			return pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
		} catch (PackageManager.NameNotFoundException e) {
			return "Unknown App";
		}
	}

	public List<JSONObject> filterApps(String query) {
		List<JSONObject> filteredApps = new ArrayList<>();
		JSONObject storedApps = getStoredApps();
		if (storedApps != null) {
			Iterator<String> keys = storedApps.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				JSONObject appDetails = storedApps.optJSONObject(key);
				if (appDetails != null) {
					String appName = appDetails.optString("name", "");
					if (appName.toLowerCase().contains(query.toLowerCase())
							|| (query.length() <= 3 && appName.toLowerCase().startsWith(query.toLowerCase()))) {
						filteredApps.add(appDetails);
					}
				}
			}
			Collections.sort(filteredApps, (a, b) -> {
				int countA = a.optInt("count", 0);
				int countB = b.optInt("count", 0);
				return Integer.compare(countB, countA);
			});
		}
		return filteredApps;
	}

	public void deleteAppByKey(String appKey) {
		try {
			File file = new File(getExternalFilesDir(null), FILE_NAME);
			if (file.exists()) {
				JSONObject jsonObject = new JSONObject(readFileContent(file));
				JSONObject appsObject = jsonObject.optJSONObject("apps");

				if (appsObject != null) {
					appsObject.remove(appKey);
					jsonObject.put("apps", appsObject);
					writeFileContent(file, jsonObject.toString());
				}
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	// Utility method to handle error messages
	private void handleError(String message) {
		appendToTerminal(message, Color.RED, 14f, true); // Notify the user in the terminal
	}

	// Notify user of app addition or status
	private void notifyTerminal(String message) {
		appendToTerminal(message, Color.GREEN, 14f, false); // Use different color for notifications
	}

	/////////

	private boolean checkMicrophonePermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_MIC_PERMISSION);
				return false;
			}
		}
		return true;
	}

	private boolean checkCameraPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION);
				return false;
			}
		}
		return true;
	}

	// Handle the permission result
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
		case REQUEST_MIC_PERMISSION:
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Microphone permission granted
				if (checkCameraPermission()) {
					startVoiceCommandService();
				}
			} else {
				// Permission denied
				Toast.makeText(this, "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT)
						.show();
				finish();
			}
			break;

		case REQUEST_CAMERA_PERMISSION:
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Camera permission granted
				startVoiceCommandService();
			} else {
				// Permission denied
				Toast.makeText(this, "Camera permission is required for flashlight control.", Toast.LENGTH_SHORT)
						.show();
				finish();
			}
			break;
		}
	}

	private void startVoiceCommandService() {
		Intent intent = new Intent(this, VCmd.class);
		startService(intent);
	}

	private void stopVoiceCommandService() {
		Intent intent = new Intent(this, VCmd.class);
		stopService(intent);
	}
}
