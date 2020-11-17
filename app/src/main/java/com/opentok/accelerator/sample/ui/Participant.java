package com.opentok.accelerator.sample.ui;

import android.util.Size;
import com.opentok.accelerator.core.utils.StreamStatus;


public class Participant {
    public enum Type {
        LOCAL,
        REMOTE
    }

    public Type mType;
    public String id = null;
    public StreamStatus mStatus;
    private Size mContainer;

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

    public void setContainer(Size mContainer) {
        this.mContainer = mContainer;
    }
}
