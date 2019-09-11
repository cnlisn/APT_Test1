package com.lisn.apt_test1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.lisn.annotationlib.AutoBundle;

public class TestActivity extends AppCompatActivity {
    @AutoBundle
    public int id;
    @AutoBundle
    public String name;
    @AutoBundle
    public boolean is;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }
}
