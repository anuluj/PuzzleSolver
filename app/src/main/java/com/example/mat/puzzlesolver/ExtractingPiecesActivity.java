package com.example.mat.puzzlesolver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.*;
import org.opencv.android.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import static org.opencv.core.Core.bitwise_not;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2RGBA;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.minAreaRect;

public class ExtractingPiecesActivity extends AppCompatActivity {

    ImageView iv0, iv1, iv2, iv3, iv4, iv5;
    Bitmap puzzlePhoto, fullPhoto, step0, step1, step2, step3, step4;
    Bitmap croppedFullImageBitmap,puzzleBitmap,croppedFullImageGreyWithKeypointsBitmap,puzzleGreyWithKeypointsBitmap,finalImg;
    int stepCounter = 0;
    Mat croppedFullImageMat;
    List<Mat> listOfPuzzles = new ArrayList<Mat>();
    List<MatOfPoint> bigContours = new ArrayList<MatOfPoint>();
    Context context;
    Button btNext, btPrvious;
    ProgressBar progressBar;
    boolean isDemo;
    private static final String TAG = "ExtractingPiecesAct";
    int puzzleNumber = 0;
    boolean matcherVisualization = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extracting_pieces);

        UriHelper mApp = ((UriHelper) getApplicationContext());

        iv0 = (ImageView) findViewById(R.id.ExtractingPiecesImageView);
        iv1 = (ImageView) findViewById(R.id.ExtractingPiecesImageView1);
        iv2 = (ImageView) findViewById(R.id.ExtractingPiecesImageView2);
        iv3 = (ImageView) findViewById(R.id.ExtractingPiecesImageView3);
        iv4 = (ImageView) findViewById(R.id.ExtractingPiecesImageView4);
        iv5 = (ImageView) findViewById(R.id.ExtractingPiecesImageView5);

        btNext = (Button) findViewById(R.id.btExtractingPiecesNext);
        btPrvious = (Button) findViewById(R.id.btExtractingPiecesPrevoius);
        btPrvious.setEnabled(false);
        context = getApplicationContext();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        isDemo = getIntent().getExtras().getBoolean("isDemo");
        if (isDemo) {
            mApp.setDemoPuzzlesPhoto(BitmapFactory.decodeResource(getResources(), R.drawable.puzzles));
            mApp.setDemoFullPhoto(BitmapFactory.decodeResource(getResources(), R.drawable.photo));
            puzzlePhoto = mApp.getDemoPuzzlesPhoto();
            fullPhoto = mApp.getDemoFullPhoto();
            iv0.setImageBitmap(puzzlePhoto);
        } else {
            try {
                puzzlePhoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mApp.getPuzzlesPhotoUri()));
                if (puzzlePhoto.
                        getWidth() > 3000 || puzzlePhoto.getHeight() > 3000) {
                    puzzlePhoto = Bitmap.createScaledBitmap(puzzlePhoto, puzzlePhoto.getWidth() / 2, puzzlePhoto.getHeight() / 2, false);
                }
                fullPhoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mApp.getFullPhotoUri()));
                if (fullPhoto.getWidth() > 3000 || fullPhoto.getHeight() > 3000) {
                    fullPhoto = Bitmap.createScaledBitmap(fullPhoto, fullPhoto.getWidth() / 2, fullPhoto.getHeight() / 2, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            iv0.setImageBitmap(puzzlePhoto);
        }
        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (stepCounter++) {
                    case 0: {
                        new extractPiecesTask().execute();
                        break;
                    }
                    case 1: {
                        new croppImageTask().execute();
                        break;
                    }
                    case 2: {
                        if(matcherVisualization==false)
                            btPrvious.setEnabled(true);
                        puzzleNumber++;
                        puzzleNumber %= listOfPuzzles.size();
                        new matchPuzzlesWithImageTask().execute();
                        matcherVisualization = false;
                    }
                    default: {
                        stepCounter = 2;
                    }
                }
            }
        });
        btPrvious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                puzzleNumber--;
                puzzleNumber =Math.abs(puzzleNumber);
                puzzleNumber %= listOfPuzzles.size();
                new matchPuzzlesWithImageTask().execute();
            }
        });
    }

    private class matchPuzzlesWithImageTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            matchPuzzlesWithImage(puzzleNumber, matcherVisualization);
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            btNext.setEnabled(false);
            btPrvious.setEnabled(false);
            iv0.setImageResource(0);
            iv1.setImageResource(0);
            iv2.setImageResource(0);
            iv3.setImageResource(0);
            iv4.setImageResource(0);
            iv5.setImageResource(0);
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btNext.setEnabled(true);
            iv0.setImageBitmap(croppedFullImageBitmap);
            iv1.setImageBitmap(puzzleBitmap);
            iv2.setImageBitmap(croppedFullImageGreyWithKeypointsBitmap);
            iv3.setImageBitmap(puzzleGreyWithKeypointsBitmap);
            if (matcherVisualization == false) {
                btPrvious.setEnabled(true);
                iv0.setImageResource(0);
                iv1.setImageResource(0);
                iv2.setImageResource(0);
                iv3.setImageResource(0);
                iv4.setImageBitmap(finalImg);
                iv5.setImageBitmap(puzzleBitmap);
            }
        }
    }
    private class croppImageTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            cropImage();
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btNext.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            iv0.setImageResource(0);
            iv1.setImageResource(0);
            iv2.setImageResource(0);
            iv3.setImageResource(0);
            iv4.setImageResource(0);
            iv5.setImageResource(0);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btNext.setEnabled(true);
            iv0.setImageBitmap(fullPhoto);
            iv1.setImageBitmap(step1);
            iv2.setImageBitmap(step2);
            iv3.setImageBitmap(step3);
            iv4.setImageBitmap(step4);
            iv5.setImageResource(0);
            progressBar.setVisibility(View.GONE);
        }


    }
    private class extractPiecesTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            extractPieces();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btNext.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            iv0.setImageResource(0);
            iv1.setImageResource(0);
            iv2.setImageResource(0);
            iv3.setImageResource(0);
            iv4.setImageResource(0);
            iv5.setImageResource(0);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btNext.setEnabled(true);
            Toast.makeText(context, "Znaleziono " + bigContours.size() + " elementów", Toast.LENGTH_SHORT).show();
            iv0.setImageBitmap(puzzlePhoto);
            iv1.setImageBitmap(step0);
            iv2.setImageBitmap(step1);
            iv3.setImageBitmap(step2);
            iv4.setImageBitmap(step3);
            iv5.setImageBitmap(step4);
            progressBar.setVisibility(View.GONE);
        }
    }
    public void matchPuzzlesWithImage(int numberOfpuzzle, boolean matcherVisualization_local) {
        // -- INIT MATs
        Mat croppedFullImageGreyMat = croppedFullImageMat.clone();
        Mat rgbImage = croppedFullImageMat.clone();
        Mat puzzleMat = listOfPuzzles.get(numberOfpuzzle);
        Mat puzzleGreyMat = new Mat();
        Mat puzzleMaskMat = new Mat();
        Mat descriptor_image = new Mat();
        Mat descriptor_puzzle = new Mat();
        Mat croppedFullImageGreyWithKeypointsMat = new Mat();
        Mat puzzleGreyWithKeypointsMat = new Mat();

        // -- INIT FULL IMAGE ImageView
        croppedFullImageBitmap = Bitmap.createBitmap(croppedFullImageMat.cols(), croppedFullImageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croppedFullImageMat, croppedFullImageBitmap);

        // -- INIT PUZZLE ImageView
        puzzleBitmap = Bitmap.createBitmap(puzzleMat.cols(), puzzleMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(puzzleMat, puzzleBitmap);

        // -- INIT DETECTOR EXTRACTOR MATCHER
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // -- CONVERT IMAGES TO GRAY
        Imgproc.cvtColor(croppedFullImageMat, croppedFullImageGreyMat, COLOR_RGB2GRAY);
        Imgproc.cvtColor(puzzleMat, puzzleGreyMat, COLOR_RGB2GRAY);

        // -- Detect the keypoints using Detector
        MatOfKeyPoint keypoints_image = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_puzzle = new MatOfKeyPoint();

        // -- MASK OF PUZZLE
        Imgproc.adaptiveThreshold(puzzleGreyMat, puzzleMaskMat, 250, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 39, 4);

        detector.detect(croppedFullImageGreyMat, keypoints_image);
        detector.detect(puzzleGreyMat, keypoints_puzzle, puzzleMaskMat);

        // -- Calculate descriptors (feature vectors)
        extractor.compute(croppedFullImageGreyMat, keypoints_image, descriptor_image);
        extractor.compute(puzzleGreyMat, keypoints_puzzle, descriptor_puzzle);

        Features2d.drawKeypoints(croppedFullImageGreyMat, keypoints_image, croppedFullImageGreyWithKeypointsMat);
        Features2d.drawKeypoints(puzzleGreyMat, keypoints_puzzle, puzzleGreyWithKeypointsMat);

        croppedFullImageGreyWithKeypointsBitmap = Bitmap.createBitmap(croppedFullImageGreyWithKeypointsMat.cols(), croppedFullImageGreyWithKeypointsMat.rows(), Bitmap.Config.ARGB_8888);
        puzzleGreyWithKeypointsBitmap = Bitmap.createBitmap(puzzleGreyWithKeypointsMat.cols(), puzzleGreyWithKeypointsMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(croppedFullImageGreyWithKeypointsMat, croppedFullImageGreyWithKeypointsBitmap);
        Utils.matToBitmap(puzzleGreyWithKeypointsMat, puzzleGreyWithKeypointsBitmap);

        if (matcherVisualization_local == false) {
            // -- Matching descriptor vectors using matcher
            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(descriptor_puzzle, descriptor_image, matches);

            List<DMatch> matchesList = matches.toList();
            double max_dist = 0;
            double min_dist = 100;
            // -- Quick calculation of max and min distances between keypoints
            for (int i = 0; i < descriptor_puzzle.rows(); i++) {
                double dist = matchesList.get(i).distance;
                if (dist < min_dist) {
                    min_dist = dist;
                }
                if (dist > max_dist) {
                    max_dist = dist;
                }
            }
            // -- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
            Vector<DMatch> good_matches = new Vector<DMatch>();
            for (int i = 0; i < descriptor_puzzle.rows(); i++) {
                if (matchesList.get(i).distance < 5 * min_dist) {
                    good_matches.add(matchesList.get(i));
                }
            }

            List<Point> objListGoodMatches = new ArrayList<Point>();
            List<Point> sceneListGoodMatches = new ArrayList<Point>();

            List<KeyPoint> keypoints_objectList = keypoints_puzzle.toList();
            List<KeyPoint> keypoints_sceneList = keypoints_image.toList();

            for (int i = 0; i < good_matches.size(); i++) {
                // -- Get the keypoints from the good matches
                objListGoodMatches.add(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
                sceneListGoodMatches.add(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
                Core.circle(rgbImage, new Point(sceneListGoodMatches.get(i).x, sceneListGoodMatches.get(i).y), 3, new Scalar(255, 0, 0, 255));

            }
            String text = "Good Matches Count: " + good_matches.size();
            Core.putText(rgbImage, text, new Point(0, 60), Core.FONT_HERSHEY_COMPLEX_SMALL, 1, new Scalar(0, 0, 255, 255));

            MatOfPoint2f objListGoodMatchesMat = new MatOfPoint2f();
            objListGoodMatchesMat.fromList(objListGoodMatches);
            MatOfPoint2f sceneListGoodMatchesMat = new MatOfPoint2f();
            sceneListGoodMatchesMat.fromList(sceneListGoodMatches);

            Log.d("good matches", " size: " + good_matches.size());
            // findHomography needs 4 corresponding points
            if (good_matches.size() > 3) {

                Mat H = Calib3d.findHomography(objListGoodMatchesMat, sceneListGoodMatchesMat, Calib3d.RANSAC, 5 /* RansacTreshold */);
                Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
                Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

                obj_corners.put(0, 0, new double[]{0, 0});
                obj_corners.put(1, 0, new double[]{puzzleGreyMat.cols(), 0});
                obj_corners.put(2, 0, new double[]{puzzleGreyMat.cols(), puzzleGreyMat.rows()});
                obj_corners.put(3, 0, new double[]{0, puzzleGreyMat.rows()});

                Core.perspectiveTransform(obj_corners, scene_corners, H);

                Core.line(rgbImage, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 2);
                Core.line(rgbImage, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 2);
                Core.line(rgbImage, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 2);
                Core.line(rgbImage, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 2);

            }
            finalImg = Bitmap.createBitmap(rgbImage.cols(), rgbImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgbImage, finalImg);
        }
    }
    public void cropImage() {
        //*******BITMAP INIT
        int width = fullPhoto.getWidth();
        int height = fullPhoto.getHeight();
        step0 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //********MAT INIT*****//
        Mat fullPhotoMat = new Mat();
        Mat fullPhotoMask = fullPhotoMat.clone();
        Mat fullPhotoGreyMat = fullPhotoMat.clone();
        Utils.bitmapToMat(fullPhoto, fullPhotoMat);

        //**************CROP FULL PHOTO***********/*******//
        Imgproc.cvtColor(fullPhotoMat, fullPhotoGreyMat, COLOR_RGB2GRAY);
        Utils.matToBitmap(fullPhotoGreyMat, step1);

        Imgproc.adaptiveThreshold(fullPhotoGreyMat, fullPhotoMask, 250, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 39, 4);
        Utils.matToBitmap(fullPhotoMask, step2);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat(fullPhotoMask.rows(), fullPhotoMask.cols(), CvType.CV_8UC1, new Scalar(0));
        Point point = new Point(0, 0);
        Imgproc.findContours(fullPhotoMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, point);

        List<MatOfPoint> bigContour = new ArrayList<MatOfPoint>();
        double max = 0;
        for (int i = 0; i < contours.size(); i++) // iterate through each contour.
        {
            if (contourArea(contours.get(i), false) > max) {
                max = contourArea(contours.get(i), false);
                bigContour.add(0, contours.get(i));
            }
        }

        Imgproc.drawContours(fullPhotoMask, bigContour, 0, new Scalar(255, 255, 255), 3);
        Utils.matToBitmap(fullPhotoMask, step3);

        Mat tempMat, croppedMat;
        tempMat = fullPhotoMat.clone();
        MatOfPoint2f mMOP2F = new MatOfPoint2f(bigContour.get(0).toArray());
        bigContour.get(0).convertTo(mMOP2F, CvType.CV_32FC2);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Imgproc.approxPolyDP(mMOP2F, approxCurve, 3, true);
        croppedMat = tempMat.submat(boundingRect(bigContour.get(0)));
        step4 = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croppedMat, step4);

        croppedFullImageMat = croppedMat.clone();
    }
    public void extractPieces() {
        //***INIT
        int width = puzzlePhoto.getWidth();
        int height = puzzlePhoto.getHeight();
        step0 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        step4 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Mat matDemoPuzzles, matDemoPuzzles_grey, matDemoPuzzles_mask, tempMat;
        matDemoPuzzles = new Mat();
        matDemoPuzzles_grey = new Mat();
        matDemoPuzzles_mask = new Mat();
        //****GREY SCALE****
        Utils.bitmapToMat(puzzlePhoto, matDemoPuzzles);
        tempMat = matDemoPuzzles.clone();
        Imgproc.cvtColor(matDemoPuzzles, matDemoPuzzles_grey, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(matDemoPuzzles_grey, step0);

        //*****MASK****
        Imgproc.adaptiveThreshold(matDemoPuzzles_grey, matDemoPuzzles_mask, 250, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 39, 4);
        Utils.matToBitmap(matDemoPuzzles_mask, step1);

        //****CONTOURS ON MASK****
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat(matDemoPuzzles_mask.rows(), matDemoPuzzles_mask.cols(), CvType.CV_8UC1, new Scalar(0));
        Point point = new Point(0, 0);
        Imgproc.findContours(matDemoPuzzles_mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, point);

        for (int i = 0; i < contours.size(); i++) // iterate through each contour.
        {
            double a = contourArea(contours.get(i), false);  //  Find the area of contour
            if (a > 10000 && a < ((matDemoPuzzles_mask.cols() * matDemoPuzzles_mask.rows()) / 2))
                bigContours.add(contours.get(i));
        }
        Imgproc.drawContours(matDemoPuzzles_mask, bigContours, -1, new Scalar(255, 255, 255), 3);
        Utils.matToBitmap(matDemoPuzzles_mask, step2);

        //****CONTOURS ON IMAGE****
        Imgproc.drawContours(matDemoPuzzles, bigContours, -1, new Scalar(0, 255, 0), 5);
        Imgproc.cvtColor(matDemoPuzzles, matDemoPuzzles, Imgproc.COLOR_RGB2BGR);
        Utils.matToBitmap(matDemoPuzzles, step3, true);

        //***** CROP PUZZLES************
        for (int i = 0; i < bigContours.size(); i++) {
            MatOfPoint2f mMOP2F = new MatOfPoint2f(bigContours.get(i).toArray());
            bigContours.get(i).convertTo(mMOP2F, CvType.CV_32FC2);
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(mMOP2F, approxCurve, 3, true);
            listOfPuzzles.add(tempMat.submat(boundingRect(bigContours.get(i))));
        }
        if (listOfPuzzles.size() > 0) {
            Mat puzzleTodisplay = listOfPuzzles.get(5);
            step4 = Bitmap.createScaledBitmap(step4, puzzleTodisplay.cols(), puzzleTodisplay.rows(), false);
            Utils.matToBitmap(puzzleTodisplay, step4);
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV Manager Connected");
                    break;
                case LoaderCallbackInterface.INIT_FAILED:
                    Log.i(TAG, "Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Log.i(TAG, "Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Log.i(TAG, "Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Log.i(TAG, "Market Error");
                    break;
                default:
                    Log.i(TAG, "OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    protected void onResume() {
        super.onResume();
        //initialize OpenCV manager
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, this, mLoaderCallback);
    }
}
