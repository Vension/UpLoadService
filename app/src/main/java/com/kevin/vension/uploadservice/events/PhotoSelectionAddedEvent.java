/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kevin.vension.uploadservice.events;

import com.kevin.vension.uploadservice.model.PhotoUpload;

import java.util.ArrayList;
import java.util.List;

public class PhotoSelectionAddedEvent {

    private final List<PhotoUpload> mUploads;

    public PhotoSelectionAddedEvent(List<PhotoUpload> uploads) {
        mUploads = uploads;
    }

    public PhotoSelectionAddedEvent(PhotoUpload upload) {
        mUploads = new ArrayList<PhotoUpload>();
        mUploads.add(upload);
    }

    public List<PhotoUpload> getTargets() {
        return mUploads;
    }

    public PhotoUpload getTarget() {
        if (isSingleChange()) {
            return mUploads.get(0);
        } else {
            throw new IllegalStateException("Can only call this when isSingleChange returns true");
        }
    }

    public boolean isSingleChange() {
        return mUploads.size() == 1;
    }

}
