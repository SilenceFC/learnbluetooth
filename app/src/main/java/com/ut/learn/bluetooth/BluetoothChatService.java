package com.ut.learn.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ut.learn.bluetooth.adapter.BlueChatAdapter;
import com.ut.learn.bluetooth.bean.API;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.security.auth.login.LoginException;

/**
 * Created by admin on 2017/1/3.
 * 1.Server 的Accept
 * 2.Client 的connect
 * 3.双方    的connection
 */

public class BluetoothChatService {
    public static final String TAG = " BluetoothChatService";

    public static final int STATE_NONE = 10;
    public static final int STATE_LISTEN = 11;
    public static final int STATE_CONNECTING = 12;
    public static final int STATE_CONNECTED = 13;

    public static final String NAME_SECURE = "BluetoothChat_Secure";
    public static final String NAME_INSECURE = "BluetoothChat_Insecure";
    public static final UUID UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");  //内容将被加密
    public static final UUID UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");  //不需要验证

    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private int mState;
    private Context mContext;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;


    public BluetoothChatService(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    public synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(API.MESSAGR_STATE_CHANGED, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    /**
     * 开启AcceptThread线程接受其他蓝牙设备的请求
     */
    public synchronized void start() {
        Log.e(TAG, "start");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }

//        if (mInsecureAcceptThread == null) {
//            mInsecureAcceptThread = new AcceptThread(false);
//            mInsecureAcceptThread.start();
//        }
    }

    /**
     * 开启 ConnectThread 线程接受特定蓝牙设备的请求
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.e(TAG, "connect to device:" + device.getName());
        Log.e(TAG, "connect mark: "+getState());
        if (getState() == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            mConnectThread = new ConnectThread(device, secure);
            mConnectThread.start();
            setState(STATE_CONNECTING);
            Log.e(TAG, "connect state: "+getState());

    }

    /**
     * 开启 ConnectedThread 线程用于管理一个蓝牙连接
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, String secure) {
        Log.e(TAG, "connected SocketType:" + secure);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, secure);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(API.MESSAGR_STATE_DEVICE);
        Bundle bundle = new Bundle();
        bundle.putString(API.NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * 关闭所有线程
     */
    public synchronized void stop() {
        Log.e(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * 发送数据
     */
    public void write(byte[] out) {
        ConnectedThread t;
        synchronized (this){
            if(mState !=STATE_CONNECTED) return;
            t = mConnectedThread;
        }
        t.write(out);
    }

    /**
     * 连接上设备之后失去与设备的连接
     */
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(API.MESSAGR_STATE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(API.TOAST, "Device Connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        BluetoothChatService.this.start();
    }

    /**
     * 连接不到设备
     */
    private void connectFailed() {
        Message msg = mHandler.obtainMessage(API.MESSAGR_STATE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(API.TOAST, "unable to connect the device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        BluetoothChatService.this.start();
    }

    /**
     * 监听来自其他蓝牙设备的连接请求
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        private String mSocketType;


        public AcceptThread(boolean secure) {
            mSocketType = secure ? "Secure" : "Insecure";
            BluetoothServerSocket tmp = null;
            try {
                if (secure) {
                    //连接需要认证并且被加密
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_SECURE);
                } else {
                    //不需要认证的连接
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread:" + mSocketType, e);
            }
            mServerSocket = tmp;
        }

        @Override
        public void run() {
            Log.e(TAG, "SocketType:" + mSocketType + " begin mAcceptThread " + this);
            setName("AcceptThread_" + mSocketType);
            BluetoothSocket mSocket = null;

            Log.e(TAG, "STATE_CONNECTED = 13 :"+mState);
            /**
             * 判断是否连接，是则关闭当前的socket（已经连接的socket不会被关闭），并退出循环。
             */
            while (mState != STATE_CONNECTED) {
                try {
                    setState(STATE_CONNECTING);
                    mSocket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "SocketType:" + mSocketType + " accept is failed!", e);
                    break;
                }
                if (mSocket != null) {
                    Log.e(TAG, "Socket连接已经建立");

                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_NONE:
                                break;
                            case STATE_LISTEN:
                                break;
                            case STATE_CONNECTING:
                                // 开启ConnectedThread线程
                                connected(mSocket,mSocket.getRemoteDevice(),mSocketType);
                                break;
                            case STATE_CONNECTED:
                                try {
                                    mSocket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;

                        }
                    }
                }
            }

        }

        public void cancel() {
            try {
                mServerSocket.close();
                Log.e(TAG, "SocketType:" + mSocketType + " close the ServerSocket sucessfully");
            } catch (IOException e) {
                Log.e(TAG, "SocketType:" + mSocketType + " ServerSocket is failed to close", e);
            }
        }
    }

    /**
     * 作为客户端试图与服务端进行连接
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread:" + secure + " is failed to create", e);
            }

            mSocket = tmp;
        }

        @Override
        public void run() {
            Log.e(TAG, "begin:ConnectThread :" + mSocketType);
            setName("ConnectThread_" + mSocketType);

            mAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "ConnectThread无法连接并且无法取消", e1);
                }
                Log.e(TAG, "ConnectThread :" + "IO异常，Socket已经关闭");
                connectFailed();
                return;
            }

            synchronized (BluetoothChatService.this) {
                //将当前ConnectThread置为空，连接完成就不需要它了
                mConnectThread = null;
            }
            connected(mSocket, mDevice, mSocketType);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel the ConnectThread is failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.e(TAG, "ConnectedThread:_" + socketType + "is start");
            mSocket = socket;
            InputStream tmpin = null;
            OutputStream tmpout = null;

            try {
                tmpin = mSocket.getInputStream();
                tmpout = mSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp socket is not create", e);
            }

            mInputStream = tmpin;
            mOutputStream = tmpout;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            //蓝牙连接上之后保持连接
            while (true) {
                try {
                    bytes = mInputStream.read(buffer);
                    mHandler.obtainMessage(API.MESSAGR_STATE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "DisConnect", e);
                    connectionLost();
                    BluetoothChatService.this.start();
                    break;
                }

            }
        }


        public void write(byte[] out) {
            try {
                mOutputStream.write(out);
                mHandler.obtainMessage(API.MESSAGR_STATE_WRITE, -1, -1, out).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel the socket is failed", e);
            }
        }
    }


}
