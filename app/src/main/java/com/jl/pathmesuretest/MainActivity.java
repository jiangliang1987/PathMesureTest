package com.jl.pathmesuretest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView v = new TextView(this);
        SearchStatusIndicator indicator  = (SearchStatusIndicator) findViewById(R.id.indicator);
        indicator.setStatus(SearchStatusIndicator.STATUS_PREPARING);
    }
}
