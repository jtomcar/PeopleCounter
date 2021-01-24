package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


//*****************************************************************************************

public class MainActivity extends AppCompatActivity {

    public static Boolean bluetoothActive = false;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    Button connectButton;
    Button disconnectButton;
    Button updateButton;
    Button UPMaximumButton;
    Button DOWNMaximumButton;
    TextView statusLabel;

    BluetoothSocket btSocket;
    InputStream inputStream;
    OutputStream outputStream;
//--------------------------------------------------
    TextView statusLabel2;
    TextView statusLabel3;
    int maximo=10;
     static int contador;
//---------------------------------------------------

    NetworkExecutor networkExecutor;

    public static String indexhtml;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = (TextView) findViewById(R.id.textView);
        statusLabel2 = (TextView) findViewById(R.id.textView2);
        statusLabel3 = (TextView) findViewById(R.id.textView3);
        connectButton = (Button) findViewById(R.id.connectButton);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        updateButton = (Button) findViewById(R.id.updateButton);
        UPMaximumButton= (Button) findViewById(R.id.UPMaximumButton);
        DOWNMaximumButton= (Button) findViewById(R.id.DOWNMaximumButton);


        this.indexhtml = readResourceTextFile();

        networkExecutor = new NetworkExecutor();
        networkExecutor.start();

    }


    public void onClickupdateButton(View view){
        update();
    }
    public void onClickUPMaximumButton(View view){
        up();
    }
    public void onClickDOWNMaximumButton(View view){
        down();
    }

    public void onClickConnectButton(View view){

        if (bluetooth.isEnabled()){
            String address = bluetooth.getAddress();
            String name = bluetooth.getName();
            //Mostramos la datos en pantalla (The information is shown in the screen)
            Toast.makeText(getApplicationContext(),"Bluetooth ACTIVADO:"+name+":"+address,
                    Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth No disponible",
                    Toast.LENGTH_SHORT).show();
        }
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        bluetoothActive = true;
        Toast.makeText(getApplicationContext(),
                "Bluetooth activado " + bluetoothActive,
                Toast.LENGTH_SHORT).show();
        startDiscovery();
        statusLabel.setText("Conectando...");
    }
    public void onClickDissconnectedButton(View view){
        if(bluetoothActive){
            bluetooth.disable();
        }
        statusLabel.setText("No se puede conectar");
    }

    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Guardamos el nombre del dispositivo descubierto
            String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            //Guardamos el objeto Java del dispositivo descubierto, para poderconectar.
            BluetoothDevice remoteDevice =  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //Leemos la intensidad de la radio con respecto a este dispositivo bluetooth
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
            //Guardamos el dispositivo encontrado en la lista
            deviceList.add(remoteDevice);

            try {
                if (remoteDevice != null && remoteDeviceName.equals("JonatanBT")) {
                    //   Log.d("onReceive", "Discovered SUM_SCH3:connecting");
                    statusLabel.setText("Conectado a PeopleCounter");
                    connect(remoteDevice);
                }
            } catch (Exception ex) {
                Log.d("MyFirstApp", "Recogida excepción");

            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) //Bluetooth permission request code
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "User Enabled Bluetooth",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "User Did not enable Bluetooth",
                        Toast.LENGTH_SHORT).show();
            }
    }
    private void startDiscovery(){
        if (bluetoothActive){
            //Borramos la lista de dispositivos anterior
            deviceList.clear();
            //Activamos un Intent Android que avise cuando se encuentre un dispositivo
            //NOTA: <<discoveryResult>> es una clase <<callback>> que describiremos en el siguiente paso
            registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            //Ponemos el adaptador bluetooth en modo <<Discovery>>
            checkBTPermissions();
            bluetooth.startDiscovery();
        }
    }

    public void checkBTPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new
                                String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION},
                                1001);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + ContextCompat.checkSelfPermission(getBaseContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION));
            }
        }
    }

    @SuppressLint("SetTextI18n")
    protected void connect(BluetoothDevice device) {

        try {
            btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            Log.d("connect", "Conectado a PeopleCounter");
            statusLabel.setText("Conectado a PeopleCounter");
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
        }catch (Exception e) {
            Log.e("ERROR: connect", ">>", e);
        }

    }

    // Movimiento del robot
    public void update() {
        try {
            String tmpStr = "0";
            byte[] bytes = tmpStr.getBytes();
            byte[] buffer = new byte[256];
            int bytes2;
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            bytes2 = btSocket.getInputStream().read(buffer);
            String readMessage = new String(buffer, 0, bytes2);

            String leer = readMessage.replaceAll("\\s","");
            int leerInt = Integer.parseInt(leer);
            this.contador=leerInt;
            //contador();

            int compMax = maximo;
            if(leerInt==compMax) {
                statusLabel3.setText("Aforo máximo alcanzado");
            }

                statusLabel.setText("Aforo actual: " + readMessage);


        } catch (Exception e) {
            Log.e("update", "ERROR:" + e);
        }
    }

    public void up() {
        try {
            //--------------
            maximo+=1;
            statusLabel2.setText("Máximo: " + maximo);
            //-------------
            String tmpStr = maximo+"";
            byte[] bytes = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("up", "ERROR:" + e);
        }
    }

    public void down() {
        try {
            //------------------
            maximo-=1;
            statusLabel2.setText("Máximo: " + maximo);
            //-----------------

            String tmpStr = maximo+"";
            //String tmpStr = "R";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("down", "ERROR:" + e);
        }
    }

    public String readResourceTextFile() {
        String fileStr = "";
        InputStream is = getResources().openRawResource(R.raw.index);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine = null;

        try {
            while ((readLine = br.readLine()) != null) {
                fileStr = fileStr + readLine + "\r\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileStr;
    }

}