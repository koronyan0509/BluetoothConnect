package com.example.mizun.kitaharasystem;

/**
 * Created by korona on 2017/04/11.
 */
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothTask {
    private static final String TAG = "BluetoothTask";

    /**
     * UUIDはサーバと一致している必要がある。
     * - 独自サービスのUUIDはツールで生成する。（ほぼ乱数）
     * - 注：このまま使わないように。
     */
    private static final UUID APP_UUID = UUID.fromString("17fcf242-f86d-4e35-805e-546ee3040b84");

    private MainActivity activity;
    private OrientationEstimater orientationEstimater = new OrientationEstimater();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice = null;
    private BluetoothSocket bluetoothSocket;
    private InputStream btIn;
    private OutputStream btOut;
    public int sendflag = 0;
    public long currenttime = 0;
    public long starttime = 0;
    public int attacktiming_switch = 0;
    private String condition = "off";

    public BluetoothTask(MainActivity activity) {
        this.activity = activity;
    }

    /**
     * Bluetoothの初期化。
     */
    public void init() {
        // BTアダプタ取得。取れなければBT未実装デバイス。
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            activity.errorDialog("This device is not implement Bluetooth.");
            return;
        }
        // BTが設定で有効になっているかチェック。
        if (!bluetoothAdapter.isEnabled()) {
            // TODO: ユーザに許可を求める処理。
            activity.errorDialog("This device is disabled Bluetooth.");
            return;
        }
    }
    /**
     * @return ペアリング済みのデバイス一覧を返す。デバイス選択ダイアログ用。
     */
    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    /**
     * 非同期で指定されたデバイスの接続を開始する。
     * - 選択ダイアログから選択されたデバイスを設定される。
     * @param device 選択デバイス
     */
    public void doConnect(BluetoothDevice device) {
        bluetoothDevice = device;
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            new ConnectTask().execute();
        } catch (IOException e) {
            Log.e(TAG,e.toString(),e);
            activity.errorDialog(e.toString());
        }
    }

    //経過時間を計算してサーバ側に送信するタスクを生成
    public void timesend(long starttime, float position,int attacktiming_control_on,int attacktiming_control_off) {
        try {
            currenttime = System.currentTimeMillis() - starttime;
            if(attacktiming_control_on == 1){
                attacktiming_switch = 1;
            }else if(attacktiming_control_off == 1){
                attacktiming_switch = -1;
            } else {
                attacktiming_switch = 0;
            }
            String SendData = currenttime + "," + position + "," + attacktiming_switch + "\r\n";
            btOut.write(SendData.getBytes());
            btOut.flush();
        } catch(Throwable t) {
            doClose();
        }
    }

    /**
     * 非同期でBluetoothの接続を閉じる。
     */
    public void doClose() {
        new CloseTask().execute();
    }

    /**
     * AsyncTaskは非同期処理のための
     * Bluetoothと接続を開始する非同期タスク。
     * - 時間がかかる場合があるのでProcessDialogを表示する。
     * - 双方向のストリームを開くところまで。
     */
    private class ConnectTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected void onPreExecute() {
            activity.showWaitDialog("Connect Bluetooth Device.");
        }
//非同期処理の前に行われる処理
        @Override
        protected Object doInBackground(Void... params) {
            //非同期で処理を実行
            try {
                bluetoothSocket.connect();
                btIn = bluetoothSocket.getInputStream();
                btOut = bluetoothSocket.getOutputStream();
            } catch (Throwable t) {
                doClose();
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            //接続エラーが起きた場合エラーダイアログを表示
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                activity.errorDialog(result.toString());
            } else {
                activity.hideWaitDialog();
            }
        }
    }

    /**
     * Bluetoothと接続を終了する非同期タスク。
     * - 不要かも知れないが念のため非同期にしている。
     */
    private class CloseTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            try {
                try{btOut.close();}catch(Throwable t){/*ignore*/}
                try{btIn.close();}catch(Throwable t){/*ignore*/}
                bluetoothSocket.close();
            } catch (Throwable t) {
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                activity.errorDialog(result.toString());
            }
        }
    }



    }