package com.ut.learn.bluetooth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by admin on 2016/12/29.
 */

public class LaunchActivity extends Activity {
    private Button bt_main,bt_second,bt_third;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        initView();
    }

    private void initView() {
        bt_main = (Button) findViewById(R.id.activity_main);
        bt_second = (Button) findViewById(R.id.activity_second);
        bt_third = (Button) findViewById(R.id.activity_third);
    }

    public void learn(View v){
        switch (v.getId()){
            case R.id.activity_main:
                Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
                startActivity(intent);
            break;
            case R.id.activity_second:
                Toast.makeText(LaunchActivity.this,"暂未开放，尽请期待~~~",Toast.LENGTH_LONG).show();
                break;
            case R.id.activity_third:
                Toast.makeText(LaunchActivity.this,"暂未开放，尽请期待~~~",Toast.LENGTH_LONG).show();
                break;
        }
    }
}
