package com.firebase.memoWebApp;

import java.util.Date;

public class Memo {
    private String key;
    private String memoText, title;
    private long createDate, updateDate;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMemoText() {
        return memoText;
    }

    public void setMemoText(String memoText) {
        this.memoText = memoText;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public String getTitle(){
        if(memoText != null){
            if(memoText.indexOf("\n")> -1){
                return memoText.substring(0, memoText.indexOf("\n"));
            } else {
                return memoText;
            }
        }
        return title;
    }

}
