package com.example.mat.puzzlesolver;

import android.app.Application;

/**
 * Created by mat on 17.01.2017.
 */

public class UriHelper extends Application {
    static public int FULL_PHOTO =1;
    static public int PUZZLE_PHOTO =2;
    public String getFullPhotoUri() {
        return fullPhotoUri;
    }

    public void setFullPhotoUri(String fullPhotoUri) {
        this.fullPhotoUri = fullPhotoUri;
    }

    public String getPuzzlesPhotoUri() {
        return puzzlesPhotoUri;
    }

    public void setPuzzlesPhotoUri(String puzzlesPhotoUri) {
        this.puzzlesPhotoUri = puzzlesPhotoUri;
    }

    private String fullPhotoUri;
    private String puzzlesPhotoUri;
}
