package com.lisn.apt_test1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.lisn.annotation.InjectHelper;
import com.lisn.annotationlib.Route;

@Route("main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InjectHelper.inject("调用生成类的方法");
        TestActivityFastBundle testActivityFastBundle = new TestActivityFastBundle();
//        testActivityFastBundle.launch(this);
        testActivityFastBundle.launchForResult(100,this);
    }
}
