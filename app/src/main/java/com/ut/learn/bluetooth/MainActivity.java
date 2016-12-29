package com.ut.learn.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Button bt_on,bt_discover,bt_finddevice,bt_close;
    private ListView list;
    private BluetoothAdapter ba;
    private Set<BluetoothDevice> devices;
    private List<String> list_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        bt_on = (Button) findViewById(R.id.bt_on);
        bt_discover = (Button) findViewById(R.id.bt_discover);
        bt_finddevice = (Button) findViewById(R.id.bt_finddevice);
        bt_close = (Button) findViewById(R.id.bt_close);
        list = (ListView) findViewById(R.id.list);
        list_name = new ArrayList<>();

        ba = BluetoothAdapter.getDefaultAdapter();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_on:
                if(ba.isEnabled()){
                    Toast.makeText(MainActivity.this,"Bluetooth already on!",Toast.LENGTH_LONG).show();
                }else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,0);
                    Toast.makeText(MainActivity.this,"Bluetooth turn on!",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.bt_discover:
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(intent,0);
                break;
            case R.id.bt_finddevice:
               devices =ba.getBondedDevices();//获取已经配对的蓝牙设备
                for (BluetoothDevice device:devices) {
                    list_name.add(device.getName());
                    Log.e("finddevice", "name: "+device.getName() );
                }
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,list_name);
                list.setAdapter(adapter);
                break;
            case R.id.bt_close:
                ba.disable();
                Toast.makeText(MainActivity.this,"Bluetooth turn off!",Toast.LENGTH_LONG).show();
                break;

        }
    }
}
