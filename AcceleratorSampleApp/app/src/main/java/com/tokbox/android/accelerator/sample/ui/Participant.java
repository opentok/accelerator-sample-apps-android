package com.tokbox.android.accelerator.sample.ui;

import android.util.Size;

import com.tokbox.android.otsdkwrapper.utils.StreamStatus;

/**
 * Created by mserrano on 09/03/2017.
 */

public class Participant {

    public enum Type {
        LOCAL,
        REMOTE
    }


    public Type mType;
    public String id = null;
    public StreamStatus mStatus;

    public void setContainer(Size mContainer) {
        this.mContainer = mContainer;
    }

    public Size mContainer;

    public Participant(Type type, StreamStatus status, Size containerSize) {
        this.mType = type;
        this.mStatus = status;
        this.mContainer = containerSize;
    }

    public Participant(Type type, StreamStatus status, Size containerSize, String id) {
        this.mType = type;
        this.mStatus = status;
        this.mContainer = containerSize;
        this.id = id;
    }

    public Size getContainer() {
        return mContainer;
    }

    public StreamStatus getStatus() {
        return mStatus;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return mType;
    }
}
