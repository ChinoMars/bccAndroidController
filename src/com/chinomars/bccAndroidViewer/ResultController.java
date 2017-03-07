package com.chinomars.bccAndroidViewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.graphics.Color;
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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import static android.view.View.VISIBLE;

/**
 * Created by Chino on 3/7/16.
 */
public class ResultController extends Activity {
    public static byte[] PKGHEAD = {(byte)0xA5, 0x5A};
    public static String FILE_SAVE_PATH = "/BccData/";
    public static String LINE_END = "\r\n";
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    LineChart mCurveDrawer;
    ScrollView svLogger;
    ProgressBar pgInfor;
    TextView tvTitle, tvLog, tvDl;
    TextView tvOperartor, tvMeasureDate;
    EditText edtCnt, edtLoss, edtDL, edtN;
    SeekBar sekbN;
    RadioGroup rdiogRangeSetter;
    Button btnMeasure, btnParamSetter, btnSaveData, btnReadData;

    BluetoothAdapter btAdapt = null;
    BluetoothSocket btSocket = null;

    int rangeMode = Common.MEASURE_RANGE_UNKNOW;
    int mCnt = 0, mLoss = 0, mN = Common.DEFAULT_N;
    double  mDl = 0;
    int recvL = 0, sendN = Common.DEFAULT_N;
//    int[] mCurveData = new int[Common.MAX_CURVE_LEN];
    Vector<Integer> mCurveData = new Vector<>();
//    int mRealCurveLen = 0;

    Boolean bConnect = false;
    String strName;
    String strAddr;
    int workMode = Common.MEASURE_MODE_GXC;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;
    Boolean ifResetTimmer = false;
//    CRC32 checkSum;
    Boolean canUpdateResult = true;
    String mFileName; // set the file name to save
    String mOperator,
            mProdType,
            mProdId,
            mProduceDate,
            mMeasureDate,
            mComment;

    Boolean isLegalDevice = false; // true for debug, false for release

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.result);

        mCurveDrawer = (LineChart) this.findViewById(R.id.curve_drawer);
        
        svLogger = (ScrollView) this.findViewById(R.id.sv_logger);
        tvLog = (TextView) this.findViewById(R.id.tv_Log);
        if (Common.IS_DEBUG) {
            mCurveDrawer.setOnLongClickListener(new LongClickEvent());
            svLogger.setOnLongClickListener(new LongClickEvent());
            tvLog.setOnClickListener(new ClickEvent());
        }

        pgInfor = (ProgressBar) this.findViewById(R.id.progressBar2);


        edtCnt = (EditText) this.findViewById(R.id.edt_cnt);
        edtDL = (EditText) this.findViewById(R.id.edt_dl);
        edtDL.addTextChangedListener(watcherforL);
        edtLoss = (EditText) this.findViewById(R.id.edt_loss);
//        edtN = (EditText) this.findViewById(R.id.edt_n);
//        edtN.addTextChangedListener(textWatcher);

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
//        tvTitle.setOnClickListener(new ClickEvent());
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

        if (btAdapt.getAddress().toString().equals(Common.LOCAL_BT_MAC)) {
            isLegalDevice = true;
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
        Log.d(Common.TAG, "Free result");
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
//        new AlertDialog.Builder(this, R.style.altDialgStyle)
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

        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
            .setTitle("请输入本次测量信息")
            .setView(paramLayout)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which){
                    EditText edtTmp = (EditText) paramLayout.findViewById(R.id.edt_operator);
                    String strTmp = edtTmp.getText().toString();
//                    if (isOperatorValid(strTmp)){
                    if (strTmp.isEmpty()) {
                        mOperator = "none";
                    } else {
//                        mToastMaker("操作人信息不合法，请重新设置");
//                        return;
                        mOperator = strTmp;
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

                    strTmp = ((EditText) paramLayout.findViewById(R.id.edt_produce_date)).getText().toString();
                    if (strTmp.isEmpty()) {
                        mProduceDate = "none";
                    } else {
                        mProduceDate = strTmp;
                    }

                    strTmp = ((EditText) paramLayout.findViewById(R.id.edt_measure_date)).getText().toString();
                    if (strTmp.isEmpty()) {
                        mMeasureDate = "none";
                    } else {
                        mMeasureDate = strTmp;
                    }

                    strTmp = ((EditText) paramLayout.findViewById(R.id.edt_measure_date)).getText().toString();
                    if (strTmp.isEmpty()) {
                        mComment = "none";
                    } else {
                        mComment = strTmp;
                    }

                    addLog("测量信息为：" + mOperator + mProdType + mProdId + mProduceDate + mMeasureDate + mComment);

                    tvOperartor.setText(Common.OPERATOR + mOperator);
                    tvMeasureDate.setText(Common.MEASURE_TIME + mMeasureDate);

                    if (mFileName == null) {
                        mFileName = mProdType + mProdId;
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
            // when get send success info, set the nNeed and nRecved
            try {
                if (mmInStream != null) {
                    if (mmInStream.available() != 0) {
                        byte[] flushByte = new byte[1024];
                        int flushLen = mmInStream.read(flushByte);
                    }
                }
            } catch (Exception e) {
                Log.e(Common.TAG, "error in flush inputstream");
            }
            nNeed = Common.RESULT_AND_DATA_LEN;
            nRecved = 0;
            mmOutStream.write(sendCommand);
            addLog("发送开始测量指令: " + bytesToString(sendCommand, 8));

            canUpdateResult = true;

        } catch(Exception e) {
            mToastMaker("发送命令失败");
            return;
        }

    }


    private void mSaveData(){
        // FILE METHOD save to SD card, absolute PATH: SDCARD/BccDATA/
        try{
            Log.d(Common.TAG, "error when saveing data" + mFileName);
            File sdCardDir = Environment.getExternalStorageDirectory();
//            File saveDir = new File(sdCardDir.getAbsolutePath() + FILE_SAVE_PATH + mProdType + mProdId);
            File saveDir = new File(sdCardDir.getAbsolutePath() + FILE_SAVE_PATH + mProdType);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File[] files = saveDir.listFiles();
            int fileNum = files.length;
            if (fileNum > Common.MAX_FILE_NUM) {
                mToastMaker("超过文件数量，请修改产品型号或产品编号");
                return;
            }
            String pre = "";
            switch (Common.FILE_SUFFIX_LEN - String.valueOf(fileNum).length()){
                case 1:
                    pre = "0";
                    break;
                case 2:
                    pre = "00";
                    break;
                default:
                    break;

            }
            String saveFileName = mFileName + pre + String.valueOf(files.length + 1);

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

            int curveLen = mCurveData.size();
            for (int i = 0; i < curveLen; ++i){
                oSWriter.write(String.valueOf(mCurveData.get(i)) + LINE_END);
            }

            oSWriter.flush();
            oSWriter.close();

            addLog("数据保存到:" + svFile);

            mToastMaker("数据保存成功");

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
//            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_SAVE_PATH + mProdType + mProdId);
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_SAVE_PATH + mProdType);
            if (!dir.exists()){
                mToastMaker("尚未保存任何文件");
                return false;
            }

            File[] files = dir.listFiles();

            LinearLayout fileListLayout = new LinearLayout(this);
            fileListLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ListView lvReadFiles = new ListView(this);
            lvReadFiles.setFadingEdgeLength(0);

            ArrayList<String> alFileNames = new ArrayList<>();
            for (int i = 0; i < files.length; ++i) {
                alFileNames.add(files[i].getName());
            }

            ArrayAdapter<String> adtReadFiles = new ArrayAdapter<>(ResultController.this, android.R.layout.simple_list_item_1, alFileNames);
            lvReadFiles.setAdapter(adtReadFiles);

            fileListLayout.addView(lvReadFiles);

            final AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("请选择文件")
                .setView(fileListLayout)
                .setNegativeButton("取消", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                        dialog.cancel();
                    }
                }).create();
//            dialog.setCanceledOnTouchOutside(false); // 取消点击对话框区域外的部分弹出
            dialog.show();

            lvReadFiles.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
                    File fileToRead = new File(dir, alFileNames.get(i));
                    try{
                        InputStreamReader inSReader = new InputStreamReader(new FileInputStream(fileToRead));
                        BufferedReader bufReader = new BufferedReader(inSReader);
                        String line = "";
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

                        if (!mCurveData.isEmpty()) {
                            mCurveData.clear();
                        }
                        int dataNum = 0;
                        while((line = bufReader.readLine()) != null){
                            int dataTmp = Integer.parseInt(line);
                            if (dataNum + 1 < Common.MAX_CURVE_LEN){
                                mCurveData.add(dataTmp);
                                dataNum++;
                            } else{
                                addLog("到达数据容量");
                                break;
                            }

                        }
//                        mRealCurveLen = dataNum;

//                        canUpdateResult = true;
                        mUpdateDataUI();
                        mDrawCurve();

                        mToastMaker("数据加载成功");
                        addLog("读取数据:\nCnt: " + String.valueOf(mCnt) + "\nLoss: " + String.valueOf(mLoss) + "\nDl: " + String.valueOf(mDl) + "\nN:" + String.valueOf(mN));
                        dialog.dismiss();
                    } catch (Exception e) {
                        Log.e(Common.TAG, "error when read file");
                    }
                }
            });
        } catch (Exception e){
            Log.e(Common.TAG, "error in read direction.");
            return false;
        }

        return true;

    }

    private void mUpdateDataUI() {
        if (!canUpdateResult) {
            mToastMaker("当前无法更新测量结果");
            return;
        }

        double datatmp = (double) mCnt;
        String str = String.format("%.1f",datatmp);
        edtCnt.setText(str);

        datatmp = (double) mLoss / 100; // change from "Common.Scale / 10"
        str = String.format("%.6f",datatmp);
        edtLoss.setText(str);

        datatmp = (double) mDl / Common.SCALE / 100;
        str = String.format("%.6f", datatmp);
        edtDL.setText(str);

        if (mN >= Common.MIN_N && mN <= Common.MAX_N){
            datatmp = (double) mN / Common.SCALE;
            str = String.format("%.4f", datatmp);
            edtN.setText(str);
        }

        tvOperartor.setText(Common.OPERATOR + mOperator);
        tvMeasureDate.setText(Common.MEASURE_TIME + mMeasureDate);

        addLog("RESULT数据更新成功");
    }

    // 仅用在画波形时
    private int findMaxer()
    {
        int maxer = Math.abs(mCurveData.get(0));
        for (int i : mCurveData){
            int tmp = Math.abs(i);
            if (tmp > maxer) {
                maxer = tmp;
            }
        }

        return maxer;
    }

    private int findMiner() {
        int miner = Math.abs(mCurveData.get(0));
        for (int i : mCurveData) {
            int tmp = Math.abs(i);
            if (tmp < miner) {
                miner = tmp;
            }
        }

        return miner;
    }

    private void mDrawCurve() {
        if (!canUpdateResult) {
            mToastMaker("目前无法更新数据和测量结果");
            return;
        }
        if (!mCurveData.isEmpty()){
            try{
                ArrayList<String> xVal = new ArrayList<>();
                ArrayList<Entry> yVal = new ArrayList<>();

                int maxer = findMaxer();
                int miner = findMiner();
                int curveDataLen = mCurveData.size();
                int base = (maxer == miner) ? 1 : (maxer - miner);

                // specific method when data of maxer and miner are too close
                float avg = (1 - (float) (maxer - miner) / base / 2.0f); // 0.5
                Boolean isSpecific = false;
                float transferScale = 1;
                if(base < Common.FLAT_DATA_THRESH_HOLD) {
                    isSpecific = true;
                    transferScale = (float)base / (float)Common.FLAT_DATA_THRESH_HOLD ;
                }

                for(int i = 0; i < curveDataLen; ++i){
                    if (mCurveData.get(i) == 0) {
                        Log.d(Common.TAG, "got a ZERO here at " + String.valueOf(i));
                        mAlert("got a ZERO here at " + String.valueOf(i));
                    }
                    float tmpVal = 1 - (float) (Math.abs(mCurveData.get(i)) - miner) / base;

                    // Speicifc method
                    if(isSpecific) {
                        if(tmpVal > avg) {
                            tmpVal = avg + (tmpVal - avg) * transferScale;
                        } else {
                            tmpVal = avg - (avg - tmpVal) * transferScale;
                        }
                    }

                    yVal.add(new Entry(tmpVal, i));
                    xVal.add("" + (i+1));
                }

                LineDataSet dataSet = new LineDataSet(yVal, "频谱波形");
                dataSet.setColor(Color.GREEN);
                dataSet.setLineWidth(1f);
                dataSet.setDrawCircles(false);
//                dataSet.setCubicIntensity(0.6f);

                LineData data = new LineData(xVal, dataSet);
                mCurveDrawer.setDescription("");
                mCurveDrawer.setData(data);
                mCurveDrawer.setBackgroundColor(Color.rgb(201, 201, 201));
                mCurveDrawer.setGridBackgroundColor(Color.BLACK);
                mCurveDrawer.setBorderColor(Color.RED);

                YAxis yl = mCurveDrawer.getAxisLeft();
                yl.setAxisMaxValue(1.1f);
                yl = mCurveDrawer.getAxisRight();
                yl.setEnabled(false);

                // TODD set yLable
//                YAxis yAxis = mCurveDrawer.get

                mCurveDrawer.animateX(3000);

            } catch (Exception e) {
                Log.e(Common.TAG, "error in drawing the curve");
            }
        }

//        canUpdateResult = false;

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
                    if (!isLegalDevice) {
                        addLog("ILLEGAL DEVICE");
                        mToastMaker("ILLEGAL DEVICE");
                        return;
                    }
                    addLog("连接成功");

                    mToastMaker("蓝牙连接成功");
                    bConnect = true;
                    btnMeasure.setEnabled(true);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] bufRev = new byte[1024];
                            int nRecv = 0;
                            int outTimmer = 0;
                            while(bConnect) {
                                try {
                                    if (ifResetTimmer) {
                                        outTimmer = 0;
                                        ifResetTimmer = false;
                                    }

                                    int streamLenAval = mmInStream.available();
                                    // Log.e(Common.TAG, "Start Recv, data rev avaiable: " + String.valueOf(streamLenAval) + "bytes");
                                    if (streamLenAval < Common.RESULT_AND_DATA_LEN) {
                                        if (nNeed > 0) {
                                            Log.d(Common.TAG, "Start Recv, data rev avaiable: " + String.valueOf(streamLenAval) + "bytes");
                                            mHandler.obtainMessage(Common.MESSAGE_UPDATE_PROGRESS, outTimmer, -1).sendToTarget();
                                            if (outTimmer > Common.TIME_OUT) {
                                                mHandler.obtainMessage(Common.MESSAGE_TIMEOUT, 0, -1).sendToTarget();
                                                // TODO time out to flush inputstream and ask for resending
                                                byte[] flushByte = new byte[streamLenAval];
                                                nRecv = mmInStream.read(flushByte); // flush inputstream
                                                ifResetTimmer = true;
                                            }
                                        }
                                        Thread.sleep(1000);
                                        outTimmer++;
                                        continue;
                                    }

                                    if (streamLenAval > Common.RESULT_AND_DATA_LEN) {
                                        Log.d(Common.TAG, "too much data");
                                        // TODO send resend commend
                                        byte[] flushByte = new byte[streamLenAval];
                                        nRecv = mmInStream.read(flushByte); // flush inputstream
                                        continue;
                                    }

                                    outTimmer = Common.TIME_OUT;

                                    nRecv = mmInStream.read(bufRev);

                                    byte[] nPacket = new byte[nRecv];
                                    System.arraycopy(bufRev, 0, nPacket, 0, nRecv);

                                    if(nNeed > 0 && nRecved < nNeed) {

                                        mHandler.obtainMessage(Common.MESSAGE_RECV, nRecv, -1, nPacket).sendToTarget();
                                        mHandler.obtainMessage(Common.MESSAGE_UPDATE_PROGRESS, outTimmer, -1).sendToTarget(); // fake full prograss

                                    }


                                } catch (Exception e) {
                                    Log.e(Common.TAG, "Recv thread:" + e.getMessage());
                                    mHandler.sendEmptyMessage(Common.MESSAGE_EXCEPTION_RECV);
                                    break;
                                }
                            }

                            Log.d(Common.TAG, "Exit while");
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
                    byte[] bBuf = (byte[]) msg.obj; // 848 bytes data

                    Log.d(Common.TAG, "Recvd bytes: " + String.valueOf(msg.arg1));

                    parseRevData(bBuf, msg.arg1); // ought to contain 848 bytes
                    mResetDataFlag(); // receive sucessfully and reset data in need and received data length

//                    for (int i = 0; i < msg.arg1; ++i) {
//                        String str = Integer.toHexString(0xff & bBuf[i]);
//                        addLog(str);
//                    }

                    // 接收成功重置所需要的数据长度和已接受的长度
//                    if (nRecved >= nNeed && nNeed != 0) {
//                        nRecved = 0;
//                        nNeed = 0;
//                    }

//                    if (canUpdateResult) {
//                        mUpdateDataUI();
//                        mDrawCurve();
//                    }

                    break;
                case Common.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), 
                        msg.getData().getString(Common.TOAST), 
                        Toast.LENGTH_SHORT).show();
                    break;
                case Common.MESSAGE_UPDATE_PROGRESS:
                    int progress = msg.arg1;
                    // Log.e(Common.TAG, "### MESSAGE_UPDATE_PROGRESS: recent progress is:" + String.valueOf(progress));
                    updateProgress(progress);
                    break;
                case Common.MESSAGE_TIMEOUT:
                    mToastMaker("数据传输超时，请重试。");
                    mResetDataFlag();
                    break;
            }
        }

    };

    public void updateProgress(int progress) {
        // int prog = progress * 100 / Common.RESULT_AND_DATA_LEN;
        int prog = progress * 100 / Common.TIME_OUT;
        Log.e(Common.TAG, "### updateProgress " + String.valueOf(prog));
        pgInfor.setProgress(prog);
    }


    public void mResetDataFlag() {
        pgInfor.setProgress(0);
        nNeed = 0;
        nRecved = 0;
    }


    private void parseRevData(byte[] revByteBuf, int len) {
        if (nNeed == 0 || nRecved >= nNeed) {
            return;
        }

        if (len < Common.RESULT_AND_DATA_LEN) {
            addLog("not enough data");
            nNeed = 0;
            nRecved = 0; // need to resend
            return;
        }

        int idx = 0;
        Boolean isDataHead1 = false;
        Boolean isDataHead = false;
        int dropDataCnt = 0;
        while(idx < len) {
            switch (revByteBuf[idx]) {
                case (byte) 0xA5:
                    isDataHead1 = true;
                    break;
                case 0x5A:
                    if (isDataHead1) {
                        isDataHead = true;
                    }
                    break;
                case 0:
                    try {
                        if (isDataHead) {
                            int revResLen = Common.RECEIVE_DATA_RESULT_LEN;
                            byte[] resultSection = new byte[revResLen];
                            System.arraycopy(revByteBuf, idx-2, resultSection, 0, revResLen);
                            byte checkSum = mGenCheckSum(resultSection, revResLen-1);
                            if (checkSum == resultSection[revResLen-1]) { // data correct
                                // length data
                                int dataTmp = (int) (((resultSection[3]&0xff) << 24) | ((resultSection[4]&0xff) << 16) | ((resultSection[5]&0xff) << 8) | (resultSection[6] & 0xff));
                                recvL = dataTmp;

                                // loss data
                                dataTmp = (int) (((Common.MinusFlagZero&0xff) << 24) | ((resultSection[8]&0xff) << 16) | ((resultSection[9]&0xff) << 8) | (resultSection[10] & 0xff));
								if(resultSection[7] != Common.MinusFlagZero){
									mLoss = -dataTmp;
								} else mLoss = dataTmp;

                                // wave cnt data
                                dataTmp = (int) (((resultSection[11]&0xff) << 24) | ((resultSection[12]&0xff) << 16) | ((resultSection[13]&0xff) << 8) | (resultSection[14] & 0xff));
                                mCnt = dataTmp;

                                nRecved += revResLen;

                                // For measuring N
                                mDl = mDl * sendN / recvL;

                                mUpdateDataUI();

                                idx += revResLen - 3; // -1 for ++idx out of switch
                            }
                        }
                    } catch (Exception e) {
                        Log.e(Common.TAG, "error in proc RESULT DATA");
                    }

                    break;
                default:
                    try {
                        if (revByteBuf[idx] > 0 && revByteBuf[idx] < 9) {
                            if (isDataHead) {
                                int revDataLen = Common.RECEIVE_DATA_SECTION_LEN;

                                byte[] dataSection = new byte[revDataLen];
                                System.arraycopy(revByteBuf, idx-2, dataSection, 0, revDataLen);
                                byte checkSum = mGenCheckSum(dataSection, revDataLen-1);
//                                if ((!mCurveData.isEmpty()) && (revByteBuf[idx] > 1) && (mCurveData.size()/50 != revByteBuf[idx] - 1)) {
//                                    break;
//                                }

                                if (checkSum == dataSection[revDataLen-1]) {
                                    if (dataSection[2] == 1) {
                                        mCurveData.clear(); // init the mCurveData when first data section received
                                    }

                                    // push 100 data to mCurveData
                                    int curveDataLen = mCurveData.size() + Common.DROP_HEAD_DATA_LEN;
                                    if (curveDataLen / 50 != revByteBuf[idx] - 1) {
                                        Log.e(Common.TAG, "unmatch: not the correct section");
                                        break;
                                    }

                                    for (int i = 3; i < revDataLen-2; i += 2) {
                                        int dataTmp =  (int) ((dataSection[i] << 8) | (dataSection[i+1] & 0xff)); // i + 1 take rask to overflow
                                        if (dropDataCnt < Common.DROP_HEAD_DATA_LEN) { // drop the first error data
                                            dropDataCnt++;
                                            continue;
                                        } else {
                                            mCurveData.add(dataTmp);                                            
                                        }

                                        // test
                                        addLog(String.valueOf(dataTmp));
                                    }
                                    nRecved += revDataLen;
                                    if (mCurveData.size() == Common.MAX_CURVE_LEN || dataSection[2] == 8) { // not rub
                                        mDrawCurve();

                                    }
                                    idx = idx + revDataLen - 3; // -1 for ++idx out of switch

                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(Common.TAG, "error in proc Curve Data");
                    }
                    break;

            }

            ++idx;
        }

        if (nRecved >= nNeed && nNeed != 0) {
            nRecved = 0;
            nNeed = 0;
        }

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

    private byte mGenCheckSum(byte[] buf, int len) {
        byte res = 0x00;
        for (int i = 0; i < len; ++i) {
            res ^= buf[i];
        }

        return res;
    }

    private byte[] mGenMeasureCmd() {
        byte[] cmdRes = new byte[8];
        cmdRes[0] = PKGHEAD[0];
        cmdRes[1] = PKGHEAD[1];
        cmdRes[2] = (byte) (workMode & 0xff);

        // int N to 2 bytes
        cmdRes[3] = (byte) (sendN >> 8);
        cmdRes[4] = (byte) (sendN & 0xff);

        cmdRes[5] = (byte) (rangeMode >> 8);
        cmdRes[6] = (byte) (rangeMode & 0xff);

        cmdRes[7] = mGenCheckSum(cmdRes, 7);

        return cmdRes;
    }

    class ClickEvent implements View.OnClickListener{
        @Override
        public void onClick(View v){
            if (v == btnMeasure) {
                if (rangeMode == Common.MEASURE_RANGE_UNKNOW){
                    mToastMaker("请先选择测量范围");
                    return;
                }
                if(0 == mDl) {
                    mToastMaker("请输入光纤长度");
                    return;
                }
                if (bConnect) {
                    edtCnt.setText("0.0");
                    edtLoss.setText("0.00");
//                    edtDL.setText("0.00000");
                    mCurveDrawer.clear();
                    byte[] sendCmd = mGenMeasureCmd();
                    Log.d(Common.TAG, "workmode: " + String.valueOf(sendCmd[6]));

                    // TODO add command data
                    send(sendCmd);
                    ifResetTimmer = true;
                }

                // clear focus when click measure button
//                edtN.clearFocus();

            } else if (v == btnParamSetter) {
                showParamSetDialog();

            } else if (v == btnSaveData) {
                if (mFileName == null) {
                    showParamSetDialog();
                    return;
                }

                if (mFileName != mProdType) {
//                    mFileName = mProdType;
                    mFileName = mProdType + mProdId;
                }
                mSaveData();
            } else if (v == btnReadData) {
                if(!mReadData()) return;
//                mUpdateDataUI();
//                mDrawCurve();
            } else if (v == tvLog) {
                svLogger.setVisibility(View.INVISIBLE);
                tvLog.setVisibility(View.INVISIBLE);
                mCurveDrawer.setVisibility(VISIBLE);

            } else if (v == tvTitle) {
                String recentTitle = tvTitle.getText().toString();
                if (recentTitle.equals(Common.BCC_MODE_TITLE)) {
                    tvTitle.setText(Common.GXC_MODE_TITLE);
//                    tvDl.setText(Common.GXC_MODE_L);
                    workMode = Common.MEASURE_MODE_GXC;
                } else {
                    tvTitle.setText(Common.BCC_MODE_TITLE);
//                    tvDl.setText(Common.BCC_MODE_DL);
                    workMode = Common.MEASURE_MODE_BCC;
                }
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
                int value = (int)(Double.valueOf(str) * Common.SCALE);
                if (value < Common.MIN_N || value > Common.MAX_N) {
                    mToastMaker("折射率超过测量范围");
                }

            } catch (Exception e){
                mToastMaker("您输入的折射率有误");
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // TODO change the N while change the seekbar
            String str = edtN.getText().toString();
            try{
                int value = (int) (Double.valueOf(str) * Common.SCALE);
                if (value < Common.MIN_N) {
                    value = Common.MIN_N ;
                } else if(value > Common.MAX_N) {
                    value = Common.MAX_N;
                }

//                edtN.clearFocus();
                int prog = value - Common.MIN_N;
                sekbN.setProgress(prog/10);

            } catch (Exception e) {
                Log.e(Common.TAG, "error edt makes seekbar move");
            }
        }
    };

    // TODO add textwatcher to mDl
    private TextWatcher watcherforL = new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String str = edtDL.getText().toString();
            try{
                mDl = Double.valueOf(str);
            } catch (Exception e) {
                Log.d(Common.TAG, "Error when setting Length of fiber");
            }

        }
    };

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            int resTmp = Common.MIN_N + 10*i;
            if (resTmp > Common.MAX_N || resTmp < Common.MIN_N) {
                mToastMaker("n应该在1.440～1.470之间");
                return;
            }

            mN = resTmp;
            double result = (double) resTmp / Common.SCALE;
//            addLog("当前折射率为: " + String.valueOf(result));
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
            else if(checkId == R.id.rdio_mid1) {
                setMeasureRange(Common.MEASURE_RANGE_MID1);
            }
            else if(checkId == R.id.rdio_mid2) {
                setMeasureRange(Common.MEASURE_RANGE_MID2);
            }
            else if(checkId == R.id.rdio_short) {
                setMeasureRange(Common.MEASURE_RANGE_SHORT);
            }

            Log.d(Common.TAG, "rangeMode: " + String.valueOf(rangeMode));

        }

    }

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

        if (tvLog.length() > 300) {
            tvLog.setText("");
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
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
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







