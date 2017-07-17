package com.example.mizun.kitaharasystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    private BluetoothTask bluetoothTask = new BluetoothTask(this);
    public BluetoothDevice device;

    private ProgressDialog waitDialog;
    private String errorMessage = "";

    public int attacktiming_control_on = 0;
    public int attacktiming_control_off = 0;
    public long starttime = 0;
    public float height = 50;
    public float height_normalization = 0.5f;



    private OrientationEstimater orientationEstimater = new OrientationEstimater();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WekaLoad wekaload = new WekaLoad();


        //接続リセットボタン
        Button resetBtn = (Button) findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                restart();
            }
        });

        //一定間隔で経過時間をサーバーに送信するボタン
        Button outBtn = (Button) findViewById(R.id.outBtn);
        outBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothTask.sendflag == 0)
                    bluetoothTask.sendflag = 1;
                else if(bluetoothTask.sendflag == 1)
                    bluetoothTask.sendflag = 0;

                starttime = System.currentTimeMillis();


            }
        });

        final Handler handler = new Handler();
        handler.post(new Runnable() { /*UI部分の操作のためにスレッドを送る*/
            @Override
            public void run() {/*UI部分の操作*/
                if(bluetoothTask.sendflag == 1) {
                    bluetoothTask.timesend(starttime, height_normalization, attacktiming_control_on, attacktiming_control_off);
                    /*出力切り替え値の処理:現状維持値に戻る*/
                    if(attacktiming_control_on == 1){
                        attacktiming_control_on = 0;
                    } else if(attacktiming_control_off == 1){
                        attacktiming_control_off = 0;
                    }
                }

                handler.postDelayed(this, 100);
            }
        });
        final Handler handler2 = new Handler();
        handler2.post(new Runnable() { /*UI部分の操作のためにスレッドを送る*/
            @Override
            public void run() {/*UI部分の操作*/
                wekaload.tracking(orientationEstimater.accVec.values[1],orientationEstimater.vVec.values[1],orientationEstimater.v2,orientationEstimater.posVec.values[1],orientationEstimater.Gy);

                if(wekaload.Position_y == 0){
                    height += 2;
                } else if(wekaload.Position_y == 1){
                    height += 1;
                } else if(wekaload.Position_y == 3){
                    height += -1;
                } else if(wekaload.Position_y == 4){
                    height += -2;
                }
                if(height > 100)
                    height = 100;
                else if(height < 0)
                    height = 0;

                height_normalization = height / 100;

              ((TextView) findViewById(R.id.heightText)).setText(""+height_normalization);
                handler.postDelayed(this, 5);
            }
        });
    }

    /*画面タッチに関する処理*/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*画面タッチ時と離した時でそれぞれ値を切り替え*/
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                attacktiming_control_on = 1;
                attacktiming_control_off = 0;
                break;
            case MotionEvent.ACTION_UP:
                attacktiming_control_off = 1;
                attacktiming_control_on = 0;
                break;
        }
        return true;
    }
    @SuppressWarnings("deprecation") //非推奨APIに関する警告無視
    @Override
    protected void onResume() {
        super.onResume();
        // Bluetooth初期化
        bluetoothTask.init();
        // ペアリング済みデバイスの一覧を表示してユーザに選ばせる。
        showDialog(DEVICES_DIALOG);
    }

    //アプリ終了時に接続をサーバーとの接続を閉じる
    @Override
    protected void onDestroy() {
        bluetoothTask.doClose();
        super.onDestroy();
    }


    //リセットボタンが押された時にactivityを再起動する
    protected void restart() {
        Intent intent = this.getIntent();
        this.finish();
        this.startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor sensorPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Sensor sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager.registerListener(this, sensorAccel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorMag, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorPressure, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onStop() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        orientationEstimater.onSensorEvent(event);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    // 以下、動作に合わせて表示するダイアログ関連
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DEVICES_DIALOG) return createDevicesDialog();
        if (id == ERROR_DIALOG) return createErrorDialog();
        return null;
    }
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == ERROR_DIALOG) {
            ((AlertDialog) dialog).setMessage(errorMessage);
        }
        super.onPrepareDialog(id, dialog);
    }

    public Dialog createDevicesDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Select device");

        // ペアリング済みデバイスをダイアログのリストに設定する。
        Set<BluetoothDevice> pairedDevices = bluetoothTask.getPairedDevices();
        final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
        String[] items = new String[devices.length];
        for (int i=0;i<devices.length;i++) {
            items[i] = devices[i].getName();
        }

        alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 選択されたデバイスを通知する。そのまま接続開始。
                bluetoothTask.doConnect(devices[which]);
            }
        });
        alertDialogBuilder.setCancelable(false);
        return alertDialogBuilder.create();
    }

    @SuppressWarnings("deprecation")
    public void errorDialog(String msg) {
        if (this.isFinishing()) return;
        this.errorMessage = msg;
        this.showDialog(ERROR_DIALOG);
    }
    public Dialog createErrorDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("");
        alertDialogBuilder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        return alertDialogBuilder.create();
    }
//サーバとの接続待ち状態に表示するダイアログ
    public void showWaitDialog(String msg) {
        if (waitDialog == null) {
            waitDialog = new ProgressDialog(this);
        }
        waitDialog.setMessage(msg);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.show();
    }
    public void hideWaitDialog() {
        waitDialog.dismiss();
    }
}