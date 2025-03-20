package the.ghost;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Window;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.StatFs;
import android.os.Environment;
import android.widget.LinearLayout;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import android.widget.TextView;
import java.lang.reflect.Method;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.EditText;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Upa {

	public static boolean isAppAction = true;
	public static final String FILE_NAMEAD = "apps.json";

	/////////

	private final Lul context;
	private final Handler handler = new Handler(Looper.getMainLooper());
	//Handler();
	private final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
	private final TextView textView;
	private boolean isUpdating = false;

	public Upa(Lul context, TextView textView) {
		this.context = context;
		this.textView = textView;
		startUpdating();
	}

	private void startUpdating() {
		isUpdating = true;
		handler.post(updateRunnable);
	}

	public void stopUpdating() {
		isUpdating = false;
		handler.removeCallbacks(updateRunnable);
		spannableStringBuilder.clear();
		//textView.setText("");
	}

	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			if (isUpdating) {
				updateDeviceInfo();
				handler.postDelayed(this, 20); // Update every 20ms
			}
		}
	};

	private void updateDeviceInfo() {
		spannableStringBuilder.clear();

		appendColoredInfo(context.getString(R.string.storage_label), " " + getStorageInfo());
		appendColoredInfo(context.getString(R.string.wifi_label), " " + getWifiStatus());
		appendColoredInfo(context.getString(R.string.bluetooth_label), " " + getBluetoothStatus());
		appendColoredInfo(context.getString(R.string.internet_label), " " + getInternetStatus().toLowerCase());
		appendColoredInfo(context.getString(R.string.ram_label), " " + getRamInfo());
		appendColoredInfo(context.getString(R.string.cpu_label), " " + getCpuInfo());
		appendColoredInfo(context.getString(R.string.battery_label), " " + getBatteryInfo());
		appendColoredInfo(context.getString(R.string.rooted_label), " " + (isDeviceRooted() ? "Rooted" : "Not rooted"));
		appendColoredInfo(context.getString(R.string.unlock_count_label), " " + getUnlockInfo(context));
		appendColoredInfo(context.getString(R.string.fdt_label), getFDT(), Color.CYAN); // CYAN color for FDT

		textView.setText(spannableStringBuilder);
	}

	private void appendColoredInfo(String label, String info) {
		appendColoredInfo(label, info, getColorForInfo(label, info));
	}

	private void appendColoredInfo(String label, String info, int infoColor) {
		int labelColor = Color.GREEN; // Default color for labels

		// Add label with default color
		spannableStringBuilder.append(label);
		int labelStart = spannableStringBuilder.length() - label.length();
		spannableStringBuilder.setSpan(new ForegroundColorSpan(labelColor), labelStart, spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		// Add info with its respective color
		spannableStringBuilder.append(info);
		int infoStart = spannableStringBuilder.length() - info.length();
		spannableStringBuilder.setSpan(new ForegroundColorSpan(infoColor), infoStart, spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private String getStorageInfo() {
		StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
		long totalStorage = stat.getBlockSizeLong() * stat.getBlockCountLong();
		long availableStorage = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
		long usedStorage = totalStorage - availableStorage;

		return String.format(Locale.US, "%s | %s | %s [%.0f%%]", formatSize(availableStorage), formatSize(usedStorage),
				formatSize(totalStorage), (float) ((usedStorage * 100) / totalStorage));
	}

	public boolean isDeviceRooted() {
		String[] paths = { "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
				"/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su" };
		for (String path : paths) {
			if (new File(path).exists())
				return true;
		}
		return false;
	}

	private String getWifiStatus() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		return (wifiManager != null && wifiManager.isWifiEnabled()) ? context.getString(R.string.wifi_enabled)
				: context.getString(R.string.wifi_disabled);
	}

	private String getBluetoothStatus() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		return (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
				? context.getString(R.string.bluetooth_enabled)
				: context.getString(R.string.bluetooth_disabled);
	}

	private String getInternetStatus() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
		String type = activeNetwork != null ? activeNetwork.getTypeName() : context.getString(R.string.no_connection);
		boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
		return isConnected ? type : context.getString(R.string.not_connected);
	}

	private String getRamInfo() {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);

		long totalRam = memoryInfo.totalMem;
		long usedRam = totalRam - memoryInfo.availMem;

		return String.format(Locale.US, "%s | %s (%.2f%%)", formatSize(totalRam), formatSize(usedRam),
				(float) (usedRam * 100) / totalRam);
	}

	private String getCpuInfo() {
		int numCores = Runtime.getRuntime().availableProcessors();
		return String.format("%d %s | %s MHz", numCores, context.getString(R.string.cores), getCpuMaxFreq());
	}

	private String getCpuMaxFreq() {
		try (RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq",
				"r")) {
			return (Long.parseLong(reader.readLine()) / 1000) + "";
		} catch (Exception e) {
			return context.getString(R.string.unavailable);
		}
	}

	private String getBatteryInfo() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL;

		return String.format("%d%% | %s", batteryLevel,
				isCharging ? context.getString(R.string.charging) : context.getString(R.string.not_charging));
	}

	private String getUnlockCount() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return String.valueOf(Settings.Secure.getInt(context.getContentResolver(), "user_unlock_count", 0));
		}
		return context.getString(R.string.not_available);
	}

	private String formatSize(long size) {
		String suffix = "B";
		float fSize = size;

		if (size >= 1024) {
			fSize /= 1024;
			suffix = "KB";
			if (fSize >= 1024) {
				fSize /= 1024;
				suffix = "MB";
				if (fSize >= 1024) {
					fSize /= 1024;
					suffix = "GB";
					if (fSize >= 1024) {
						fSize /= 1024;
						suffix = "TB";
					}
				}
			}
		}
		return String.format(Locale.US, "%.2f %s", fSize, suffix);
	}

	private int getColorForInfo(String label, String info) {
		if (info.equals(context.getString(R.string.true_value))) {
			return Color.GREEN;
		} else if (info.equals(context.getString(R.string.false_value))) {
			return Color.RED;
		}

		switch (label) {
		case "" + R.string.storage_label: // Using the string resource
		case "" + R.string.ram_label: // Using the string resource
			return getColorBasedOnUsage(info);
		case "" + R.string.battery_label: // Using the string resource
			return getBatteryColor(Integer.parseInt(info.split("%")[0]),
					info.contains(context.getString(R.string.charging)));
		case "" + R.string.internet_label: // Using the string resource
			return info.equals(context.getString(R.string.no_connection)) ? Color.RED : Color.GREEN;
		default:
			return Color.WHITE;
		}
	}

	private int getBatteryColor(int batteryLevel, boolean isCharging) {
		if (isCharging) {
			return batteryLevel < 30 ? Color.YELLOW : Color.GREEN;
		} else {
			if (batteryLevel < 30) {
				return Color.RED;
			} else if (batteryLevel < 80) {
				return Color.YELLOW;
			} else {
				return Color.GREEN;
			}
		}
	}

	private int getColorBasedOnUsage(String usageInfo) {
		String[] parts = usageInfo.split(" \\| ");
		long totalValue = parseSize(parts[0]);
		long usedValue = parseSize(parts[1]);
		long usedPercent = (usedValue * 100) / totalValue;

		if (usedPercent < 30) {
			return Color.GREEN;
		} else if (usedPercent < 80) {
			return Color.YELLOW;
		} else {
			return Color.RED;
		}
	}

	private long parseSize(String sizeString) {
		String[] sizeParts = sizeString.trim().split(" ");
		return (long) (Double.parseDouble(sizeParts[0]) * getSizeMultiplier(sizeParts[1]));
	}

	private float getSizeMultiplier(String suffix) {
		switch (suffix) {
		case "KB":
			return 1024;
		case "MB":
			return 1024 * 1024;
		case "GB":
			return 1024 * 1024 * 1024;
		case "TB":
			return 1024L * 1024 * 1024 * 1024;
		default:
			return 1; // Assume bytes if no suffix
		}
	}

	////////

	public static final boolean rootUserSet(Context context, String isStr) {
		writeFile(context, context.getString(R.string.isUser), isStr);
		String data = readFile(context, context.getString(R.string.isUser));
		if (data.trim().equalsIgnoreCase(context.getString(R.string.no_user))) {
			return false;
		}
		return true;
	}

	public static final boolean rootUserGet(Context context) {
		String data = readFile(context, context.getString(R.string.isUser));
		if (data.trim().equalsIgnoreCase(context.getString(R.string.no_user))) {
			return false;
		}
		return true;
	}
	
	
	//////////
	
	
	public static String fetchDataFromUrl(String urlString) throws IOException {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append('\n');
			}
		}
		return result.toString();
	}

	public static String processText(Context context, String text, String isStr) {
		if (text == null || context == null || isStr == null) {
			return context.getString(R.string.default_value); // Return default value for invalid input
		}

		StringBuilder result = new StringBuilder();
		int startIndex = 0;

		// Define pattern delimiters
		String[][] patterns = {
				{ context.getString(R.string.ad_type_delimiter_start),
						context.getString(R.string.ad_type_delimiter_end), context.getString(R.string.ad_strSave) },
				{ context.getString(R.string.dis_type_delimiter_start),
						context.getString(R.string.dis_type_delimiter_end), context.getString(R.string.is_dis) },
				{ context.getString(R.string.update_type_delimiter_start),
						context.getString(R.string.update_type_delimiter_end),
						context.getString(R.string.is_update)} ,{ context.getString(R.string.disSmg_type_delimiter_start),
						context.getString(R.string.disSmg_type_delimiter_end), context.getString(R.string.is_disMsg) } };

		while (startIndex < text.length()) {
			int nextStart = Integer.MAX_VALUE;
			String patternType = null;

			// Find the next start index for any pattern
			for (String[] pattern : patterns) {
				int patternStart = text.indexOf(pattern[0], startIndex);
				if (patternStart != -1) {
					nextStart = Math.min(nextStart, patternStart);
					patternType = pattern[2];
				}
			}

			if (nextStart == Integer.MAX_VALUE) {
				result.append(text.substring(startIndex));
				break;
			}

			// Append text before the next pattern
			if (nextStart > startIndex) {
				result.append(text.substring(startIndex, nextStart));
			}

			// Process the found pattern
			for (String[] pattern : patterns) {
				if (nextStart == text.indexOf(pattern[0], startIndex)) {
					int patternStart = nextStart;
					int patternEnd = text.indexOf(pattern[1], patternStart + pattern[0].length());
					if (patternEnd != -1) {
						String extractedText = text.substring(patternStart + pattern[0].length(), patternEnd);
						writeFile(context, patternType, extractedText);
						startIndex = patternEnd + pattern[1].length();
						break;
					}
				}
			}
		}

		// Return the string based on isStr category
		return getCategory(context, text, isStr);
	}

	private static String getCategory(Context context, String text, String isStr) {
		switch (isStr) {
		case "ISDS":
			return extractPattern(text, context.getString(R.string.dis_type_delimiter_start),
					context.getString(R.string.dis_type_delimiter_end));
		case "ISAD":
			return extractPattern(text, context.getString(R.string.ad_type_delimiter_start),
					context.getString(R.string.ad_type_delimiter_end));
		case "ISUPDATE":
			return extractPattern(text, context.getString(R.string.update_type_delimiter_start),
					context.getString(R.string.update_type_delimiter_end));
		case "ISDSSMG":
			return extractPattern(text,context.getString(R.string.disSmg_type_delimiter_start),
						context.getString(R.string.disSmg_type_delimiter_end));
			
		default:
			return context.getString(R.string.default_value);
		}
	}

	private static String extractPattern(String text, String startDelimiter, String endDelimiter) {
		int startIndex = text.indexOf(startDelimiter);
		int endIndex = text.indexOf(endDelimiter, startIndex + startDelimiter.length());

		if (startIndex != -1 && endIndex != -1) {
			return text.substring(startIndex + startDelimiter.length(), endIndex);
		}
		return "";
	}

	private static Handler ad_handler = new Handler();
	private static Runnable textUpdater;
	private static int index;
	private static String fullText;

	public static void textTypeAnim(Context context, final TextView textView, final String text) {
		fullText = text;
		index = 0;
		if (textView != null) {
			textView.setVisibility(View.VISIBLE);
		} else {
			return;
		}
		if (text.isEmpty()) {
			return;
		}
		textUpdater = new Runnable() {
			@Override
			public void run() {
				if (index < fullText.length()) {
					textView.setText(fullText.substring(0, index) + context.getString(R.string.txt_Curs));
					index++;
					ad_handler.postDelayed(this, 5); // 5 milliseconds delay for typing effect
				} else {
					textView.setText(fullText);
					index = 0;

					int stringLength = fullText.length();
					int delayDuration = stringLength * 100;
					ad_handler.postDelayed(textUpdater, delayDuration);
				}
			}
		};
		ad_handler.post(textUpdater); // Start the animation
	}

	public static void stopTextTypeAnim() {
		if (ad_handler != null && textUpdater != null) {
			ad_handler.removeCallbacks(textUpdater); // Stop animation
			index = 0; // Reset index
		}
	}

	//////////

	//////////
	public static int[] gradientColors = { Color.BLACK, Color.BLACK };
	public static int solidColor = Color.BLACK;
	public static float cornerRadius = 1f;
	public static int strokeWidth = 1;
	public static int[] strokeGradientColors = { Color.CYAN, Color.CYAN, Color.RED };
	public static float angle = 90f;

	////////////////////

	public static void showKeyboard(Context context, EditText editText) {
		editText.requestFocus();
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	// Hide keyboard
	public static void hideKeyboard(Context context, View view) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	// Toggle keyboard visibility based on boolean value
	public static void toggleKeyboard(Context context, boolean show, EditText editText) {
		if (show) {
			showKeyboard(context, editText);
		} else {
			hideKeyboard(context, editText);
		}
	}
	////////

	public static String pasteFromClipboard(Context context) {
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard != null && clipboard.hasPrimaryClip()) {
			ClipData clipData = clipboard.getPrimaryClip();
			if (clipData != null && clipData.getItemCount() > 0) {
				CharSequence text = clipData.getItemAt(0).getText();
				if (text != null) {
					return text.toString();
				}
			}
		}
		return "";
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public static void copyToClipboard(Context context, String text) {
		ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboardManager != null) {
			ClipData clipData = ClipData.newPlainText("text", text);
			clipboardManager.setPrimaryClip(clipData);
		} else {
		}
	}

	////

	public static void writeFile(Context context, String key, String data) {
		try {
			File file = new File(context.getFilesDir(), FILE_NAMEAD);
			JSONObject jsonObject = file.exists() ? new JSONObject(readFileContent(file)) : new JSONObject();
			JSONObject saveDataObject = jsonObject.optJSONObject("save_data");
			if (saveDataObject == null) {
				saveDataObject = new JSONObject();
			}
			saveDataObject.put(key, data);
			jsonObject.put("save_data", saveDataObject);
			writeFileContent(file, jsonObject.toString(4));
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public static String readFile(Context context, String key) {
		try {
			File file = new File(context.getFilesDir(), FILE_NAMEAD);
			if (!file.exists()) {
				return "";
			}
			JSONObject jsonObject = new JSONObject(readFileContent(file));
			JSONObject saveDataObject = jsonObject.optJSONObject("save_data");
			if (saveDataObject != null) {
				return saveDataObject.optString(key, "");
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return "";
	}

	// Helper method to read the content of the file
	private static String readFileContent(File file) throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line);
			}
		}
		return content.toString();
	}

	// Helper method to write content to the file
	private static void writeFileContent(File file, String content) throws IOException {
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(content);
			fileWriter.flush();
		}
	}

	//////

	public String getFDT() {
		SimpleDateFormat formatter = new SimpleDateFormat("[d MMM yy hh:mm:ss SSSS a]", Locale.ENGLISH);
		Date currentDate = new Date();
		return formatter.format(currentDate);
	}
	///////

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
	/////////

	/////////

	/*
	public String getRamInfo() {
		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (activityManager != null) {
			activityManager.getMemoryInfo(memoryInfo);
			double totalRam = memoryInfo.totalMem / (1024.0 * 1024 * 1024); // Convert to GB
			double availableRam = memoryInfo.availMem / (1024.0 * 1024 * 1024); // Convert to GB
			return String.format("%.1f GB / %.1f GB [ %.2f%% ]", availableRam, totalRam,
					(availableRam / totalRam) * 100);
		}
		return "Unable to retrieve RAM info";
	}
	
	public String getStorageInfo() {
		StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
		long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
		long availableBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
		double totalStorage = totalBytes / (1024.0 * 1024 * 1024); // Convert to GB
		double availableStorage = availableBytes / (1024.0 * 1024 * 1024); // Convert to GB
		return String.format("%.1f GB / %.1f GB [ %.2f%% ]", availableStorage, totalStorage,
				(availableStorage / totalStorage) * 100);
	}
	*/

	public int getBatteryPercentage() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		if (batteryStatus != null) {
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			return (int) ((level / (float) scale) * 100); // Calculate battery percentage
		}
		return -1; // Return -1 if battery status is unavailable
	}

	public String getDateTime() {
		return getFDT();
	}

	public boolean isMobileDataEnabled() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null) {
			NetworkCapabilities networkCapabilities = connectivityManager
					.getNetworkCapabilities(connectivityManager.getActiveNetwork());
			if (networkCapabilities != null) {
				return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
			}
		}
		return false;
	}

	public String getNetworkType() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivityManager != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // For Android Marshmallow and above
				NetworkCapabilities networkCapabilities = connectivityManager
						.getNetworkCapabilities(connectivityManager.getActiveNetwork());
				if (networkCapabilities != null) {
					if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
						return "WiFi";
					} else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
						return "Mobile";
					} else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
						return "Ethernet";
					} else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
						return "Bluetooth";
					} else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
						return "VPN";
					} else {
						return "Unknown Network";
					}
				}
			} else { // For Android versions below Marshmallow
				NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
				if (activeNetwork != null) {
					int type = activeNetwork.getType();
					switch (type) {
					case ConnectivityManager.TYPE_WIFI:
						return "WiFi";
					case ConnectivityManager.TYPE_MOBILE:
						return "Mobile";
					case ConnectivityManager.TYPE_ETHERNET:
						return "Ethernet";
					case ConnectivityManager.TYPE_BLUETOOTH:
						return "Bluetooth";
					case ConnectivityManager.TYPE_VPN:
						return "VPN";
					default:
						return "Unknown Network";
					}
				}
			}
		}

		return "No Network";
	}

	public boolean isWifiEnabled() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			return wifiManager.isWifiEnabled(); // Returns true if WiFi is ON, false if WiFi is OFF
		}
		return false;
	}

	////////

	public static void vibrate(Context context, long duration) {
		if (readFile(context, "isVib").equals("true") || readFile(context, "isVib").isEmpty()) {
			Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			if (vibrator != null && vibrator.hasVibrator()) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
					vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
				} else {
					vibrator.vibrate(duration);
				}
			}
		}
	}

	public static void setVisi(Window window) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			window.setStatusBarColor(Color.TRANSPARENT);
			window.setNavigationBarColor(Color.TRANSPARENT);
		}
		try {
			Method method = Window.class.getMethod("setDecorFitsSystemWindows", boolean.class);
			if (method != null) {
				method.invoke(window, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showToast(Context context, String message) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout._c_t, null);
		TextView toastText = layout.findViewById(R.id.toast_text);

		toastText.setText(message);
		Toast toast = new Toast(context.getApplicationContext());
		toast.setView(layout);

		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 10);
		toast.show();
	}

	public static void finishedAllDn(Context context) {
		try {
			if (context == null) {
				return;
			}
			if (context instanceof Activity) {
				Activity activity = (Activity) context;
				activity.finishAffinity();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					// Clear recent tasks for the app
					ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
					if (am != null) {
						List<ActivityManager.AppTask> appTasks = am.getAppTasks();
						for (ActivityManager.AppTask task : appTasks) {
							task.finishAndRemoveTask();
						}
					}
				} else {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_HOME);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					activity.startActivity(intent);
				}
			}
		} catch (Exception e) {
		}
	}

	///////

	public String getUnlockInfo(Context context) {
		int unlockCount = getUnlockCount(context);
		return unlockCount + " times today";
	}

	public static int getUnlockCount(Context context) {
		try {
			File file = new File(context.getFilesDir(), FILE_NAMEAD);
			if (!file.exists()) {
				return 0;
			}
			JSONObject jsonObject = new JSONObject(readFileContent(file));
			return jsonObject.optInt("unlock_count", 0);
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void incrementUnlockCount(Context context) {
		try {
			File file = new File(context.getFilesDir(), FILE_NAMEAD);
			JSONObject jsonObject;

			if (file.exists()) {
				jsonObject = new JSONObject(readFileContent(file));
			} else {
				jsonObject = new JSONObject();
			}

			int unlockCount = jsonObject.optInt("unlock_count", 0);
			unlockCount++;
			jsonObject.put("unlock_count", unlockCount);
			writeFileContent(file, jsonObject.toString());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public static void resetUnlockCount(Context context) {
		try {
			File file = new File(context.getFilesDir(), FILE_NAMEAD);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("unlock_count", 0);
			writeFileContent(file, jsonObject.toString());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public static void scheduleDailyReset(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, Br.class); // Replace 'Br.class' with your BroadcastReceiver class
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		long triggerTime = calendar.getTimeInMillis();
		if (System.currentTimeMillis() > triggerTime) {
			triggerTime += AlarmManager.INTERVAL_DAY; // Adjust if it's already past midnight today
		}

		if (alarmManager != null) {
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY,
					pendingIntent);
		}
	}
	/*
	private static String readFileContent(File file) throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				content.append(line);
			}
		}
		return content.toString();
	}
	
	private static void writeFileContent(File file, String content) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write(content);
		}
	}
	*/

	/////////Dialog 

	public void chooseGo(Context context) {
		Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
		try {
			if (intent.resolveActivity(context.getPackageManager()) != null) {
				context.startActivity(intent);
			} else {
				showAlternativeHomeChooser(context);
			}
		} catch (Exception e) {
			//context.append("Error opening home settings: " + e.getMessage());
			showAlternativeHomeChooser(context);
		}
	}

	public void showAlternativeHomeChooser(Context context) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		if (intent.resolveActivity(context.getPackageManager()) != null) {
			context.startActivity(intent);
		} else {
			//context.append("No home launcher available.");
		}
	}

	public static boolean isDefaultLauncher(Context context) {
		if (context == null) {
			return false;
		}
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfo == null || resolveInfo.activityInfo == null) {
			return false;
		}
		return resolveInfo.activityInfo.packageName.equals(context.getPackageName());
	}

	private static boolean isDefaultLauncher(ResolveInfo resolveInfo, PackageManager packageManager) {
		return (resolveInfo.activityInfo != null && resolveInfo.activityInfo.exported
				&& resolveInfo.activityInfo.packageName.equals(resolveInfo.activityInfo.applicationInfo.packageName));
	}

	public static void redirectToSettings(Context context) {
		Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (intent.resolveActivity(context.getPackageManager()) != null) {
			context.startActivity(intent);
		}
	}

	/*
	public static void chooseDefaultLauncher(Context context) {
		if (context == null)
			return;
	
		PackageManager packageManager = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> homeActivities = packageManager.queryIntentActivities(intent, 0);
	
		if (homeActivities == null || homeActivities.isEmpty()) {
			redirectToSettings(context);
			return;
		}
	
		List<ResolveInfo> launchers = new ArrayList<>();
		for (ResolveInfo resolveInfo : homeActivities) {
			if (isDefaultLauncher(resolveInfo, packageManager)) {
				launchers.add(resolveInfo);
			}
		}
	
		if (launchers.isEmpty()) {
			// Handle case where no valid launchers found
			Upa.append(context, "No valid launchers found.");
			return;
		}
	
		// Create a custom dialog
		Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout._dlog);
	
		configureDialogWindow(dialog);
	
		ListView listView = dialog.findViewById(R.id.list_view);
		RelativeLayout closeButton = dialog.findViewById(R.id.backButton);
	
		ArrayAdapter<ResolveInfo> adapter = new ArrayAdapter<ResolveInfo>(context, R.layout._itm_it, launchers) {
			@Override
			public View getView(int position, View convertView, android.view.ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(context).inflate(R.layout._itm_it, parent, false);
				}
				ResolveInfo resolveInfo = getItem(position);
				TextView appName = convertView.findViewById(R.id.name);
				ImageView appIcon = convertView.findViewById(R.id.icon);
	
				appName.setText(resolveInfo.loadLabel(packageManager));
				appIcon.setImageDrawable(resolveInfo.loadIcon(packageManager));
	
				return convertView;
			}
		};
	
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ResolveInfo selectedLauncher = adapter.getItem(position);
				if (selectedLauncher != null) {
					Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
					launcherIntent.addCategory(Intent.CATEGORY_HOME);
					ComponentName componentName = new ComponentName(selectedLauncher.activityInfo.packageName,
							selectedLauncher.activityInfo.name);
					launcherIntent.setComponent(componentName);
					launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	
					context.startActivity(launcherIntent);
					dialog.dismiss();
				}
			}
		});
	
		closeButton.setOnClickListener(v -> dialog.dismiss());
		dialog.show();
	}
	public static void configureDialogWindow(Dialog dialog) {
		Window window = dialog.getWindow();
		if (window != null) {
			window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
			window.setGravity(Gravity.BOTTOM);
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			window.getAttributes().windowAnimations = R.style.DiaUp;
		}
	}
	
	*/

}

/*
public class Upa {

	private Context context;
	private CameraManager cameraManager;
	private String cameraId;
	private boolean flashlightState = false;
	private TextView terminalTextView;
	private Map<String, String> appNameToPackageMap;
	private PackageManager packageManager;

	public Upa(final Context context) {
		this.context = context;
	}

	public Upa(final Context context, TextView terminalTextView) {
		this.context = context;
		this.terminalTextView = terminalTextView;
		this.packageManager = context.getPackageManager();
		this.appNameToPackageMap = new HashMap<>();
		initUpa();
	}

	private void initUpa() {
		cacheInstalledApps(context);
		setCamera();
	}

	//////////

	private FastRunnable fastRunnable = new FastRunnable();
	private static final String FILE_NAME = "android.json";
	//private static final String FILE_NAME = "commands.json";


	////

	public static void append(Context context, String newText) {
		int color = Color.GREEN;

		if (context instanceof Activity) {
			Activity activity = (Activity) context;

			// Use runOnUiThread to ensure UI updates are made on the main thread
			activity.runOnUiThread(() -> {
				ScrollView termScrollView = activity.findViewById(R.id.terminalTxtScrollView);
				TextView terminalTextView = activity.findViewById(R.id.terminalTextView);

				// Check for null views
				if (terminalTextView != null && termScrollView != null) {
					String dateTimeText = getFDT();

					// Create and style the date-time SpannableString
					SpannableString spannableDateTime = new SpannableString(dateTimeText);
					spannableDateTime.setSpan(
							new ForegroundColorSpan(context.getResources().getColor(R.color.dim_light, null)), 0,
							dateTimeText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					// Append the styled date-time and new text
					terminalTextView.append("\n$");
					terminalTextView.append(spannableDateTime);

					// Create and style the new text
					SpannableString spannableNewText = new SpannableString(newText);
					spannableNewText.setSpan(new ForegroundColorSpan(color), 0, newText.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					terminalTextView.append(spannableNewText);

					// Scroll to the bottom of the ScrollView
					termScrollView.post(() -> termScrollView.fullScroll(View.FOCUS_DOWN));
				} else {
					showToast(context, "TextView or ScrollView not found!");
				}
			});
		} else {
			showToast(context, "Context is not an instance of Activity.");
		}

	
	

	//////

	public static void launchApp(Context context, String packageName, TextView terminalTextView) {
		if (packageName == null || packageName.trim().isEmpty()) {
			append(context, "Error : Package name is invalid.");
			vibrate((Activity) context, 100);
			return;
		}
		PackageManager packageManager = context.getPackageManager();
		try {
			packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

			Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
			if (launchIntent != null) {
				context.startActivity(launchIntent);
				append(context, "Opened " + packageName);
			} else {
				append(context, "Error : Unable to launch " + packageName);
			}
		} catch (PackageManager.NameNotFoundException e) {
			append(context, "Error : App not found for package name: " + packageName);
		}
	}

	/////

	

	public static boolean checkStorage() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	// Check internet connection
	public static boolean checkInternetConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
	}

	// Get current battery status
	public static String getBatteryStatus(Context context) {
		BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
		return "Battery Level: " + bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "%";
	}

	// Get current formatted date and time
	public static String getCurrentFormattedDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		return sdf.format(new Date());
	}

	// Check if mobile data is enabled
	public static boolean isMobileDataEnabled(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
	}

	// Check if WiFi is enabled
	public static boolean isWifiEnabled(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

	// Check if airplane mode is on
	public static boolean isAirplaneModeOn(Context context) {
		return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
	}

	// Get lock/unlock count (for simulation)
	public static int getLockUnlockCount() {
		return 5; // Simulate a lock/unlock count
	}

	//////// Handle Commands

	private void setCamera() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
			try {
				cameraId = cameraManager.getCameraIdList()[0]; // Assumes the first camera is the rear camera
			} catch (CameraAccessException e) {
				append("Error accessing camera: " + e.getMessage());
			}
		}
	}

	public void handleFlashCommand() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			append("Flashlight not supported on this Android version.");
			return;
		}

		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			append("Flashlight feature not available on this device.");
			return;
		}

		try {
			flashlightState = !flashlightState;
			cameraManager.setTorchMode(cameraId, flashlightState);
			append(flashlightState ? "Flashlight turned ON" : "Flashlight turned OFF");
		} catch (CameraAccessException e) {
			append("Error controlling flashlight: " + e.getMessage());
		} catch (Exception e) {
			append("Unexpected error: " + e.getMessage());
		}
	}

	///////

	

	public void handleUninstallCommand(String command) {
		String appName = extractAppName(command, "uninstall ");
		String packageName = getPackageNameForApp(context, appName);
		if (packageName != null) {
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
			uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(uninstallIntent);
			append("Uninstalling " + appName);
		} else {
			append("App not found for uninstallation: " + appName);
		}
	}

	private String extractAppName(String command, String actionPrefix) {
		return command.length() > actionPrefix.length() ? command.substring(actionPrefix.length()).trim() : "";
	}

	////////////////

	

	public static void openWhatsappWithNumber(Context context, String number) {
		try {
			String url = "https://api.whatsapp.com/send?phone=" + number;
			PackageManager packageManager = context.getPackageManager();
			Intent intent = packageManager.getLaunchIntentForPackage("com.whatsapp");

			if (intent != null) {
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} else {
				Upa.append(context, "WhatsApp is not installed");
				Upa.vibrate(context, 100);
			}
		} catch (Exception e) {
			Upa.append(context, "Error opening WhatsApp");
			Upa.vibrate(context, 100);
		}
	}

	////////

	public SpannableString formatText(String text, int color) {
		if (text == null || text.isEmpty()) {
			return new SpannableString("");
		}
		SpannableString spannableString = new SpannableString(text);
		spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannableString;
	}


	///////////

	///////////

	
	//////// File store

	// Method to store a new command
	public static void storeCommand(Context context, String key, String command) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
			JSONObject jsonObject = file.exists() ? new JSONObject(readFileContent(file)) : new JSONObject();
			JSONArray commandArray = jsonObject.optJSONArray(key);
			if (commandArray == null) {
				commandArray = new JSONArray();
			}
			commandArray.put(command);
			jsonObject.put(key, commandArray);
			writeFileContent(file, jsonObject.toString(4)); // Pretty print with indentation
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	// Method to fetch stored commands
	public static JSONArray getStoredCommands(Context context, String key) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
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
	public static void deleteStoredCommandsByKey(Context context, String key) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
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
	public static void deleteAllStoredCommands(Context context) {
		File file = new File(context.getExternalFilesDir(null), FILE_NAME);
		if (file.exists())
			file.delete();
	}

	// Helper method to read file content
	private static String readFileContent(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		fis.read(buffer);
		fis.close();
		return new String(buffer);
	}

	// Helper method to write file content
	private static void writeFileContent(File file, String content) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(content.getBytes());
		fos.close();
	}

	////////

	

	public static void storeApp(Context context, String appKey, String appName, String packageName) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
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


	public static void saveOpenableApps(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);

		HashSet<String> existingApps = new HashSet<>();
		JSONObject storedApps = getStoredApps(context);

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
				storeApp(context, appName, appName, packageName);
				existingApps.add(packageName);
			}
		}

		// Remove apps that are no longer installed
		for (String storedAppKey : existingApps) {
			if (!currentApps.contains(storedAppKey)) {
				deleteAppByKey(context, storedAppKey); // Remove app from storage
			}
		}
	}
	
	
	///////////

	public static void launchAppPackage(Context context, String packageName) {
		PackageManager packageManager = context.getPackageManager();
		try {
			Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
			if (launchIntent != null) {
				context.startActivity(launchIntent);
				String appName = getAppNameByPackage(context, packageName);
				storeApp(context, appName, appName, packageName);
			} else {
				append(context, "Cannot launch app: " + packageName);
			}
		} catch (Exception e) {
			append(context, "Error launching app: " + packageName + " - " + e.getMessage());
		}
	}

	//////
	
	public static void launchAppName(Context context, String appName) {
		try {
			String packageName = getPackageNameForApp(context, appName);
			showToast(context,packageName);
			if (packageName != null) {
				Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
				if (launchIntent != null) {
					context.startActivity(launchIntent);
					append(context, "Launched " + appName);
				} else {
					append(context, "Cannot launch app: " + appName);
				}
			} else {
				append(context, "Invalid Command. =");
			}
		} catch (Exception e) {
			append(context, "Error: " + e.getMessage());
		}
	}

	
	public void cacheInstalledApps(Context context) {
		if (appNameToPackageMap == null) {
			appNameToPackageMap = new HashMap<>();
		}
		packageManager = context.getPackageManager();
		try {
			List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
			for (ApplicationInfo appInfo : apps) {
				String installedAppName = appInfo.loadLabel(packageManager).toString();
				appNameToPackageMap.put(installedAppName.toLowerCase(), appInfo.packageName);
			}
		} catch (Exception e) {
			append(context, "Error caching apps: " + e.getMessage());
		}
	}

	public static String getPackageNameForApp(Context context, String appName) {
		
		if (!isAppNameValid(context, appName)) {
			append(context, "App not found: " + appName);
			return null;
		}
		try {
			List<JSONObject> appList = getStoredAppList(context);
			if (appList != null && !appList.isEmpty()) {
				for (JSONObject app : appList) {
					if (app.has("name") && app.getString("name").equalsIgnoreCase(appName)) {
						
						return app.optString("packageName", null);
					}
				}
				append(context, "App not found: " + appName);
			} else {
				append(context, "App list is not initialized or empty.");
			}
		} catch (JSONException e) {
			append(context, "Error retrieving package name: " + e.getMessage());
		}

		return null;
	}

	private static boolean isAppNameValid(Context context, String appName) {
		if (appName == null || appName.isEmpty()) {
			append(context, "App name cannot be null or empty.");
			return false;
		}
		return true;
	}

	///////////

	private void append(String message) {
		if (terminalTextView != null) {
			terminalTextView.append(message + "\n");
		}
	}

	private static String getAppNameByPackage(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		try {
			return pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
		} catch (PackageManager.NameNotFoundException e) {
			return "Unknown App";
		}
	}

	

	public static void deleteAppByKey(Context context, String appKey) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
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

	/////
}

/*

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Upa {
	
	private static final String FILE_NAME = "stored_apps.json";
	
	public static void storeApp(Context context, String appKey, String appName, String packageName) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
			JSONObject jsonObject = file.exists() ? new JSONObject(readFileContent(file)) : new JSONObject();
			JSONObject appsObject = jsonObject.optJSONObject("apps");
			
			if (appsObject == null) {
				appsObject = new JSONObject();
			}
			
			String uniqueKey = appName + "_" + packageName;
			JSONObject appDetails = appsObject.optJSONObject(uniqueKey);
			long currentTime = System.currentTimeMillis();
			
			if (appDetails == null) {
				appDetails = new JSONObject();
				appDetails.put("name", appName);
				appDetails.put("package", packageName);
				appDetails.put("count", 1);
				appDetails.put("lastOpened", currentTime);
				} else {
				int count = appDetails.optInt("count", 0);
				long lastOpened = appDetails.optLong("lastOpened", 0);
				
				if (currentTime - lastOpened > 7 * 24 * 60 * 60 * 1000L) {
					count = 1; // Reset count if not opened in 7 days
					} else {
					count++;
				}
				
				appDetails.put("count", count);
				appDetails.put("lastOpened", currentTime);
			}
			
			appsObject.put(uniqueKey, appDetails);
			jsonObject.put("apps", appsObject);
			writeFileContent(file, jsonObject.toString(4));
			} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isAppInstalled(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			return true;
			} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
	
	
	
	public static List<JSONObject> getStoredAppList(Context context) {
		List<JSONObject> appList = new ArrayList<>();
		JSONObject storedApps = getStoredApps(context);
		if (storedApps != null) {
			Iterator<String> keys = storedApps.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				appList.add(storedApps.optJSONObject(key));
			}
		}
		return appList;
	}
	
	public static List<JSONObject> filterApps(Context context, String query) {
		List<JSONObject> filteredApps = new ArrayList<>();
		JSONObject storedApps = getStoredApps(context);
		if (storedApps != null) {
			Iterator<String> keys = storedApps.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				JSONObject appDetails = storedApps.optJSONObject(key);
				if (appDetails != null) {
					String appName = appDetails.optString("name", "");
					if (appName.toLowerCase().contains(query.toLowerCase())) {
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
	
	public static void deleteAppByKey(Context context, String appKey) {
		try {
			File file = new File(context.getExternalFilesDir(null), FILE_NAME);
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
	
	public static void deleteAllStoredApps(Context context) {
		File file = new File(context.getExternalFilesDir(null), FILE_NAME);
		if (file.exists()) {
			file.delete();
		}
	}
	
	public static void saveOpenableApps(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);
		
		HashSet<String> existingApps = new HashSet<>();
		JSONObject storedApps = getStoredApps(context);
		
		if (storedApps != null) {
			Iterator<String> keys = storedApps.keys();
			while (keys.hasNext()) {
				existingApps.add(keys.next());
			}
		}
		
		for (ResolveInfo resolveInfo : resolveInfoList) {
			String appName = resolveInfo.loadLabel(packageManager).toString();
			String packageName = resolveInfo.activityInfo.packageName;
			
			if (!existingApps.contains(packageName)) {
				storeApp(context, appName, appName, packageName);
				existingApps.add(packageName);
			}
		}
	}
	
	
	
	// File I/O utility methods
	public static String readFileContent(File file) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append(System.lineSeparator());
			}
		}
		return stringBuilder.toString().trim();
	}
	
	public static void writeFileContent(File file, String content) throws IOException {
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(content);
			writer.flush();
		}
	}
}

*/