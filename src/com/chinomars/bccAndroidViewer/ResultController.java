package com.chinomars.bccAndroidViewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.*;

import com.chinomars.bccAndroidViewerCommon.Common;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.UUID;

/**
 * Created by Chino on 3/7/16.
 */
public class ResultController extends Activity {
    public static String FILE_SAVE_PATH = "/BccData";
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    ScrollView curveDrawer;
    TextView tvTitle, tvLog;
    EditText edtCnt, edtLoss, edtDL, edtN;
    SeekBar sekbN;
    RadioGroup rdiogRangeSetter;
    Button btnMeasure, btnParamSetter, btnSaveData, btnReadData;

    BluetoothAdapter btAdapt = null;
    BluetoothSocket btSocket = null;

    int rangeMode = Common.MEASURE_RANGE_UNKNOW;
    int mCnt = 0, mLoss = 0, mDl = 0, mN = 0;
    int[] mCurveData = new int [Common.MAX_CURVE_LEN];
    int mRealCurveLen = 0;

    Boolean bConnect = false;
    String strName = null;
    String strAddr = null;
    int workMode = Common.MEASURE_MODE_UNKNOW;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;
    Boolean canUpdateResult = false;
    String mFileName = null; // set the file name to save

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
        btnMeasure.setEnabled(false);
        btnParamSetter = (Button) this.findViewById(R.id.btn_paramsetter);
        btnParamSetter.setOnClickListener(new ClickEvent());
        btnSaveData = (Button) this.findViewById(R.id.btn_savedata);
        btnSaveData.setOnClickListener(new ClickEvent());
        btnReadData = (Button) this.findViewById(R.id.btn_readdata);
        btnReadData.setOnClickListener(new ClickEvent());

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

        registerReceiver(connectDevices, intent);

        mHandler.sendEmptyMessageDelayed(Common.MESSAGE_CONNECT, 1000);
    }

    @Override
    protected void onDestroy()
    {
        this.unregisterReceiver(connectDevices);
        Log.e(Common.TAG, "Free result");
        super.onDestroy();
    }

    public void closeAndExit(){
        if (bConnect){
            bConnect = false;

            try{
                Thread.sleep(1000);
                if (mmInStream != null) {
                    mmInStream.close();
                }
                if (mmOutStream != null) {
                    mmOutStream.close();
                }
                if (btSocket != null) {
                    btSocket.close();
                }

            } catch (Exception e) {
                Log.e(Common.TAG, "Close error...");
                e.printStackTrace();
            }
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            closeAndExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


    private Boolean isFileNameLegal(String strFileName){
        // TODO judge whether file name is legal
        return true; // for debug
    }

    private void showParamSetDialog() {
        final EditText etTmp = new EditText(this);
        etTmp.setText(null);

        new AlertDialog.Builder(this)
            .setTitle("当前文件名为:" + mFileName)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setView(etTmp)
            .setPositiveButton("确定", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    String strTmp = etTmp.getText().toString();
                    if (strTmp == null) {
                        Toast.makeText(ResultController.this, "文件名不能为空！", 1000).show();
                        return;
                    }
                    if (strTmp.equals("test")) {
                        Toast.makeText(ResultController.this, "文件名不合法，请重新输入", 1000).show();
                        return;
                    }
                    addLog("设置文件名为：" + strTmp);
                    mFileName = strTmp;
                }
            })
            .setNegativeButton("取消", null).show();
    }

    // send message to BlueTooth
    // TODO add BlueTooth setting Command [Priority Low]
    public void send(byte[] sendCommand) {
        if (!bConnect) {
            return;
        }

        try{
            if (mmOutStream == null) {
                return;
            }
//            nNeed = Common.RESULT_DATA_LEN;
            // nNeed = 10; // for debug
            // nRecved = 0;
            mmOutStream.write(sendCommand);
            addLog("发送开始测量指令");

            canUpdateResult = true;

        } catch(Exception e) {
            Toast.makeText(ResultController.this, "发送命令失败", 1000).show();
            return;
        }

    }

    private void mSaveData(){
        // FILE METHOD save to SD card, absolute PATH: SDCARD/BccDATA/
        try{
            Log.e(Common.TAG, "error when saveing data" + mFileName);
            File sdCardDir = Environment.getExternalStorageDirectory();
            File saveDir = new File(sdCardDir.getAbsolutePath() + FILE_SAVE_PATH);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

//            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//            Date curTime = new Date(System.currentTimeMillis());
//            String saveFileName = mFileName + formatter.format(curTime);
            String saveFileName = mFileName;

            File svFile = new File(saveDir, saveFileName);
            if (svFile.exists()){
                Toast.makeText(ResultController.this, "文件已存在，请修改文件名", 1000).show();
                return;
            }

            FileOutputStream fOutS = new FileOutputStream(svFile);
            BufferedWriter oSWriter = new BufferedWriter(new OutputStreamWriter(fOutS));
            oSWriter.write(mCnt);
            oSWriter.write(mLoss);
            oSWriter.write(mDl);

            for (int i = 0; i < mRealCurveLen; ++i){
                oSWriter.write(mCurveData[i]);
            }

            oSWriter.flush();
            oSWriter.close();

            addLog("数据保存到:" + svFile);


        } catch (Exception e) {
            Toast.makeText(ResultController.this, "数据保存错误", 1000).show();
        }

        // TODO SQLite Method




    }

    private void mReadData() {
        // Read test data 'testdata.txt'
        try{
            String fn = "testdata.txt";
            Log.e(Common.TAG, "can't open file" + fn);
            InputStreamReader inSReader = new InputStreamReader(getResources().getAssets().open(fn));
            BufferedReader bufReader = new BufferedReader(inSReader);
            String line = "";
            int dataNum = 0;
            if ((line = bufReader.readLine()) != null){
                int dataTmp = Integer.parseInt(line);
                mCnt = dataTmp;
            }

            if ((line = bufReader.readLine()) != null){
                int dataTmp = Integer.parseInt(line);
                mLoss = dataTmp;
            }

            if ((line = bufReader.readLine()) != null){
                int dataTmp = Integer.parseInt(line);
                mDl = dataTmp;
            }

            while((line = bufReader.readLine()) != null){
                int dataTmp = Integer.parseInt(line);
                if (dataNum + 1 < Common.MAX_CURVE_LEN){
                    mCurveData[dataNum] = dataTmp;
                    dataNum++;
                } else{
                    mRealCurveLen = dataNum;
                    break;
                }

            }

            addLog("读取数据:\nCnt: " + String.valueOf(mCnt) + "\nLoss: " + String.valueOf(mLoss) + "\nDl: " + String.valueOf(mDl));

        } catch (Exception e){
            Toast.makeText(ResultController.this, "无法打开文件", 1000).show();
        }

        // Read from SD card
        try{
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_SAVE_PATH);


        } catch (Exception e){
            e.printStackTrace();
        }


    }

    private void mUpdateDataUI() {
        double datatmp = (double) mCnt / 10000;
        String str = String.format("%.5f",datatmp);
        edtCnt.setText(str);

        datatmp = (double) mLoss / 10000;
        str = String.format("%.5f", datatmp);
        edtLoss.setText(str);

        datatmp = (double) mDl / 10000;
        str = String.format("%.5f", datatmp);
        edtDL.setText(str);

        addLog("数据更新成功");
    }

    private void mDrawCurve() {

    }

    // Button Event Overrider
    class ClickEvent implements View.OnClickListener{
        @Override
        public void onClick(View v){
            if (v == btnMeasure) {
                if (rangeMode == Common.MEASURE_RANGE_UNKNOW){
                    Toast.makeText(ResultController.this, "请先选择测量范围", 1000).show();
                    return;
                }
                if (bConnect) {
                    edtCnt.setText("0.0");
                    edtLoss.setText("0.0");
                    edtDL.setText("0.0");
                    byte[] sendTmp = new byte[8];
                    // TODO add command data
                    send(sendTmp);
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run(){
//                            try{
//                                Log.e(Common.TAG, "Start Send");
//                                send(sendTmp);
//                                addLog("try 发送数据成功 by Chino");
//                            } catch(Exception e){
//                                Toast.makeText(ResultController.this, "Thread 发送数据失败 by Chino", 1000).show();
//                            }
//                        }
//                    }).start();
                }

            } else if (v == btnParamSetter) {
                showParamSetDialog();

            } else if (v == btnSaveData) {
                if (mFileName == null) {
                    showParamSetDialog();
                    return;
                }
                mSaveData();
            } else if (v == btnReadData) {
                mReadData();
                mUpdateDataUI();
                mDrawCurve();
            }
        }
    }

    private BroadcastReceiver connectDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(Common.TAG, "Receiver:" + action);
            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){

            } else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){

            }
        }
    };

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

                    addLog("连接成功了噢 by Chino"); // debug

                    Toast.makeText(ResultController.this, "蓝牙连接成功", 1000).show();
                    bConnect = true;
                    btnMeasure.setEnabled(true);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] bufRev = new byte[1024];
                            int nRecv = 0;
                            while(bConnect) {
                                try {
                                    Log.e(Common.TAG, "Start Recv" + String.valueOf(mmInStream.available()));
                                    nRecv = mmInStream.read(bufRev);
                                    if (nRecv < 1) {
                                        Log.e(Common.TAG, "Recving Short");
                                        Thread.sleep(1000);
                                        continue;
                                    }

                                    byte[] nPacket = new byte[nRecv];
                                    System.arraycopy(bufRev, 0, nPacket, 0, nRecv);
                                    Log.e(Common.TAG, "Recv:" + String.valueOf(nRecved));
                                    nRecved += nRecv;
                                    canUpdateResult = true;
                                    if (nRecved < nNeed) {
                                        Thread.sleep(1000);
                                    }

                                    mHandler.obtainMessage(Common.MESSAGE_RECV, nRecv, -1, nPacket).sendToTarget();

                                } catch (Exception e) {
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
                    btnMeasure.setEnabled(false);

                    addLog("出错了 by Chino"); // debug
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
                        bConnect = false;

                        btnMeasure.setEnabled(false);

                    }
                    break;
                case Common.MESSAGE_WRITE:
                    // TODO if need write

                    break;
                case Common.MESSAGE_READ:
                    // TODO if need read

                    break;
                case Common.MESSAGE_RECV:
                    byte[] bBuf = (byte[]) msg.obj;
                    String strRecv = bytesToString(bBuf, msg.arg1);
                    addLog("接收数据: " + strRecv);

                    break;
                case Common.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), 
                        msg.getData().getString(Common.TOAST), 
                        Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };

    public static String bytesToString(byte[] b, int length) {
        StringBuffer result = new StringBuffer("");
        for (int i = 0; i < length; i++) {
            result.append((char) (b[i]));
        }

        return result.toString();
    }

    public void addLog(String str) {
        tvLog.append(str + "\n");
        curveDrawer.post(new Runnable() {
            public void run() {
                curveDrawer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }


    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            int resTmp = Common.MIN_N + 10*i;
            if (resTmp > Common.MAX_N || resTmp < Common.MIN_N) {
                Toast.makeText(ResultController.this, "n应该在1.440～1.449之间", 1000).show();
                return;
            }

            mN = resTmp;
            double result = (double)resTmp / Common.SCALE;
            addLog("当前折射率为: " + String.valueOf(result));
            String str = String.format("%.5f", result);
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

    private void mSetTitle(int mode) {
        if (mode == Common.MEASURE_MODE_BCC) {
            tvTitle.setText(Common.BCC_MODE_TITLE);
        } else if (mode == Common.MEASURE_MODE_GXC) {
            tvTitle.setText(Common.GXC_MODE_TITLE);
        }
    }

}



