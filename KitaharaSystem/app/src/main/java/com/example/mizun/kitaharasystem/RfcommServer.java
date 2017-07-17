package com.example.mizun.kitaharasystem;

/**
 * Created by korona on 2017/04/11.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

/**
 * クライアントからの文字列を受信するBluetooth サーバ。
 */
public class RfcommServer {
    /**
     * スマホ側との通信部分(見る必要なし)
     UUIDは乱数生成したものを使用
     */
    static final String serverUUID = "17fcf242f86d4e35805e546ee3040b84";

    private StreamConnectionNotifier server = null;


    public RfcommServer() throws IOException {
        // RFCOMMベースのサーバの開始。
        // - btspp:は PRCOMM 用なのでベースプロトコルによって変わる。
        server = (StreamConnectionNotifier) Connector.open(
                "btspp://localhost:" + serverUUID,
                Connector.READ_WRITE, true//読み書きモードでサーバースタート
        );
        // ローカルデバイスにサービスを登録。
        ServiceRecord record = LocalDevice.getLocalDevice().getRecord(server);
        LocalDevice.getLocalDevice().updateRecord(record);
    }

    /*
      クライアントからの接続待ち。
     @return 接続されたたセッションを返す。*/
    public Session connect(List time, List position, List attacktiming_switch) throws IOException {
        System.out.println("Accept");
        StreamConnection channel = server.acceptAndOpen();//クライアントがくるまでここで待機
        System.out.println("Connect");
        return new Session(channel,time,position,attacktiming_switch);
    }

    /*
     接続したクライアントとの間にIn,Outのストリームを構築
     */
    static class Session implements Runnable {
        private StreamConnection channel = null;
        private InputStream btIn = null;
        private OutputStream btOut = null;

        private List<Long> time;
        private List<Float> position;
        private List<Integer> attacktiming_switch;


        public Session(StreamConnection channel, List<Long> time, List<Float> position, List<Integer> attacktiming_switch) throws IOException {
            this.channel = channel;
            this.btIn = channel.openInputStream();
            this.btOut = channel.openOutputStream();
            this.time = time;
            this.position = position;
            this.attacktiming_switch = attacktiming_switch;
        }

        /**
         * - 受信したデータ処理を実行するスレッド
         */
        public void run() {
            try {
                byte[] buff = new byte[2048];
                int n = 0;
                String[] element;

                /*約0.5秒間隔で受信データを処理*/
                while (true) {
                    n = btIn.read(buff);
                    /*
                     **受信データの分割セクション
                     */

                    if(n != 0) {
                        String data = new String(buff, 0, n);
                        //1レコード分ごとに分割
                        String[] samples = data.split("\r\n", 0);
                        //各レコードを要素ごとに分割
                        for (int i = 0; i < samples.length; i++) {
                            element = samples[i].split(",", 0);
                            synchronized(time) {
                                time.add(Long.parseLong(element[0]));
                            }
                            synchronized(position) {
                                position.add(Float.parseFloat(element[1]));
                            }
                            synchronized(attacktiming_switch){
                                attacktiming_switch.add(Integer.parseInt(element[2]));
                            }
                        }


                    }
                    try{
                        Thread.sleep(500);
                    }catch (InterruptedException e){
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                close();
            }
        }

        //接続が切れた場合ストリームを閉じる
        public void close() {
            System.out.println("Session Close");
            if (btIn    != null)
                try {
                    btIn.close();
            } catch (Exception e) {}
            if (btOut   != null)
                try {
                    btOut.close();
                } catch (Exception e) {}
            if (channel != null)
                try {
                    channel.close();
                } catch (Exception e) {}
        }
    }


    //メインメソッド
    public static void main(String[] args) throws Exception {
        /*
        *プログラム開始時にクライアント(スマホ)との接続を確立する処理
         */
        List<Long> time = Collections.synchronizedList(new LinkedList<Long>());
        List<Float> position = Collections.synchronizedList(new LinkedList<Float>());
        List<Integer> attacktiming_switch = Collections.synchronizedList(new LinkedList<Integer>());
        RfcommServer server = new RfcommServer();
            //相手端末との接続
        Session session = server.connect(time,position,attacktiming_switch);
            //クライアントからのデータを受け付けるスレッドを生成
        Thread thread = new Thread(session);
        thread.start();
        /*
         *ここからプログラムのメイン部分：通信で入手したデータを利用する処理を加える
         * とりあえずサンプルコードなので受信したデータをそれぞれに出力する(1秒間隔)
        */
        while(true){
                synchronized(time) {
                    for (int i = 0; i < session.time.size(); i++) {
                        System.out.println("time=" + time.get(i) + ",position=" + position.get(i)+",attacktiming_switch="+attacktiming_switch.get(i));

                    }
                    time.clear();
                    position.clear();
                    attacktiming_switch.clear();
                }
            try{
                Thread.sleep(1000); //1秒Sleepする
            }catch(InterruptedException e){}
        }





    }
}