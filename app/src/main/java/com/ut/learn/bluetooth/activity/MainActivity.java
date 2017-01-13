package com.ut.learn.bluetooth.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.ut.learn.bluetooth.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private Button bt_on,bt_discovered,bt_finddevice,bt_close;
    private Button bt_discover;
    private ListView list;
    private BluetoothAdapter ba;
    private Set<BluetoothDevice> devices;
    private List<String> list_name;
    private ArrayAdapter adapter;
    private boolean isFound;
    private BroadcastReceiver mReceiver;
    private boolean isRegister = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        createAndRegisterBroadcast();
        list.setOnItemClickListener(this);
    }

    /**
     * 创建并注册广播接受者
     */
    private void createAndRegisterBroadcast() {
         mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    list_name.add("发现的设备："+device.getName()+"\n"+device.getAddress());
                    Toast.makeText(MainActivity.this,"Bluetooth搜索到设备："+device.getName(),Toast.LENGTH_LONG).show();
                    bt_discover.setText("搜索附近蓝牙");
                    isFound = false;
                }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    Toast.makeText(MainActivity.this,"Bluetooth搜索结束",Toast.LENGTH_LONG).show();
                    bt_discover.setText("搜索附近蓝牙");
                    isFound = false;
                }
                if (adapter!=null){
                    adapter.notifyDataSetChanged();
                }else {
                    adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,list_name);
                    list.setAdapter(adapter);
                }
            }

        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);
        isRegister = true;
    }

    private void initView() {
        bt_on = (Button) findViewById(R.id.bt_on);
        bt_discovered = (Button) findViewById(R.id.bt_discovered);
        bt_finddevice = (Button) findViewById(R.id.bt_finddevice);
        bt_close = (Button) findViewById(R.id.bt_close);
        bt_discover = (Button) findViewById(R.id.bt_discover);
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
            case R.id.bt_discovered:
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(intent,0);
                break;
            case R.id.bt_finddevice:
               devices =ba.getBondedDevices();//获取已经配对的蓝牙设备
                for (BluetoothDevice device:devices) {
                    list_name.add("已配对设备："+device.getName()+"\n"+device.getAddress());
                    Log.e("finddevice", "name: "+device.getName() );
                }
                if (adapter == null){
                    adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,list_name);
                    list.setAdapter(adapter);
                }else {
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.bt_close:
                ba.disable();
                Toast.makeText(MainActivity.this,"Bluetooth turn off!",Toast.LENGTH_LONG).show();
                break;
            case R.id.bt_discover:
                if(!isFound){
                    ba.startDiscovery();
                    bt_discover.setText("正在搜索附近蓝牙。。。");
                    isFound =true;
                    Toast.makeText(MainActivity.this,"Bluetooth start Discovery!",Toast.LENGTH_LONG).show();
                }else {
                    ba.cancelDiscovery();
                    bt_discover.setText("搜索附近蓝牙");
                    isFound = false;
                    Toast.makeText(MainActivity.this,"Bluetooth stop Discovery!",Toast.LENGTH_LONG).show();
                }

                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ba!=null){
            ba.cancelDiscovery();
        }
        if(isRegister){
            this.unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String str = list_name.get(position);
        if(str!=null ){
            ba.cancelDiscovery();
            Intent intent = new Intent(MainActivity.this,ChatActivity.class);
            intent.putExtra("Device",str.substring(str.length()-17));
            intent.putExtra("DeviceName",str.substring(6,str.length()-17));

            setResult(RESULT_OK,intent);
            finish();
        }
    }
}
