# BluetoothConnect
PCとAndroid端末間でBluetooth通信を行うシステム  
PCをサーバ，Android端末をクライアントとしてBluetoothを用いて入出力ストリームを構築してデータの送受信を行う  

1.PC側で実行するプログラム(/app/src/main/java/com/example/korona/bluetoothserver/RfcommServer.java)  
  機能  
  ・Javaプログラム実行時にサーバを作成し，クライアント(Android端末)からの接続を待つ  
　・接続確率時に入出力ストリームを確立してクライアントから送信データを受信する  
  補足  
　・bluecove-2.1.1-SNAPSHOT.jarをライブラリとして使用  

2.Android側で実行するプログラム(/app/src/main/java/com/example/korona/pctoandroidconnect/MainActivity.java,BluetoothTask.java)  
  機能  
　・アプリ起動時にペアリングしている機器の一覧を表示し，選択した機器とBluetooth接続  
　・アプリ起動からの経過時間をサーバに送信(送信実験用なんで用途に合わせて仕様変更してください)  


