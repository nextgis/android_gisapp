package com.keenfin.easypicker;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class AttachInfo implements Parcelable, Serializable {

    public final boolean onlineAttach;
    public final String url; // url where image located
    public final String storePath; // url to cache path for  file to store

    public final String filename;
    public final String description;
    public final String attachId; // -1  its offile attach - not on server

    public final String oldAttachString; // for offline attach

    public AttachInfo(  boolean onlineAttach, String url, String storePath, String filename, String description,
                        String oldAttachString, String attachId){
        this.onlineAttach = onlineAttach;
        this.url = url;
        this.storePath = storePath;
        this.filename = filename;
        this.description = description;
        this.oldAttachString = oldAttachString;
        this.attachId = attachId;
    }

    public AttachInfo(  boolean onlineAttach,String oldAttachString, String attachId){
        this.onlineAttach = onlineAttach;
        this.oldAttachString = oldAttachString;
        this.url = null;
        this.storePath = null;
        this.filename = null;
        this.description = null;
        this.attachId = attachId;
    }

    protected AttachInfo(Parcel in) {
        onlineAttach = in.readByte() != 0;
        url = in.readString();
        storePath = in.readString();
        filename = in.readString();
        description = in.readString();
        oldAttachString = in.readString();
        attachId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (onlineAttach ? 1 : 0));
        dest.writeString(url);
        dest.writeString(storePath);
        dest.writeString(filename);
        dest.writeString(description);
        dest.writeString(oldAttachString);
        dest.writeString(attachId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AttachInfo> CREATOR = new Creator<AttachInfo>() {
        @Override
        public AttachInfo createFromParcel(Parcel in) {
            return new AttachInfo(in);
        }

        @Override
        public AttachInfo[] newArray(int size) {
            return new AttachInfo[size];
        }
    };
}
