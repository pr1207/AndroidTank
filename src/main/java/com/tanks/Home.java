package com.tanks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Home extends ActionBarActivity {

    private static final int SLIKA = 100;
    private static final int VIDEO = 101;
    Button btnSlika;
    Button btnVideo;
    Button btnRealTime;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /*Podesavaju se onclick listeneri za dugmad*/
        btnSlika = (Button) findViewById(R.id.btnSlika);
        btnSlika.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                odaberiFajl(SLIKA);
            }
        });
        btnVideo = (Button) findViewById(R.id.btnVideo);
        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                odaberiFajl(VIDEO);
            }
        });
        btnRealTime = (Button) findViewById(R.id.btnRealTime);
        btnRealTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in1 = new Intent(Home.this,Realtime.class);
                startActivity(in1);
            }
        });

    }

    /*Metoda koja otvara nativni file explorer, na osnovu zadatih parametara
    * proslijedjenih exploreru preko intenta
    */
    private void odaberiFajl(int kod) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if(kod==SLIKA)
            intent.setType("image/*");
        else
            intent.setType("video/*");
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        try {
            startActivityForResult(Intent.createChooser(intent, "Odaberite fajl"),kod);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "Morate instalirati file manager sa Play Store-a.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /*Callback metod koji se poziva nakon sto korisnik odabere fajl
    * Na osnovu requestCode se utvrdjuje da li odabirao sliku ili video
    * */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SLIKA:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    String nazivFajla="fajl";
                    try {
                        //Otvara se input stream sa sadrzajem odabrane slike,
                        //Zatim se ta od tog streama kreira bitmapa koja se u memoriji aplikacije snima preko output streama
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        FileOutputStream stream = this.openFileOutput(nazivFajla, Context.MODE_PRIVATE);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

                        stream.close();
                        bmp.recycle();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                    //Kreira se novi intent u kome se obradjuje slika i prosledjuje se naziv fajla
                    Intent in1 = new Intent(this, Slika.class);
                    in1.putExtra("image", nazivFajla);
                    startActivity(in1);
                }
                break;
            case VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    Intent in1 = new Intent(this, Video.class);
                    in1.putExtra("video", uri.toString());
                   startActivity(in1);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
