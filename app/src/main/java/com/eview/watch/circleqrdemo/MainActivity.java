package com.eview.watch.circleqrdemo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageView = findViewById(R.id.image);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getDrawable(R.drawable.csdn);
        Bitmap bitmap = QrUtil.createBindQr("https://blog.csdn.net/",bitmapDrawable.getBitmap(),0.2f,300);
        imageView.setImageBitmap(bitmap);
    }
}