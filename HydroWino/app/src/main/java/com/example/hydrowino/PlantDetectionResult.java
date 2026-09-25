package com.example.hydrowino;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class PlantDetectionResult implements Parcelable {
    private final int plantNumber;
    private final String className;
    private final float confidence;
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    public PlantDetectionResult(int plantNumber, String className, float confidence, float left, float top, float right, float bottom) {
        this.plantNumber = plantNumber;
        this.className = className;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    protected PlantDetectionResult(Parcel in) {
        plantNumber = in.readInt();
        className = in.readString();
        confidence = in.readFloat();
        left = in.readFloat();
        top = in.readFloat();
        right = in.readFloat();
        bottom = in.readFloat();
    }

    public static final Creator<PlantDetectionResult> CREATOR = new Creator<PlantDetectionResult>() {
        @Override
        public PlantDetectionResult createFromParcel(Parcel in) {
            return new PlantDetectionResult(in);
        }

        @Override
        public PlantDetectionResult[] newArray(int size) {
            return new PlantDetectionResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(plantNumber);
        dest.writeString(className);
        dest.writeFloat(confidence);
        dest.writeFloat(left);
        dest.writeFloat(top);
        dest.writeFloat(right);
        dest.writeFloat(bottom);
    }

    public int getPlantNumber() {
        return plantNumber;
    }

    public String getClassName() {
        return className;
    }

    public float getConfidence() {
        return confidence;
    }

    public float getLeft() {
        return left;
    }

    public float getTop() {
        return top;
    }

    public float getRight() {
        return right;
    }

    public float getBottom() {
        return bottom;
    }
}
