package com.chinomars.bccAndroidViewer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.chinomars.bccAndroidViewerCommon.Common;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.UUID;

/**
 * Created by Chino on 3/7/16.
 */
public class ResultController extends Activity {
//    public static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    ScrollView curveDrawer;
    TextView tvTitle, tvLog;
    EditText edtCnt, edtLoss, edtDL, edtN;
    SeekBar sekbN;
    RadioGroup rdiogRangeSetter;
    Button btnMeasure, btnParamSetter, btnSaveData;

    int rangeMode = Common.MEASURE_RANGE_UNKNOWN;
    double valCnt = 0, valLoss = 0, valDl = 0;
    double valN = 0;

    BluetoothAdapter btAdapt = null;
    BluetoothSocket btSocket = null;

    Boolean bConnect = false;
    String strName = null;
    String strAddr = null;
    int workMode = Common.MEASURE_MODE_UNKNOW;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        curveDrawer = (ScrollView) this.findViewById(R.id.curve_drawer);

        edtCnt = (EditText) this.findViewById(R.id.edt_cnt);
        edtDL = (EditText) this.findViewById(R.id.edt_dl);
        edtLoss = (EditText) this.findViewById(R.id.edt_loss);
        edtN = (EditText) this.findViewById(R.id.edt_n);
        edtN.clearFocus();

        sekbN = (SeekBar) this.findViewById(R.id.skb_n);
        sekbN.setOnSeekBarChangeListener(new SeekBarChangeEvent());

        rdiogRangeSetter = (RadioGroup) this.findViewById(R.id.rdiog_rangeSetter);
        rdiogRangeSetter.setOnCheckedChangeListener(new CheckedChangeEvent());

        btnMeasure = (Button) this.findViewById(R.id.btn_measure);
        btnMeasure.setOnClickListener(new ClickEvent());
        btnParamSetter = (Button) this.findViewById(R.id.btn_paramsetter);
        btnParamSetter.setOnClickListener(new ClickEvent());
        btnSaveData = (Button) this.findViewById(R.id.btn_savedata);
        btnSaveData.setOnClickListener(new ClickEvent());

        tvTitle = (TextView) this.findViewById(R.id.result_title);
        tvLog = (TextView) this.findViewById(R.id.tvLog);
        Bundle bund = this.getIntent().getExtras();
        strName = bund.getString("NAME");
        strAddr = bund.getString("MAC");
        workMode = bund.getInt("MODE");
        mSetTitle(workMode);
        tvLog.append(strName + "......\n");

        btAdapt = BluetoothAdapter.getDefaultAdapter();
        if (btAdapt == null) {
            tvLog.append("本机无蓝牙设备，连接失败\n");
            finish();
            return;
        }

        if (btAdapt.getState() != BluetoothAdapter.STATE_ON) {
            tvLog.append("本机蓝牙状态不正常，连接失败\n");
            finish();
            return;
        }

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

//        registerReceiver(connectDevices, intent);

        mHandler.sendEmptyMessageDelayed(Common.MESSAGE_CONNECT, 1000);
    }

//    private BroadcastReceiver connectDevices = new BroadcastReceiver() {
////        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Log.d(Common.TAG, "Receiver:" + action);
//            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
//
//            } else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
//
//            }
//        }
//    };

    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Common.MESSAGE_CONNECT:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InputStream tmpIn;
                            OutputStream tmpOut;
                            try {
                                UUID uuid = UUID.fromString(Common.SPP_UUID);
                                BluetoothDevice btDev = btAdapt.getRemoteDevice(strAddr);
                                btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
                                btSocket.connect();
                                tmpIn = btSocket.getInputStream();
                                tmpOut = btSocket.getOutputStream();
                            } catch(Exception e) {
                                Log.d(Common.TAG, "Error connected to: " + strName + "|" + strAddr);
                                bConnect = false;
                                mmInStream = null;
                                mmOutStream = null;
                                btSocket = null;
                                e.printStackTrace();
                                mHandler.sendEmptyMessage(Common.MESSAGE_CONNECT_LOST);
                                return;
                            }
                            mmInStream = tmpIn;
                            mmOutStream = tmpOut;
                            mHandler.sendEmptyMessage(Common.MESSAGE_CONNECT_SUCCEED);
                        }
                    }).start();
                    break;
                case Common.MESSAGE_CONNECT_SUCCEED:
                    addLog("连接成功");
                    bConnect = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] bufRev = new byte[1024];
                            int nRev = 0;
                            while(bConnect) {
                                try {
                                    Log.e(Common.TAG, "Start Recv" + String.valueOf(mmInStream.available()));
                                    nRev = mmInStream.read(bufRev);
                                    if(nRev < 1){
                                        Log.e(Common.TAG, "Recving Short");
                                        Thread.sleep(1000);
                                        continue;
                                    }
                                    System.arraycopy(bufRev, 0, bRecv, nRecved, nRev);
                                    Log.e(Common.TAG, "Recv:" + String.valueOf(nRecved));
                                    nRecved += nRev;
                                    if(nRecved < nNeed){
                                        Thread.sleep(1000);
                                        continue;
                                    }

                                    mHandler.obtainMessage(Common.MESSAGE_RECV, nNeed, -1, null).sendToTarget();

                                } catch(Exception e){
                                    Log.e(Common.TAG, "Recv thread:" + e.getMessage());
                                    mHandler.sendEmptyMessage(Common.MESSAGE_EXCEPTION_RECV);
                                    break;
                                }
                            }
                            Log.e(Common.TAG, "Exit while");
                        }
                    }).start();
                    break;
                case Common.MESSAGE_EXCEPTION_RECV:
                case Common.MESSAGE_CONNECT_LOST:
                    addLog("连接异常， 请退出本界面后重新连接");
                    try{
                        if(mmInStream != null){
                            mmInStream.close();
                        }
                        if(mmOutStream != null){
                            mmOutStream.close();
                        }
                        if(btSocket != null){
                            btSocket.close();
                        }
                    } catch(IOException e){
                        Log.e(Common.TAG, "Close Error");
                        e.printStackTrace();
                    } finally {
                        mmInStream = null;
                        mmOutStream = null;
                        btSocket = null;
                        bConnect = null;

                        // TODO Button apperance Proc
                    }
                    break;
                case Common.MESSAGE_WRITE:
                    // TODO if need write
                    break;
                case Common.MESSAGE_READ:
                    // TODO if need read
                    break;
                case Common.MESSAGE_RECV:
                    break;
            }
        }

    };

    private void addLog(String log) {
        tvLog.append(log);
        curveDrawer.post(() -> curveDrawer.fullScroll(ScrollView.FOCUS_DOWN));
    }


    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            double result = Common.MIN_N + 0.001 * i;
            if (result > Common.MAX_N || result < Common.MIN_N) {
                Toast.makeText(ResultController.this, "n应该在1.440～1.449之间", 1000).show();
                return;
            }

            setN(result);
            String str = String.format("%.3f", result);
            edtN.setText(str);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    class CheckedChangeEvent implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkId) {
            if (checkId == R.id.rdio_long) {
                setMeasureRange(Common.MEASURE_RANGE_LONG);
            }
            else if(checkId == R.id.rdio_mid) {
                setMeasureRange(Common.MEASURE_RANGE_MID);
            }
            else if(checkId == R.id.rdio_short) {
                setMeasureRange(Common.MEASURE_RANGE_SHORT);
            }
        }

    }

    private void setMeasureRange(int rangeCode) {
        rangeMode = rangeCode;
    }

    private void setN(double setVal) {
        valN = setVal;
    }

    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(v == btnMeasure) {
                // TODO
            }
            else if(v == btnParamSetter) {
                // TODO 用户设置保存数据的命名格式
            }
            else if(v == btnSaveData) {
                // TODO
            }
        }
    }

    private void mSetTitle(int mode) {
        if (mode == Common.MEASURE_MODE_BCC) {
            tvTitle.setText(Common.BCC_MODE_TITLE);
        }
        else if (mode == Common.MEASURE_MODE_GXC) {
            tvTitle.setText(Common.GXC_MODE_TITLE);
        }
    }
}



