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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.wifi.aware.DiscoverySession;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;

//*****************************************************************************************

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_REQUEST_CAMERA = 1;

    //private static final UUID UUID = ;
    public static Boolean bluetoothActive = false;
    private static File mediaStorageDir;
    private static File mediaFile;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    Button connectButton;
    Button disconnectButton;
    Button forwardButton;
    Button backwardButton;
    Button turnLeftForwardButton;
    Button turnRightForwardButton;
    TextView statusLabel;

    BluetoothSocket btSocket;
    InputStream inputStream;
    OutputStream outputStream;
    Button stopButton;
//--------------------------------------------------
    TextView statusLabel2;
    int maximo=10;
//---------------------------------------------------
    Camera mCamera;
    private static Handler handlerNetworkExecutorResult;

    FrameLayout cameraPreviewFrameLayout;
    CameraPreview mCameraPreview;

    NetworkExecutor networkExecutor;

    public static String indexhtml;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = (TextView) findViewById(R.id.textView);
        statusLabel2 = (TextView) findViewById(R.id.textView2);
        connectButton = (Button) findViewById(R.id.connectButton);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        forwardButton = (Button) findViewById(R.id.forButton);
        turnLeftForwardButton= (Button) findViewById(R.id.leftButton);
        turnRightForwardButton= (Button) findViewById(R.id.rightButton);
        //cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraView);

//        mCamera = getCameraInstance();
//        if(mCamera == null ){
//            Log.d("CAMARA", "Camara es null");
//            System.exit(-1);
//        }
//        mCameraPreview = new CameraPreview(this, mCamera);
//        cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraView);
//        cameraPreviewFrameLayout.addView(mCameraPreview);

        handlerNetworkExecutorResult = new Handler() {
            public void handleMessage(Message  msg) {
            Log.d("handlerNetworkExecutor", (String) msg.obj);
            if (msg != null) {
                if (msg.obj.equals("FORWARD")) {
                    forward();
                } else if (msg.obj.equals("BACKWARD")) {
                    backward();
                } else if (msg.obj.equals("LEFT")) {
                    left();
                } else if (msg.obj.equals("RIGHT")) {
                    right();
                } else if (msg.obj.equals("CAMERA")) {
                    captureCamera();
                }
            }
            }
        };

        this.indexhtml = readResourceTextFile();

        networkExecutor = new NetworkExecutor();
        networkExecutor.start();

    }

    private android.hardware.Camera getCameraInstance() {
        android.hardware.Camera camera = null;
        try {
            if (!haveCameraPermission())
                Log.d("PERMISOS CAMARA", " NO TIENE PERMISOS");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
            camera = android.hardware.Camera.open(0);
            System.out.println("CAMARA -> ");
            return camera;
        } catch (Exception e) {
            // cannot get camera or does not exist
            Log.d("getCameraInstance", "ERROR" + e);
        }
        return camera;
    }

    private boolean haveCameraPermission()
    {
        if (Build.VERSION.SDK_INT < 23)
            return true;
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void onClickForwardButton(View view){
        forward();
    }
    public void onClickStopButton(View view){
        stop();
    }
    public void onClickTurnLeftForwardButton(View view){left();    }
    public void onClickTurnRightForwardButton(View view){
        right();
    }

    public void onClickConnectButton(View view){

        if (bluetooth.isEnabled()){
            String address = bluetooth.getAddress();
            String name = bluetooth.getName();
            //Mostramos la datos en pantalla (The information is shown in the screen)
            Toast.makeText(getApplicationContext(),"Bluetooth ENABLED:"+name+":"+address,
                    Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth NOT enabled",
                    Toast.LENGTH_SHORT).show();
        }
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        bluetoothActive = true;
        Toast.makeText(getApplicationContext(),
                "Bluetooth active " + bluetoothActive,
                Toast.LENGTH_SHORT).show();
        startDiscovery();
        statusLabel.setText("Connect pressed");
    }
    public void onClickDissconnectedButton(View view){
        if(bluetoothActive){
            bluetooth.disable();
        }
        statusLabel.setText("Connect down");
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

//            statusLabel.setText("Discovered "+ remoteDeviceName+"\nRSSI "+ rssi + "dBm");
//            //Mostramos el evento en el Log
//            Log.d("MyFirstApp", "Discovered "+ remoteDeviceName);
//            Log.d("MyFirstApp", "RSSI "+ rssi + "dBm");

            try {
                if (remoteDevice != null && remoteDeviceName.equals("JonatanBT")) {
                    //   Log.d("onReceive", "Discovered SUM_SCH3:connecting");
                    statusLabel.setText("Conectado a PeopleCounter");
                    connect(remoteDevice);
                }
            } catch (Exception ex) {
                Log.d("MyFirstApp", "Recogida excepción");

            }

/*            remoteDevice = bluetooth.getRemoteDevice("4c:74:03:c8:70:da");
            connect(remoteDevice); */
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

       /* device = bluetooth.getRemoteDevice("30:85:A9:51:1A:A6");
        connect(device);*/
    }


    // Movimiento del robot
    public void forward() {
        try {
            String tmpStr = "0";
            byte[] bytes = tmpStr.getBytes();
            byte[] buffer = new byte[256];
            int bytes2;
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            bytes2 = btSocket.getInputStream().read(buffer);
            String readMessage = new String(buffer, 0, bytes2);

           // String readMessageCopia=readMessage;

            //int readMessageEntero=parseInt(readMessageCopia);
            //System.out.println(readMessageCopia);
           // System.out.println(maximo+"");

           // if(readMessageCopia.equals(maximo+"")){
            //    statusLabel.setText("Aforo maximo alcanzado");
            //}else{
                statusLabel.setText("Aforo actual: " + readMessage);
           // }


        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }

    public void left() {
        try {
            //--------------
            maximo+=1;
            statusLabel2.setText("Máximo: " + maximo);
            //-------------
            String tmpStr = maximo+"";
            //byte[] bytes = tmpStr.getBytes();
            //String tmpStr = "L";
            byte[] bytes = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("left", "ERROR:" + e);
        }
    }

    public void right() {
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
            Log.e("right", "ERROR:" + e);
        }
    }

    public void stop() {
        try {
            String tmpStr = "S";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("stop", "ERROR:" + e);
        }
    }
    private void backward() {
        try {
            String tmpStr = "B";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("stop", "ERROR:" + e);
        }

    }

    public static File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCameraApp");
           // mediaStorageDir = new File(Environment.getExternalStorageDirectory().toString(),"MyCameraApp");

            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
        }

        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        return mediaFile;
    }

    public void captureCamera(){

        if (mCamera!=null) {
            try {
                mCamera.takePicture(null, null, mPicture);
            }catch (Exception ex){
                System.out.println("EL ERROR "+ex.toString());
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            byte[] resized = resizeImage(data);

            mCamera.startPreview();

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(resized);
                fos.close();
            } catch (Exception e) {
                Log.e("onPictureTaken", "ERROR:" + e);
            }
        }
    };

    byte[] resizeImage(byte[] input) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 107,true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
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

    public static void showDisplayMessage(String displayMessage) {
        Message msg = new Message();
        msg.arg1 = 0;
        msg.obj = displayMessage.replaceAll("_", " ");
        handlerNetworkExecutorResult.sendMessage(msg);
    }

}