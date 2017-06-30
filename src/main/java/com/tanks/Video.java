package com.tanks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


/*Ideja je sledeca
* Nakon ucitavanja opencv-a vade se frejmovi iz videa
* Svaki izvadjeni frejm se analizira, provlaci kroz opencv
* Rezltujuci frejm se smanjuje na max 250px po bilo kojoj dimenziji i snima u niz
* Nakon toga se AnimationThread kreira, koji svaki sekund salje novi frejm u View (Animacija)
* */

public class Video extends ActionBarActivity {

    private CascadeClassifier klasifikator;
    Bitmap bmp = null;
    ArrayList<Bitmap> bmpNiz = new ArrayList<Bitmap>();
    AnimationThread at=new AnimationThread();
    int bmpIndex=0;
    int frejm = 0;
    Animacija aw;
    TextView tw;

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

    /*Metoda koja uzima bitmap frejm iz videa i provlaci ga kroz opencv
    * Rezultat skalira na mali bitmap i snima ga u niz bitmapa
    * */
    private void analizaFrejma(Bitmap bmp){
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

        //Konverzija iz Mat u bitmap
        Bitmap bmpPrikaz = Bitmap.createBitmap(matSlika.cols(),  matSlika.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matSlika, bmpPrikaz);
        bmpNiz.add(smanjiBitmap(bmpPrikaz, 250, 250));
        System.gc();
        bmpPrikaz.recycle();
        matSlika.release();
        grayscale.release();
        bmp32.recycle();
    }

    public Bitmap smanjiBitmap(Bitmap bmp, int maxH, int maxW) {
        int maxHeight = maxH;
        int maxWidth = maxW;
        float scale = Math.min(((float) maxHeight / bmp.getWidth()), ((float) maxWidth / bmp.getHeight()));

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        return bmp;
    }

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
            //Export frejmova iz Videa u bitmap niz
            bmpNiz.clear();
            //Vadjenje frejmova iz videa na svakih sekund preko MediaMetadataRetrievera
            MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();
            Uri vidURI=Uri.parse(getIntent().getStringExtra("video"));
            mRetriever.setDataSource(getApplicationContext(),vidURI);
            String time = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long trajanje = Long.parseLong(time);
            Log.e("Trajanje",trajanje+"");
            Bitmap temp;
             for(int i=1000000;i<trajanje*1000;i+=1000000)
            {
                temp=mRetriever.getFrameAtTime(i,MediaMetadataRetriever.OPTION_CLOSEST);
                analizaFrejma(temp);
            }
            bmpIndex = 0;
            at.start();
        } catch (Exception e) {
            Log.e("Exc",e.getMessage());
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    //Klasa animacija koja prosiruje View, koja nakon primljene poruke od animationThreada invalidira prikaz
    //I crta novi proslijedjeni bitmap u view
    class Animacija extends View {
        Bitmap frame = null;

        public Animacija(Context context) {
            super(context);
        }

        public void postFrame(Bitmap frame) {
            Message message = frameHandler.obtainMessage(0, frame);
            frameHandler.sendMessage(message);
        }

        protected final Handler frameHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.obj != null) {
                    frame = (Bitmap) message.obj;
                } else {
                    frame = null;
                }
                frejm++;
                tw.setText(frejm+"");
                invalidate();
            }
        };

        @Override
        protected void onDraw(Canvas canvas) {
            if (frame == null) return;
            canvas.drawARGB(0, 0, 0, 0);
            //Novi bmp je prikazan 4 puta veci nego sto je njegova stvarna dimenzija
            canvas.drawBitmap(frame, null,new android.graphics.Rect(0,0,frame.getWidth()*4,frame.getHeight()*4), null);
        }
    }

    //Klasa koja salje na svaki sekund novi frejm Animacija view-u
    private final class AnimationThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                //Salje sledeci bitmap u view
                aw.postFrame(bmpNiz.get(bmpIndex));
                // Ako stigne do zadnjega krece opet od nule
                bmpIndex++;
                if (bmpIndex >= bmpNiz.size()) {
                    bmpIndex = 0;
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        LinearLayout myLayout = (LinearLayout) findViewById(R.id.linLay);
        aw=new Animacija(getApplicationContext());
        tw= (TextView) findViewById(R.id.txtFrame);
        myLayout.addView(aw);
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
