package com.kevin.vension.uploadservice.model;


import com.kevin.vension.uploadservice.events.UploadStateChangedEvent;
import com.kevin.vension.uploadservice.events.UploadsModifiedEvent;

import de.greenrobot.event.EventBus;

/** 上传任务 */
public class PhotoUpload {

	// 任务状态
	public static final int STATE_UPLOAD_COMPLETED = 5;
	public static final int STATE_UPLOAD_ERROR = 4;
	public static final int STATE_UPLOAD_IN_PROGRESS = 3;
	public static final int STATE_UPLOAD_WAITING = 2;
	public static final int STATE_SELECTED = 1;
	public static final int STATE_NONE = 0;
	private int mState;
	private String name;

	public int getUploadState() {
		return mState;
	}

	public void reset() {
		mState = STATE_NONE;
	}

	public void setUploadState(final int state) {
		if (mState != state) {
			mState = state;

			switch (state) {
			case STATE_UPLOAD_ERROR:
			case STATE_UPLOAD_COMPLETED:
				EventBus.getDefault().post(new UploadsModifiedEvent());
				break;
			case STATE_SELECTED:
			case STATE_UPLOAD_WAITING:
				break;
			}
			notifyUploadStateListener();
		}
	}

	private void notifyUploadStateListener() {
		EventBus.getDefault().post(new UploadStateChangedEvent(this));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
