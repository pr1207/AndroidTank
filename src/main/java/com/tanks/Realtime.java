package com.tanks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class Realtime extends Activity implements CvCameraViewListener {

    private CameraBridgeViewBase openCvKameraView;
    private CascadeClassifier klasifikator;
    private Mat grayscale;
    private int minVelicina;

    private void pokreniOpenCV() {
        try {
            //kopiranje klasifikatora iz resource u cascade/klasifikator.xml
            InputStream is = getResources().openRawResource(R.raw.cascade);
            File klasFolder = getDir("cascade", Context.MODE_PRIVATE);
            File klasFajl = new File(klasFolder, "klasifikator.xml");
            FileOutputStream os = new FileOutputStream(klasFajl);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // Ucitava se klasifikator iz apsolutne putanje
            klasifikator = new CascadeClassifier(klasFajl.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Ne mogu ucitati klasifikator", Toast.LENGTH_SHORT).show();
        }
        openCvKameraView.enableView();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        openCvKameraView = new JavaCameraView(this, -1);
        setContentView(openCvKameraView);
        openCvKameraView.setCvCameraViewListener(this);
    }


    @Override
    //Metod koji kreira mat sliku nakon sto se upali kamera
    public void onCameraViewStarted(int width, int height) {
        grayscale = new Mat(height, width, CvType.CV_8UC4);
        //Objekat treba da je bar 30% visine ekrana
        minVelicina = (int) (height * 0.3);
    }


    @Override
    public void onCameraViewStopped() {
    }


    @Override
    //Kako se mijenja frejm na kameri dolazi do obrade u ovoj metodi
    public Mat onCameraFrame(Mat aInputFrame) {
        Imgproc.cvtColor(aInputFrame, grayscale, Imgproc.COLOR_RGBA2RGB);
        MatOfRect tenkovi = new MatOfRect();
        // Pokretanje detektora i crtanje pravougaonika na nadjenim
        if (klasifikator != null) {
            klasifikator.detectMultiScale(grayscale, tenkovi, 1.1, 2, 2,
                    new Size(minVelicina, minVelicina), new Size());
        }
        Rect[] nizTenkova = tenkovi.toArray();
        for (int i = 0; i <nizTenkova.length; i++)
            Core.rectangle(aInputFrame, nizTenkova[i].tl(), nizTenkova[i].br(), new Scalar(0, 255, 0, 255), 3);

        return aInputFrame;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
           Log.e("GRESKA", "Ne mogu pokrenuti OpenCV");
       }
        else{
            pokreniOpenCV();
         }
    }
}