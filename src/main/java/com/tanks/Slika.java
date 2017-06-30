package com.tanks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;


public class Slika extends ActionBarActivity {

    private CascadeClassifier klasifikator;
    Bitmap bmp = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    pokreniOpenCV();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

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
            //Load bitmapa koji je proslijedjen kao extra na intentu
            String slika = getIntent().getStringExtra("image");
            try {
                FileInputStream is2 = this.openFileInput(slika);
                bmp = BitmapFactory.decodeStream(is2);
                is2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Kreiranje Mat slike od bitmapa
            Mat matSlika = new Mat ( bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4, new Scalar(4));
            Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp32, matSlika);

            //Obrada Mat slike - konverzija u Grayscale, odredjivanje minimalnog objekta,
            Mat grayscale = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
            Imgproc.cvtColor(matSlika, grayscale, Imgproc.COLOR_RGB2GRAY,4);

            int minVelicina = (int) (bmp.getHeight() * 0.2);
            MatOfRect tenkovi = new MatOfRect();
            if (klasifikator != null) {
                klasifikator.detectMultiScale(grayscale, tenkovi, 1.1, 2, 2,
                        new Size(minVelicina,minVelicina),
                        new Size());
            }
            //crtanje kvadrata
            Rect[] skupObjekata = tenkovi.toArray();
            for (int i = 0; i <skupObjekata.length; i++)
                Core.rectangle(matSlika, skupObjekata[i].tl(), skupObjekata[i].br(), new Scalar(0, 255, 0, 255), 3);

            //Konverzija iz Mat u bitmpa i prikaz u View-u
            Bitmap bmpPrikaz = Bitmap.createBitmap(matSlika.cols(),  matSlika.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(matSlika, bmpPrikaz);
            ImageView imgView = (ImageView) findViewById(R.id.imgPrikaz);
            imgView.setImageBitmap(bmpPrikaz);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Ne mogu ucitati klasifikator",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slika);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"Nema OpenCV biblioteke ili nije mogla biti ucitana",Toast.LENGTH_SHORT).show();
        }
        else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            Toast.makeText(getApplicationContext(), "OpenCV ucitan",Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slika, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
