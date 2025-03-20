package the.ghost;

import android.app.Application;
import android.content.Context;

public class Kakak extends Application {
	private static Kakak sApp;

	@Override
	public void onCreate() {
		super.onCreate();
		sApp = this;
		Chandle.init(this);
	}

	public static Kakak getApp() {
		return sApp;
	}

}
