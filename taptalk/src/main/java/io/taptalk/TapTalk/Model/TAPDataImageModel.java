package io.taptalk.TapTalk.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

import io.taptalk.TapTalk.Helper.TAPUtils;

public class TAPDataImageModel implements Parcelable {
    @Nullable @JsonProperty("fileID") private String fileID;
    @Nullable @JsonProperty("mediaType") private String mediaType;
    @Nullable @JsonProperty("size") private Long size;
    @Nullable @JsonProperty("width") private Integer width;
    @Nullable @JsonProperty("height") private Integer height;
    @Nullable @JsonProperty("caption") private String caption;
    @Nullable @JsonProperty("fileUri") private String fileUri;

    public TAPDataImageModel(@Nullable Integer width, @Nullable Integer height, @Nullable String caption, @Nullable String fileUri) {
        this.width = width;
        this.height = height;
        this.caption = caption;
        this.fileUri = fileUri;
    }

    public TAPDataImageModel(@Nullable String fileID, @Nullable String mediaType,
                             @Nullable Long size, @Nullable Integer width,
                             @Nullable Integer height, @Nullable String caption) {
        this.fileID = fileID;
        this.mediaType = mediaType;
        this.size = size;
        this.width = width;
        this.height = height;
        this.caption = caption;
    }

    public TAPDataImageModel(HashMap<String, Object> imageDataMap) {
        this.fileID = (String) imageDataMap.get("fileID");
        this.mediaType = (String) imageDataMap.get("mediaType");
        this.size = (Long) imageDataMap.get("size");
        this.width = (Integer) imageDataMap.get("width");
        this.height = (Integer) imageDataMap.get("height");
        this.caption = (String) imageDataMap.get("caption");
        this.fileUri = (String) imageDataMap.get("fileUri");
    }

    public TAPDataImageModel() {
    }

    public static TAPDataImageModel Builder(String fileID, String mediaType, Long size,
                                            Integer width, Integer height, String caption) {
        return new TAPDataImageModel(fileID, mediaType, size, width, height, caption);
    }

    public HashMap<String, Object> toHashMapWithoutFileUri() {
        HashMap<String, Object> dataMap = TAPUtils.getInstance().toHashMap(this);
        dataMap.remove("fileUri");
        return dataMap;
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> dataMap = TAPUtils.getInstance().toHashMap(this);
        return dataMap;
    }

    @Nullable
    public String getFileID() {
        return fileID;
    }

    public void setFileID(@Nullable String fileID) {
        this.fileID = fileID;
    }

    @Nullable
    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(@Nullable String mediaType) {
        this.mediaType = mediaType;
    }

    @Nullable
    public Long getSize() {
        return size;
    }

    public void setSize(@Nullable Long size) {
        this.size = size;
    }

    @Nullable
    public Integer getWidth() {
        return width;
    }

    public void setWidth(@Nullable Integer width) {
        this.width = width;
    }

    @Nullable
    public Integer getHeight() {
        return height;
    }

    public void setHeight(@Nullable Integer height) {
        this.height = height;
    }

    @Nullable
    public String getCaption() {
        return caption;
    }

    public void setCaption(@Nullable String caption) {
        this.caption = caption;
    }

    @Nullable
    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(@Nullable String fileUri) {
        this.fileUri = fileUri;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileID);
        dest.writeString(this.mediaType);
        dest.writeValue(this.size);
        dest.writeValue(this.width);
        dest.writeValue(this.height);
        dest.writeString(this.caption);
        dest.writeString(this.fileUri);
    }

    protected TAPDataImageModel(Parcel in) {
        this.fileID = in.readString();
        this.mediaType = in.readString();
        this.size = (Long) in.readValue(Long.class.getClassLoader());
        this.width = (Integer) in.readValue(Integer.class.getClassLoader());
        this.height = (Integer) in.readValue(Integer.class.getClassLoader());
        this.caption = in.readString();
        this.fileUri = in.readString();
    }

    public static final Creator<TAPDataImageModel> CREATOR = new Creator<TAPDataImageModel>() {
        @Override
        public TAPDataImageModel createFromParcel(Parcel source) {
            return new TAPDataImageModel(source);
        }

        @Override
        public TAPDataImageModel[] newArray(int size) {
            return new TAPDataImageModel[size];
        }
    };
}
