package com.ut.learn.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by admin on 2017/1/3.
 */

public class ChatActivity extends Activity {
    private TextView tv_titile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();
    }

    private void initView() {
        tv_titile = (TextView) findViewById(R.id.tv_title);

        tv_titile.setText(getIntent().getStringExtra("Device"));
    }
}
