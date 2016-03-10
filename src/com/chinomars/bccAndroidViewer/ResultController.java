package com.chinomars.bccAndroidViewer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.chinomars.bccAndroidViewerCommon.Common;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;

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

    BluetoothAdapter btAdapt = null;
    BluetoothSocket btSocket = null;

    Boolean bConnect = false;
    String strName = null;
    String strAddr = null;
    int nNeed = 0;
    byte[] bRecv = new byte[1024];
    int nRecved = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        curveDrawer = (ScrollView) this.findViewById(R.id.curve_drawer);

        tvTitle = (TextView) this.findViewById(R.id.result_title);

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

    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b)
        {
            double result = Common.MIN_N + 0.001 * i;
            if (result > Common.MAX_N || result < Common.MIN_N)
            {
                Toast.makeText(ResultController.this, "n应该在1.440～1.449之间", 1000).show();
                return;
            }

            String str = String.format("%.3f", result);
            edtN.setText(str);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar)
        {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {

        }
    }

    class CheckedChangeEvent implements RadioGroup.OnCheckedChangeListener
    {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkId)
        {
            if (checkId == R.id.rdio_long)
            {
                setMeasureRange(Common.MEASURE_RANGE_LONG);
            }
            else if(checkId == R.id.rdio_mid)
            {
                setMeasureRange(Common.MEASURE_RANGE_MID);
            }
            else if(checkId == R.id.rdio_short)
            {
                setMeasureRange(Common.MEASURE_RANGE_SHORT);
            }
        }

    }

    private void setMeasureRange(int rangeCode)
    {
        rangeMode = rangeCode;
    }

    class ClickEvent implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            if(v == btnMeasure)
            {
                // TODO
            }
            else if(v == btnParamSetter)
            {
                // TODO 用户设置保存数据的命名格式
            }
            else if(v == btnSaveData)
            {
                // TODO
            }
        }
    }
}



