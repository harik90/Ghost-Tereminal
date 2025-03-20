package the.ghost;

import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

public class Br extends BroadcastReceiver {
	private Lul lul;
	private final Set<String> processedPackages = new HashSet<>();
	
	// Default constructor
	public Br() {
	}
	
	// Constructor with Lul parameter
	public Br(Lul lul) {
		this.lul = lul;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null || intent.getAction() == null) {
			return;
		}
		
		String action = intent.getAction();
		
		// Handling different actions
		switch (action) {
			case Intent.ACTION_BOOT_COMPLETED:
			handleBootCompleted(context);
			break;
			
			case Intent.ACTION_USER_PRESENT:
			handleUserPresent(context);
			break;
			
			case DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED:
			handleAdminAction(context, true);
			break;
			
			case DeviceAdminReceiver.ACTION_DEVICE_ADMIN_DISABLED:
			handleAdminAction(context, false);
			break;
			
			case Intent.ACTION_PACKAGE_ADDED:
			case Intent.ACTION_PACKAGE_REMOVED:
			handlePackageChange(context, intent);
			break;
			
			default:
			break;
		}
	}
	
	// Handles Boot Completed event
	private void handleBootCompleted(Context context) {
		Upa.showToast(context, "Device Boot Completed");
	}
	
	// Handles User Present event
	private void handleUserPresent(Context context) {
		Upa.showToast(context, "User present");
		Upa.incrementUnlockCount(context);
	}
	
	// Handles Device Admin action
	private void handleAdminAction(Context context, boolean isEnabled) {
		String message = isEnabled ? "Device Admin Enabled" : "Device Admin Disabled";
		Upa.showToast(context, message);
	}
	
	// Handles package added or removed
	private void handlePackageChange(Context context, Intent intent) {
		Uri data = intent.getData();
		if (data == null) {
			return;
		}
		
		String packageName = data.getSchemeSpecificPart();
		if (packageName == null) {
			return;
		}
		
		String action = intent.getAction();
		boolean isAdded = Intent.ACTION_PACKAGE_ADDED.equals(action);
		String appName = getAppName(context, packageName);
		
		if (isAdded) {
			// Only add if not already processed
			if (!processedPackages.contains(packageName)) {
				lul.storeApp(packageName, packageName, appName);
				appendChangeMessage(context, packageName, appName, true);
				processedPackages.add(packageName);
			}
			} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
			// Only remove if already processed
			if (processedPackages.contains(packageName)) {
				lul.deleteAppByKey(packageName);
				appendChangeMessage(context, packageName, null, false);
				processedPackages.remove(packageName);
			}
		}
	}
	
	// Appends a change message about the app
	private void appendChangeMessage(Context context, String packageName, String appName, boolean isAdded) {
		String status = isAdded ? "Installed" : "Uninstalled";
		int color = isAdded ? Color.CYAN : Color.RED;
		String message = String.format("%s %s : %s", lul.getFDT(), packageName, status);
		lul.appendByBr(lul.createSpannableText(message, color, 10f));
	}
	
	// Retrieves the app name by its package
	private String getAppName(Context context, String packageName) {
		try {
			PackageManager pm = context.getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
			return pm.getApplicationLabel(appInfo).toString();
			} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}