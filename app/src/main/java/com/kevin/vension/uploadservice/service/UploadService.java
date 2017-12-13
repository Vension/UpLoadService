package com.kevin.vension.uploadservice.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.kevin.vension.uploadservice.MainActivity;
import com.kevin.vension.uploadservice.MyApp;
import com.kevin.vension.uploadservice.R;
import com.kevin.vension.uploadservice.events.UploadStateChangedEvent;
import com.kevin.vension.uploadservice.events.UploadingPausedStateChangedEvent;
import com.kevin.vension.uploadservice.model.PhotoUpload;
import com.kevin.vension.uploadservice.model.PhotoUploadController;

import org.xutils.common.Callback.CommonCallback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.greenrobot.event.EventBus;

public class UploadService extends Service {
	//
	private boolean mCurrentlyUploading;
	private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private PhotoUploadController mController;
	private int mNumberUploaded = 0;
	private Future<?> mCurrentUploadRunnable;
	private EventBus bus = EventBus.getDefault();
	private android.support.v4.app.NotificationCompat.Builder mNotificationBuilder;
	private NotificationManager mNotificationMgr;

	private class UpdateRunnable extends PhotupThreadRunnable {

		private final PhotoUpload mSelection;

		public UpdateRunnable(PhotoUpload selection) {
			mSelection = selection;
		}

		public void runImpl() {
			try {
				if (mSelection.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
					mSelection
							.setUploadState(PhotoUpload.STATE_UPLOAD_IN_PROGRESS);
					// 暂停1秒 模拟上传
					Thread.sleep(1000);
					// TODO 在这里配置文件服务器地址和参数，需要上传的文件 使用公司文件服务器测试通过
					RequestParams params = new RequestParams(
							"https://www.baidu.com/");
					params.addBodyParameter("f", new File("文件路径"));
					x.http().post(params, new CommonCallback<String>() {

						@Override
						public void onSuccess(String result) {
							mSelection
									.setUploadState(PhotoUpload.STATE_UPLOAD_COMPLETED);
						}

						@Override
						public void onError(Throwable ex, boolean isOnCallback) {
							mSelection
									.setUploadState(PhotoUpload.STATE_UPLOAD_ERROR);
						}

						@Override
						public void onCancelled(CancelledException cex) {
							mSelection
									.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);
						}

						@Override
						public void onFinished() {
							// 通知service
							bus.post(new UploadingPausedStateChangedEvent());
						}

					});
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		bus.register(this);
		mController = ((MyApp) getApplication()).getPhotoUploadController();
		mNotificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		bus.unregister(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null == intent
				|| "INTENT_SERVICE_UPLOAD_ALL".equals(intent.getAction())) {
			if (uploadAll()) {
				return START_STICKY;
			}
		}
		return START_NOT_STICKY;
	}

	// 监听添加任务
	public void onEventMainThread(UploadStateChangedEvent event) {
		PhotoUpload upload = event.getUpload();

		switch (upload.getUploadState()) {
		case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
			updateNotification(upload);
			break;

		case PhotoUpload.STATE_UPLOAD_COMPLETED:
			mNumberUploaded++;

		case PhotoUpload.STATE_UPLOAD_ERROR:
			startNextUploadOrFinish();

		case PhotoUpload.STATE_UPLOAD_WAITING:
			break;
		}
	}

	// 监听任务改变
	void startNextUploadOrFinish() {
		PhotoUpload nextUpload = mController.getNextUpload();
		if (null != nextUpload && canUpload()) {
			startUpload(nextUpload);
		} else {
			mCurrentlyUploading = false;
			stopSelf();
		}
	}

	private boolean canUpload() {
		return !isUploadingPaused(this) && isConnected(this);
	}

	private void startUpload(PhotoUpload upload) {
		mCurrentUploadRunnable = mExecutor.submit(new UpdateRunnable(upload));
		mCurrentlyUploading = true;
	}

	private boolean uploadAll() {
		if (mCurrentlyUploading) {
			return true;
		}

		if (canUpload()) {
			PhotoUpload nextUpload = mController.getNextUpload();
			if (null != nextUpload) {
				startForeground();
				startUpload(nextUpload);
				return true;
			}
		}

		mCurrentlyUploading = false;
		stopSelf();

		return false;
	}

	void stopUploading() {
		if (null != mCurrentUploadRunnable) {
			mCurrentUploadRunnable.cancel(true);
		}
		mCurrentlyUploading = false;
		stopSelf();
	}

	public void onEvent(UploadingPausedStateChangedEvent event) {
		if (isUploadingPaused(this)) {
			stopUploading();
		} else {
			startNextUploadOrFinish();
		}
	}

	private boolean isUploadingPaused(UploadService uploadService) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return prefs.getBoolean("isPaused", false);
	}

	private void startForeground() {
		if (null == mNotificationBuilder) {
			mNotificationBuilder = new android.support.v4.app.NotificationCompat.Builder(
					this);
			mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
			mNotificationBuilder.setContentTitle(getString(R.string.app_name));
			mNotificationBuilder.setOngoing(true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			PendingIntent intent = PendingIntent.getActivity(this, 0,
					new Intent(this, MainActivity.class), 0);
			mNotificationBuilder.setContentIntent(intent);
		}
		startForeground(12, mNotificationBuilder.build());
	}

	void updateNotification(final PhotoUpload upload) {
		String text;

		switch (upload.getUploadState()) {
		case PhotoUpload.STATE_UPLOAD_WAITING:
			text = "上传： " + upload.getName();
			mNotificationBuilder.setContentTitle(text);
			mNotificationBuilder.setTicker(text);
			mNotificationBuilder.setProgress(0, 0, true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());
			break;

		case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
			text = "上传： " + upload.getName();
			mNotificationBuilder.setContentTitle(text);
			mNotificationBuilder.setTicker(text);
			mNotificationBuilder.setProgress(0, 0, true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			break;
		}

		mNotificationMgr.notify(12, mNotificationBuilder.build());
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager mgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo info = mgr.getActiveNetworkInfo();
		return null != info && info.isConnectedOrConnecting();
	}
}
