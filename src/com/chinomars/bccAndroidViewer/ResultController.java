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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chinomars.bccAndroidViewerCommon.Common;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

import static android.view.View.VISIBLE;

/**
 * Created by Chino on 3/7/16.
 */
public class ResultController extends Activity {
    public static String FILE_SAVE_PATH = "/BccData/";
    public static String LINE_END = "\r\n";
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    LineChart mCurveDrawer;
    ScrollView svLogger;
    TextView tvTitle, tvLog, tvDl;
    TextView tvOperartor, tvMeasureDate;
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

    Boolean isShowChart = true;
    Boolean bConnect = false;
    String strName = null;
    String strAddr = null;
    int workMode = Common.MEASURE_MODE_UNKNOW;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;
    Boolean canUpdateResult = false;
    String mFileName = null; // set the file name to save
    String mOperator = null, mProdType = null, mProdId = null, mProduceDate = null, mMeasureDate = null, mComment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.result);

        mCurveDrawer = (LineChart) this.findViewById(R.id.curve_drawer);
        mCurveDrawer.setOnLongClickListener(new LongClickEvent());
        svLogger = (ScrollView) this.findViewById(R.id.sv_logger);
//        svLogger.setOnLongClickListener(new LongClickEvent());
        tvLog = (TextView) this.findViewById(R.id.tv_Log);
        tvLog.setOnClickListener(new ClickEvent());

        edtCnt = (EditText) this.findViewById(R.id.edt_cnt);
        edtDL = (EditText) this.findViewById(R.id.edt_dl);
        edtLoss = (EditText) this.findViewById(R.id.edt_loss);
        edtN = (EditText) this.findViewById(R.id.edt_n);
        edtN.addTextChangedListener(textWatcher);

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
        tvDl = (TextView) this.findViewById(R.id.txt_dl);
        tvOperartor = (TextView) this.findViewById(R.id.txt_operator);
        tvMeasureDate = (TextView) this.findViewById(R.id.txt_date);

        Bundle bund = this.getIntent().getExtras();
        strName = bund.getString("NAME");
        strAddr = bund.getString("MAC");
        workMode = bund.getInt("MODE");

        mSetTitle(workMode);
        addLog(strName + "......");

        btAdapt = BluetoothAdapter.getDefaultAdapter();
        if (btAdapt == null) {
            addLog("本机无蓝牙设备，连接失败");
            finish();
            return;
        }

        if (btAdapt.getState() != BluetoothAdapter.STATE_ON) {
            addLog("本机蓝牙状态不正常，连接失败");
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

    private Boolean isOperatorValid(String str){
        // TODO jude the operator name is legal
        return true;
    }

    private Boolean isProductInfoValid(String proNum){
        // TODO low case alpha + num
        return true;
    }

    public void mSetParamSetterInfo(View v){
        ((EditText) v.findViewById(R.id.edt_operator)).setText(mOperator);
        ((EditText) v.findViewById(R.id.edt_product_type)).setText(mProdType);
        ((EditText) v.findViewById(R.id.edt_product_id)).setText(mProdId);
        ((EditText) v.findViewById(R.id.edt_produce_date)).setText(mProduceDate);
        ((EditText) v.findViewById(R.id.edt_measure_date)).setText(mMeasureDate);
        ((EditText) v.findViewById(R.id.edt_comment)).setText(mComment);
    }

    private void showParamSetDialog() {
//        final EditText etTmp = new EditText(this);
//        etTmp.setText(null);
//
//        new AlertDialog.Builder(this)
//            .setTitle("当前文件名为:" + mFileName)
//            .setIcon(android.R.drawable.ic_dialog_info)
//            .setView(etTmp)
//            .setPositiveButton("确定", new DialogInterface.OnClickListener(){
//                public void onClick(DialogInterface dialog, int which){
//                    String strTmp = etTmp.getText().toString();
//                    if (strTmp == null) {
//                        mToastMaker("文件名不能为空！");
//                        return;
//                    }
//                    if (strTmp.equals("test")) {
//                        mToastMaker("文件名不合法，请重新输入");
//                        return;
//                    }
//                    addLog("设置文件名为：" + strTmp);
//                    mFileName = strTmp;
//                }
//            })
//            .setNegativeButton("取消", null).show();

        // new implements on param setter

        LayoutInflater inflater = getLayoutInflater();
        View paramLayout = inflater.inflate(R.layout.parameditor, (ViewGroup) findViewById(R.id.layout_paramsetter));
        if (mOperator != null && mProdType != null && mProdId != null && mMeasureDate != null){
            mSetParamSetterInfo(paramLayout);
        }

        new AlertDialog.Builder(this)
            .setTitle("请输入本次测量信息")
            .setView(paramLayout)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which){
                    // TODO deal with the Measure Info
                    EditText edtTmp = (EditText) paramLayout.findViewById(R.id.edt_operator);
                    String strTmp = edtTmp.getText().toString();
                    if (isOperatorValid(strTmp)){
                        mOperator = strTmp;
                    } else {
                        mToastMaker("操作人信息不合法，请重新设置");
                        return;
                    }

                    edtTmp = (EditText) paramLayout.findViewById(R.id.edt_product_type);
                    strTmp = edtTmp.getText().toString();
                    if (isProductInfoValid(strTmp)){
                        mProdType = strTmp.toLowerCase();
                    } else {
                        mToastMaker("产品型号不合法，请重新输入");
                        return;
                    }

                    edtTmp = (EditText) paramLayout.findViewById(R.id.edt_product_id);
                    strTmp = edtTmp.getText().toString();
                    if (isProductInfoValid(strTmp)){
                        mProdId = strTmp.toLowerCase();
                    } else {
                        mToastMaker("产品编号不合法，请重新输入");
                        return;
                    }

                    mProduceDate = ((EditText) paramLayout.findViewById(R.id.edt_produce_date)).getText().toString();
                    mMeasureDate = ((EditText) paramLayout.findViewById(R.id.edt_measure_date)).getText().toString();
                    mComment = ((EditText) paramLayout.findViewById(R.id.edt_measure_date)).getText().toString();

                    addLog("测量信息为：" + mOperator + mProdType + mProdId + mProduceDate + mMeasureDate + mComment);

                    tvOperartor.setText(Common.OPERATOR + mOperator);
                    tvMeasureDate.setText(Common.MEASURE_TIME + mMeasureDate);

                    if (mFileName == null) {
                        mFileName = mProdType;
                    }


                }
            })
            .setNegativeButton("取消", null)
            .show();
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
            mToastMaker("发送命令失败");
            return;
        }

    }

    private void mSaveData(){
        // FILE METHOD save to SD card, absolute PATH: SDCARD/BccDATA/
        try{
            Log.e(Common.TAG, "error when saveing data" + mFileName);
            File sdCardDir = Environment.getExternalStorageDirectory();
            File saveDir = new File(sdCardDir.getAbsolutePath() + FILE_SAVE_PATH + mProdType + mProdId);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File[] files = saveDir.listFiles();
            String saveFileName = mFileName + String.valueOf(files.length + 1);

            File svFile = new File(saveDir, saveFileName);
            if (svFile.exists()){
                mToastMaker("文件已存在，请修改文件名");
                return;
            }

            FileOutputStream fOutS = new FileOutputStream(svFile);
            BufferedWriter oSWriter = new BufferedWriter(new OutputStreamWriter(fOutS));
            oSWriter.write(mOperator + LINE_END);
            oSWriter.write(mProdType + LINE_END);
            oSWriter.write(mProdId + LINE_END);
            oSWriter.write(mProduceDate + LINE_END);
            oSWriter.write(mMeasureDate + LINE_END);
            oSWriter.write(mComment + LINE_END);

            oSWriter.write(String.valueOf(mCnt) + LINE_END);
            oSWriter.write(String.valueOf(mLoss) + LINE_END);
            oSWriter.write(String.valueOf(mDl) + LINE_END);
            oSWriter.write(String.valueOf(mN) + LINE_END);

            for (int i = 0; i < mRealCurveLen; ++i){
                oSWriter.write(String.valueOf(mCurveData[i]) + LINE_END);
            }

            oSWriter.flush();
            oSWriter.close();

            addLog("数据保存到:" + svFile);


        } catch (Exception e) {
            mToastMaker("数据保存错误");
        }

        // TODO SQLite Method




    }

    private Boolean mReadData() {
        // Read test data 'testdata.txt'
//        try{
//            String fn = "testdata.txt";
//            Log.e(Common.TAG, "can't open file" + fn);
//            InputStreamReader inSReader = new InputStreamReader(getResources().getAssets().open(fn));
//            BufferedReader bufReader = new BufferedReader(inSReader);
//            String line = "";
//            int dataNum = 0;
//            if ((line = bufReader.readLine()) != null) {
//                mOperator = line;
//            }
//            if ((line = bufReader.readLine()) != null) {
//                mProdType = line;
//            }
//            if ((line = bufReader.readLine()) != null) {
//                mProdId = line;
//            }
//            if ((line = bufReader.readLine()) != null) {
//                mProduceDate = line;
//            }
//            if ((line = bufReader.readLine()) != null) {
//                mMeasureDate = line;
//            }
//            if ((line = bufReader.readLine()) != null) {
//                mComment = line;
//            }
//
//            if ((line = bufReader.readLine()) != null){
//                mCnt = Integer.parseInt(line);
//            }
//
//            if ((line = bufReader.readLine()) != null){
//                mLoss = Integer.parseInt(line);
//            }
//
//            if ((line = bufReader.readLine()) != null){
//                mDl = Integer.parseInt(line);
//            }
//
//            if ((line = bufReader.readLine()) != null) {
//                mN = Integer.parseInt(line);
//            }
//
//            while((line = bufReader.readLine()) != null){
//                int dataTmp = Integer.parseInt(line);
//                if (dataNum + 1 < Common.MAX_CURVE_LEN){
//                    mCurveData[dataNum] = dataTmp;
//                    dataNum++;
//                } else {
//                    addLog("到达数据容量");
//                    break;
//                }
//
//            }
//
//            mRealCurveLen = dataNum;
//            addLog("读取" + String.valueOf(mRealCurveLen) + "个数据:\nCnt: " + String.valueOf(mCnt) + "\nLoss: " + String.valueOf(mLoss) + "\nDl: " + String.valueOf(mDl) + "\nN:" + String.valueOf(mN));
//
//        } catch (Exception e){
//            mToastMaker("无法打开文件");
//            return false;
//        }
//        return true;

        // new read data
        if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
            mAlert("本机未插入SD卡，无法读取");
            return false;
        }

        if (mFileName == null){
            mToastMaker("请设置需要读取的产品的信息");
            return false;
        }
        try{
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_SAVE_PATH + mProdType + mProdId);
            if (!dir.exists()){
                mToastMaker("尚未保存任何文件");
                return false;
            }

            File[] files = dir.listFiles();
            
            // TODO 弹出file list
            ArrayList<String> alFileNames = new ArrayList<>();
            for (int i = 0; i < files.length; ++i) {
                alFileNames.add(files[i].getName());
            }
            LayoutInflater inflater = getLayoutInflater();
            View fileListLayout = inflater.inflate(R.layout.filelister, (ViewGroup) findViewById(R.id.layout_filelister));
            ListView lvReadFiles = (ListView) fileListLayout.findViewById(R.id.lv_readfiles);
            ArrayAdapter<String> adtReadFiles = new ArrayAdapter<>(ResultController.this, android.R.layout.simple_list_item_1, alFileNames);
            lvReadFiles.setAdapter(adtReadFiles);
            lvReadFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    File fileToRead = new File(dir, alFileNames.get(i));
                    try{
                        InputStreamReader inSReader = new InputStreamReader(new FileInputStream(fileToRead));
                        BufferedReader bufReader = new BufferedReader(inSReader);
                        String line = "";
                        int dataNum = 0;
                        if ((line = bufReader.readLine()) != null) {
                            mOperator = line;
                        }
                        if ((line = bufReader.readLine()) != null) {
                            mProdType = line;
                        }
                        if ((line = bufReader.readLine()) != null) {
                            mProdId = line;
                        }
                        if ((line = bufReader.readLine()) != null) {
                            mProduceDate = line;
                        }
                        if ((line = bufReader.readLine()) != null) {
                            mMeasureDate = line;
                        }
                        if ((line = bufReader.readLine()) != null) {
                            mComment = line;
                        }

                        if ((line = bufReader.readLine()) != null){
                            mCnt = Integer.parseInt(line);
                        }

                        if ((line = bufReader.readLine()) != null){
                            mLoss = Integer.parseInt(line);
                        }

                        if ((line = bufReader.readLine()) != null){
                            mDl = Integer.parseInt(line);
                        }
                        if ((line = bufReader.readLine()) != null){
                            mN = Integer.parseInt(line);
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

                        addLog("读取数据:\nCnt: " + String.valueOf(mCnt) + "\nLoss: " + String.valueOf(mLoss) + "\nDl: " + String.valueOf(mDl) + "\nN:" + String.valueOf(mN));

                    } catch (Exception e) {
                        Log.e(Common.TAG, "error when read file");
                    }
                }
            });

            new AlertDialog.Builder(this)
                .setTitle("请选择文件")
                .setView(fileListLayout)
                .show();

        } catch (Exception ioe){
            ioe.printStackTrace();
            return false;
        }

        return true;

        // Read from SD card
//        if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
//            mAlert("本机未插入SD卡，无法读取");
//            return false;
//        }
//
//        if (mFileName == null){
//            mToastMaker("请设置需要读取的产品的信息");
//            return false;
//        }
//        try{
//            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_SAVE_PATH);
//            if (!dir.exists()){
//                mToastMaker("尚未保存任何文件");
//                return false;
//            }
//
//            File file = new File(dir, mFileName);
//            if (!file.exists()){
//                mToastMaker("文件不存在，请确认文件名无误");
//                return false;
//            }
//
//            InputStreamReader inSReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bufReader = new BufferedReader(inSReader);
//            String line = "";
//            int dataNum = 0;
//            if ((line = bufReader.readLine()) != null){
//                mCnt = Integer.parseInt(line);
//            }
//
//            if ((line = bufReader.readLine()) != null){
//                mLoss = Integer.parseInt(line);
//            }
//
//            if ((line = bufReader.readLine()) != null){
//                mDl = Integer.parseInt(line);
//            }
//            if ((line = bufReader.readLine()) != null){
//                mN = Integer.parseInt(line);
//            }
//
//            while((line = bufReader.readLine()) != null){
//                int dataTmp = Integer.parseInt(line);
//                if (dataNum + 1 < Common.MAX_CURVE_LEN){
//                    mCurveData[dataNum] = dataTmp;
//                    dataNum++;
//                } else{
//                    mRealCurveLen = dataNum;
//                    break;
//                }
//
//            }
//
//            addLog("读取数据:\nCnt: " + String.valueOf(mCnt) + "\nLoss: " + String.valueOf(mLoss) + "\nDl: " + String.valueOf(mDl) + "\nN:" + String.valueOf(mN));
//
//        } catch (Exception ioe){
//            ioe.printStackTrace();
//            return false;
//        }
//
//        return true;
    }

    private void mUpdateDataUI() {
        double datatmp = (double) mCnt / Common.SCALE;
        String str = String.format("%.5f",datatmp);
        edtCnt.setText(str);

        datatmp = (double) mLoss / Common.SCALE;
        str = String.format("%.5f", datatmp);
        edtLoss.setText(str);

        datatmp = (double) mDl / Common.SCALE;
        str = String.format("%.5f", datatmp);
        edtDL.setText(str);

        datatmp = (double) mN / Common.SCALE;
        str = String.format("%.5f", datatmp);
        edtN.setText(str);

        tvOperartor.setText(Common.OPERATOR + mOperator);
        tvMeasureDate.setText(Common.MEASURE_TIME + mMeasureDate);

        addLog("数据更新成功");
    }

    private void mDrawCurve() {
        LineData data;
        ArrayList<String> xVal = new ArrayList<>();
        LineDataSet dataSet;
        ArrayList<Entry> yVal = new ArrayList<>();

        for(int i = 0; i < mRealCurveLen; ++i){
            yVal.add(new Entry(mCurveData[i], i));
            xVal.add(String.valueOf(i+1));
        }

        dataSet = new LineDataSet(yVal, "curve label");
        data = new LineData(xVal, dataSet);
        mCurveDrawer.setData(data);
        mCurveDrawer.animateY(3000);

    }

    // Button Event Overrider
    class LongClickEvent implements  View.OnLongClickListener{
        @Override
        public boolean onLongClick(View v){
//            if (v == svLogger || v == tvLog){
//                svLogger.setVisibility(View.INVISIBLE);
//                tvLog.setVisibility(View.INVISIBLE);
//                mCurveDrawer.setVisibility(VISIBLE);
//                return true;
//            } else
            if (v == mCurveDrawer) {
                svLogger.setVisibility(VISIBLE);
                tvLog.setVisibility(VISIBLE);
                mCurveDrawer.setVisibility(View.INVISIBLE);
                return true;
            }

            return false;
        }

    }

    class ClickEvent implements View.OnClickListener{
        @Override
        public void onClick(View v){
            if (v == btnMeasure) {
                if (rangeMode == Common.MEASURE_RANGE_UNKNOW){
                    mToastMaker("请先选择测量范围");
                    return;
                }
                if (bConnect) {
                    edtCnt.setText("0.00000");
                    edtLoss.setText("0.00000");
                    edtDL.setText("0.00000");
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
                            //    mToastMaker("Thread 发送数据失败 by Chino");
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

                if (mFileName != mProdType) {
                    mFileName = mProdType;
                }
                mSaveData();
            } else if (v == btnReadData) {
                if(!mReadData()) return;
                mUpdateDataUI();
                mDrawCurve();
            } else if (v == tvLog) {
                svLogger.setVisibility(View.INVISIBLE);
                tvLog.setVisibility(View.INVISIBLE);
                mCurveDrawer.setVisibility(VISIBLE);

            }
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String str = edtN.getText().toString();
            try{

                Double value = Double.parseDouble(str);
                if (value < Common.MIN_N || value > Common.MAX_N) {
                    mToastMaker("您输入的折射率超过范围，请重新输入");
                }

            } catch (Exception e){
                mToastMaker("您输入的折射率有误");
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // TODO change the N while change the seekbar
            
        }
    };

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

                    // addLog("连接成功了噢 by Chino"); // debug

                    mToastMaker("蓝牙连接成功");
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
        svLogger.post(new Runnable() {
            public void run() {
                svLogger.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }


    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            int resTmp = Common.MIN_N + 10*i;
            if (resTmp > Common.MAX_N || resTmp < Common.MIN_N) {
                mToastMaker("n应该在1.440～1.470之间");
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
            tvDl.setText(Common.BCC_MODE_DL);
        } else if (mode == Common.MEASURE_MODE_GXC) {
            tvTitle.setText(Common.GXC_MODE_TITLE);
            tvDl.setText(Common.GXC_MODE_L);
        }
    }

    public void mAlert(String alertContent){
        new AlertDialog.Builder(this)
            .setTitle(alertContent)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("确定", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    return;
                }
            }).show();
    }

    public void mToastMaker(String tosStr){
        Toast.makeText(ResultController.this, tosStr, 1000).show();
    }

}







