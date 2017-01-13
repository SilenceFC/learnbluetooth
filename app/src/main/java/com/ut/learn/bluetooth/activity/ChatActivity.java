package com.ut.learn.bluetooth.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ut.learn.bluetooth.BluetoothChatService;
import com.ut.learn.bluetooth.R;
import com.ut.learn.bluetooth.adapter.BlueChatAdapter;
import com.ut.learn.bluetooth.bean.API;
import com.ut.learn.bluetooth.bean.MsgEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 2017/1/3.
 * 处理蓝牙链接的Activity，主要功能如下：
 * 1.初始化控件。
 * 2.获取BluethoothAdapter,查看其是否可用
 * 3.启用蓝牙，并且创建蓝牙ChatService，用于执行Server的Accept，Client的connect，以及两者之间的Connection
 */

public class ChatActivity extends Activity implements View.OnClickListener {
    private static  final String TAG = "ChatActivity";
    private boolean LOG = true;
    private String deviceAdress;
    private static final int REQUEST_ENABLE = 0;
    private static final int REQUEST_MAIN = 1;

    private TextView tv_titile;
    private Button bt_send;
    private EditText et_chat;
    private ListView chat_list;

    private BluetoothAdapter ba;
    private BlueChatAdapter bcAdapter;
    private List<MsgEntity> chatList = new ArrayList<>();
    private BluetoothChatService mService;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case API.MESSAGR_STATE_CHANGED:
                    switch (msg.arg1){
                        case BluetoothChatService.STATE_NONE:
                            setTitleState("未连接");
                        break;
                        case BluetoothChatService.STATE_LISTEN:
                        break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setTitleState("正在连接中...");
                        break;
                        case BluetoothChatService.STATE_CONNECTED:
                            setTitleState("已连接");
                        break;
                    }
                break;
                case API.MESSAGR_STATE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMsg = new String(readBuf);
                    receiveMsg(readMsg);
                break;
                case API.MESSAGR_STATE_WRITE:
                    byte[] sendBuf = (byte[]) msg.obj;
                    String sendMsg = new String(sendBuf);
                    sendMsg(sendMsg);
                break;
                case API.MESSAGR_STATE_DEVICE:
                    tv_titile.setText(msg.getData().getString(API.NAME));
                break;
                case API.MESSAGR_STATE_TOAST:
                break;
            }
        }
    };

    /**
     * 处理要发送的消息
     * @param sendMsg
     */
    private void sendMsg(String sendMsg) {
        MsgEntity entity = new MsgEntity();
        entity.msg= sendMsg;
        entity.type = 1;
        chatList.add(entity);
        Log.e(TAG, "sendMsg: "+entity);
        bcAdapter.notifyDataSetChanged();

        chat_list.setSelection(chat_list.getCount()-1);
    }

    /**
     * 处理收到的消息
     * @param readMsg
     */
    private void receiveMsg(String readMsg) {
        MsgEntity entity = new MsgEntity();
        entity.msg= readMsg;
        entity.type = 0;
        chatList.add(entity);
        Log.e(TAG, "readMsg: "+entity);
        bcAdapter.notifyDataSetChanged();
        chat_list.setSelection(chat_list.getCount()-1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();


        ba = BluetoothAdapter.getDefaultAdapter();

        if(ba==null){
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!ba.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_ENABLE);
        }else{
            if(mService==null) {
                setChat();

            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mService!=null){
            if(mService.getState() == BluetoothChatService.STATE_NONE){
                mService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService!=null){
            mService.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE:
                if(resultCode == RESULT_OK){
                    setChat();
                }else{
                    Toast.makeText(this, "蓝牙开启失败！", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_MAIN:
                if (resultCode == RESULT_OK){
                    deviceAdress = data.getExtras().getString("Device");
                    tv_titile.setText(data.getExtras().getString("DeviceName"));
                    connectDevice(deviceAdress,true);
                }else{
                    finish();
                }
        }
    }

    private void connectDevice(String deviceAdress, boolean secure) {
        BluetoothDevice device = ba.getRemoteDevice(deviceAdress);
        mService.connect(device,secure);
    }

    private void setChat() {
        bcAdapter = new BlueChatAdapter(ChatActivity.this,chatList);
        chat_list.setAdapter(bcAdapter);

        mService = new BluetoothChatService(ChatActivity.this, mHandler);
    }

    private void initView() {
        tv_titile = (TextView) findViewById(R.id.tv_title);
        et_chat = (EditText) findViewById(R.id.et_chat);
        bt_send = (Button) findViewById(R.id.bt_send);
        chat_list = (ListView) findViewById(R.id.chat_list);

        chatList.clear();

        tv_titile.setText("蓝牙连接");

        bt_send.setOnClickListener(ChatActivity.this);

        tv_titile.setOnClickListener(ChatActivity.this);

        et_chat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s!=null && !s.equals("")){
                    bt_send.setEnabled(true);
                }else{
                    bt_send.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString()!=null && !s.toString().equals("")){
                    bt_send.setEnabled(true);
                }else{
                    bt_send.setEnabled(false);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_send:
                sendMessage();
                break;
            case R.id.tv_title:
                Intent intent =  new Intent(ChatActivity.this,MainActivity.class);
                startActivityForResult(intent,REQUEST_MAIN);
                break;
        }

    }

    private void sendMessage() {
        if(mService.getState() != BluetoothChatService.STATE_CONNECTED){
            Toast.makeText(ChatActivity.this,"尚未连接到相应蓝牙，请稍等。。。",Toast.LENGTH_LONG).show();
            return;
        }
        String content = et_chat.getText().toString().trim();

        if (content.length()>0){
            byte[] send = content.getBytes();
            mService.write(send);
            et_chat.setText("");
        }
    }

    private void setTitleState(String msg){
        tv_titile.append("("+msg+")");
    }

}
