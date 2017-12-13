package com.kevin.vension.uploadservice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.kevin.vension.uploadservice.model.PhotoUpload;
import com.kevin.vension.uploadservice.model.PhotoUploadController;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MyApp app = (MyApp) getApplication();
		PhotoUploadController con = app.getPhotoUploadController();
		//
		for (int i = 0; i < 20; i++) {
			PhotoUpload selection = new PhotoUpload();
			selection.setName("图片" + i);
			con.addUpload(selection);
		}//

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean isPaused = prefs.getBoolean("isPaused", false);
		menu.findItem(R.id.action_settings).setTitle(
				isPaused ? "Resume" : "Pause");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			boolean isPaused = prefs.getBoolean("isPaused", false);
			prefs.edit().putBoolean("isPaused", !isPaused).commit();
			if (isPaused) {
				startService(MyApp.createExplicitFromImplicitIntent(this,
						new Intent("INTENT_SERVICE_UPLOAD_ALL")));
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
