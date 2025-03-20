package the.ghost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Ksk extends Activity {

	private static final int LOADING_TIME = 1;
	private static boolean isLulOpen = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Upa.setVisi(getWindow());
		
		if(!Upa.isNetworkAvailable(this) && Upa.readFile(this,getString(R.string.isUser)).isEmpty()){
			Upa.showToast(this,getString(R.string.no_connection));
			Upa.vibrate(this,500);
			Upa.finishedAllDn(this);
			return;
		}
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(LOADING_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!isLulOpen) {
							isLulOpen = true;
							Intent intent = new Intent(Ksk.this, Lul.class);
							startActivity(intent);
							finish();
						} else {
							Intent intent = new Intent(Ksk.this, Lul.class);
							startActivity(intent);
						}
					}
				});
			}
		});

		executor.shutdown();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isLulOpen = false; // Reset the flag when Ksk is destroyed
	}
}