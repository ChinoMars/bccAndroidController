package com.chinomars.bccAndroidViewerCommon;

/**
 * Created by Chino on 3/8/16.
 */
public class Common {
    // Commnunications Prtocol
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int MESSAGE_CONNECT = 6;
    public static final int MESSAGE_CONNECT_SUCCEED = 7;
    public static final int MESSAGE_CONNECT_LOST = 8;
    public static final int MESSAGE_RECV = 10;
    public static final int MESSAGE_EXCEPTION_RECV = 11;

    // C.P. plus
    public static final int MESSAGE_SEND_COMMAND = 12; // 发送协议
    public static final int MESSAGE_RECV_RESULT = 13; // 接收协议
    public static final int MESSAGE_UPDATE_PROGRESS = 14; // update the progress bar
    public static final int MESSAGE_TIMEOUT = 15; // receiving time out

    // Static Setting
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String BIND_BT_MAC = "30:14:12:26:11:35"; // test blue tooth
//    public static final String BIND_BT_MAC = "00:0E:EA:CF:62:55";
//    public static final String LOCAL_BT_MAC = "86:28:E1:24:46:6E"; // ANDROID PAD
    public static final String LOCAL_BT_MAC = "22:22:41:C4:81:7C"; // MULTIPLE SYS PAD OLD
    public static final int MIN_N = 14400; // mim n
    public static final int MAX_N = 14700; // max n
    public static final double SCALE = 10000; // based on the MAX_N and MIN_N
    public static final int RESULT_AND_DATA_LEN = 848; // 总数据长
    public static final int MAX_CURVE_LEN = 400;
    public static final int MAX_FILE_NUM = 999;
    public static final int FILE_SUFFIX_LEN = 3;
    public static final int RECEIVE_TYPE_DATA = 1;
    public static final int RECEIVE_TYPE_RESULT = 0;
    public static final int RECEIVE_DATA_SECTION_LEN = 104; // data section length
    public static final int RECEIVE_DATA_RESULT_LEN = 16; // result section length

    public static final int TIME_OUT = 15; // time out of 12s will alert

	public static final int MinusFlagZero = 0; // for judging negative Loss

    // Control Setting
    public static final Boolean IS_DEBUG = false; // set to true to show debug log
    public static final int DROP_HEAD_DATA_LEN = 1; // drop the first length data of mCurveDrawer

    // Measure mode
    public static final Integer MEASURE_MODE_UNKNOW = 0;
    public static final Integer MEASURE_MODE_BCC = 1;
    public static final Integer MEASURE_MODE_GXC = 2;
    public static final Integer MESURE_MODE_CHECK_FREQ = 3; // 频带校准
    public static final Integer MESURE_MODE_CHECK_LENGTH_TO_ZERO = 4; //长度校零
    public static final Integer MEASURE_RANGE_UNKNOW = 0;
    public static final Integer MEASURE_RANGE_LONG = 1;
    public static final Integer MEASURE_RANGE_MID1 = 2;
    public static final Integer MEASURE_RANGE_MID2 = 3;
    public static final Integer MEASURE_RANGE_SHORT = 4;

    // Erro Code
    public static final String TOAST = "toast";
    public static final String TAG = "BlueToothTool";

    // Static String Content
    public static final String APP_NAME = "光纤长度精密测量仪";
    public static final String ABOUT_CONTENT = "请选择软件工作模式后再开始工作:\n1.干涉型臂长差测量模式\n2.光线链路长度测量模式";
    public static final String BTN_YES = "确定";
    public static final String BCC_MODE_TITLE = "干涉型臂长差测量仪";
    public static final String GXC_MODE_TITLE = "光纤链路长度测量仪";
    public static final String BCC_MODE_DL = "臂长差：";
    public static final String GXC_MODE_L = "长度：";
    public static final String OPERATOR = "操作人：";
    public static final String MEASURE_TIME = "测量时间：";

}








