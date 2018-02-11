package com.kevin.vension.uploadservice.model;

import android.content.Context;
import android.util.Log;

import com.kevin.vension.uploadservice.MyApp;
import com.kevin.vension.uploadservice.events.UploadsModifiedEvent;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * 上传事件队列
 */
public class PhotoUploadController {
	public static PhotoUploadController getFromContext(Context context) {
		return MyApp.getApplication(context).getPhotoUploadController();
	}

	private final Context mContext;
	private final ArrayList<PhotoUpload> mSelectedPhotoList;

	private final ArrayList<PhotoUpload> mUploadingList;

	public PhotoUploadController(Context context) {
		mContext = context;
		mSelectedPhotoList = new ArrayList<PhotoUpload>();
		mUploadingList = new ArrayList<PhotoUpload>();
	}

	// 添加任务方法
	public boolean addUpload(PhotoUpload selection) {
		if (null != selection) {
			synchronized (this) {
				if (!mUploadingList.contains(selection)) {
					selection.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);
					mUploadingList.add(selection);
					mSelectedPhotoList.remove(selection);
					postEvent(new UploadsModifiedEvent());
					return true;
				}
			}
		}

		return false;
	}

	public synchronized int getActiveUploadsCount() {
		int count = 0;
		for (PhotoUpload upload : mUploadingList) {
			if (upload.getUploadState() != PhotoUpload.STATE_UPLOAD_COMPLETED) {
				count++;
			}
		}
		return count;
	}

	/* 获取下一个任务 */
	public synchronized PhotoUpload getNextUpload() {
		for (PhotoUpload selection : mUploadingList) {
			if (selection.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
				return selection;
			}
		}
		return null;
	}

	public synchronized List<PhotoUpload> getUploadingUploads() {
		return new ArrayList<PhotoUpload>(mUploadingList);
	}

	public synchronized int getUploadsCount() {
		return mUploadingList.size();
	}

	public synchronized boolean hasSelections() {
		return !mSelectedPhotoList.isEmpty();
	}

	public synchronized boolean hasUploads() {
		return !mUploadingList.isEmpty();
	}

	public synchronized boolean hasWaitingUploads() {
		for (PhotoUpload upload : mUploadingList) {
			if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean isOnUploadList(PhotoUpload selection) {
		return mUploadingList.contains(selection);
	}

	public synchronized boolean isSelected(PhotoUpload selection) {
		return mSelectedPhotoList.contains(selection);
	}

	public void removeUpload(final PhotoUpload selection) {
		boolean removed = false;
		synchronized (this) {
			removed = mUploadingList.remove(selection);
		}
		Log.e("图片上传","removeUpload==》" + removed);
		if (removed) {
			selection.setUploadState(PhotoUpload.STATE_NONE);
			postEvent(new UploadsModifiedEvent());
		}
	}

	public void reset() {
		synchronized (this) {
			mSelectedPhotoList.clear();
			mUploadingList.clear();
		}

	}

	private void postEvent(Object event) {
		EventBus.getDefault().post(event);
	}

	void populateFromDatabase() {

		final List<PhotoUpload> uploadsFromDb = new ArrayList<>();// =
		// PhotoUploadDatabaseHelper.getUploads(mContext);
		if (null != uploadsFromDb) {
			mUploadingList.addAll(uploadsFromDb);
			// PhotoUpload.populateCache(uploadsFromDb);
		}
	}

}
