package com.example.korona.bluetoothserver;

/**
 * Created by korona on 2017/04/11.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    public Session connect() throws IOException {
        printdata("Accept");
        StreamConnection channel = server.acceptAndOpen();//クライアントがくるまでここで待機
        printdata("Connect");
        return new Session(channel);
    }

    /*
     接続したクライアントとの間にIn,Outのストリームを構築
     */
    static class Session implements Runnable {
        private StreamConnection channel = null;
        private InputStream btIn = null;
        private OutputStream btOut = null;

        public Session(StreamConnection channel) throws IOException {
            this.channel = channel;
            this.btIn = channel.openInputStream();
            this.btOut = channel.openOutputStream();
        }

        /**
         * - 入力が空なら終了。
         */
        public void run() {
            try {
                byte[] buff = new byte[512];
                int n = 0;
                while ((n = btIn.read(buff)) > 0) {
                    String data = new String(buff, 0, n);
                    printdata("Receive:"+data);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                close();
            }
        }
        //接続が切れた場合ストリームを閉じる
        public void close() {
            printdata("Session Close");
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

    //受信した文字列を表示
    private static void printdata(String msg) {
        System.out.println(msg);
    }

    //メイン部分：クライアントからの接続を待って生成したスレッドでデータを受信
    public static void main(String[] args) throws Exception {
        RfcommServer server = new RfcommServer();
        while (true) {
            Session session = server.connect();
            new Thread(session).start();
        }
    }
}
