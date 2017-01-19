package com.example.mat.puzzlesolver;

import android.app.Application;
import android.graphics.Bitmap;

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
    public Bitmap getDemoFullPhoto() {
        return demoFullPhoto;
    }

    public void setDemoFullPhoto(Bitmap demoFullPhoto) {
        this.demoFullPhoto = demoFullPhoto;
    }

    public Bitmap getDemoPuzzlesPhoto() {
        return demoPuzzlesPhoto;
    }

    public void setDemoPuzzlesPhoto(Bitmap demoPuzzlesPhoto) {
        this.demoPuzzlesPhoto = demoPuzzlesPhoto;
    }
    private String fullPhotoUri;
    private String puzzlesPhotoUri;
    private Bitmap demoFullPhoto;
    private Bitmap demoPuzzlesPhoto;
}
