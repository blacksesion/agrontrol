package cl.inspira2.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by blacksesion on 12-01-2017.
 */

public class BluetoothPrinterService {
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;

    // needed for communication to bluetooth device / network
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;

    private byte[] readBuffer;
    private int readBufferPosition;
    private volatile boolean stopWorker;
    private String printAddres = "";

    BluetoothPrinterService() {
    }

    public void write(byte[] buffer) {
        try {
            mmOutputStream.write(buffer);
            // Share the sent message back to the UI Activity
        } catch (IOException e) {
            //Log.e(TAG, "Exception during write", e);
        }
    }

    public boolean conected() {
        return mmSocket != null && mmSocket.isConnected();
    }

    public void connectDevice(String printAddres, Activity activity){
        if (!printAddres.equals("")) {
            try {
                BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();

                if (BA != null) {
                    if (!BA.isEnabled()) {
                        Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.getApplicationContext().startActivity(btIntent);
                    }
                    Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

                    if (pairedDevices != null && mmSocket == null) {
                        //mDeviceList.addAll(pairedDevices);
                        if (pairedDevices.size() > 0) {
                            for (BluetoothDevice device : pairedDevices) {

                                // RPP300 is the name of the bluetooth printer device
                                // we got this name from the list of paired devices
                                if (device.getAddress().equals(printAddres)) {
                                    mmDevice = device;
                                    break;
                                }
                            }
                        }
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                        Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        mmSocket = (BluetoothSocket) m.invoke(mmDevice, 1);
                        BA.cancelDiscovery();

                        mmSocket.connect();
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();
                        beginListenForData();

                    } else {
                        if (mmSocket != null) {
                            mmOutputStream = mmSocket.getOutputStream();
                            mmInputStream = mmSocket.getInputStream();
                        } else {
                            Toast.makeText(activity, "Dispositivo Bluetooth no encontrado.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(activity, "No hay adaptador Bluetooth", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity, "Ocurrio un error al conectar con la impresora", Toast.LENGTH_SHORT).show();

            }
        }else{
            Toast.makeText(activity, "No hay impresora Configurada", Toast.LENGTH_SHORT).show();
        }
    }

    public void connect(Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        //para la impresion
        if (sharedPref.contains(activity.getString(R.string.options_correlativo))) {
            printAddres = sharedPref.getString(activity.getString(R.string.options_printer), "");
        }
        if (!printAddres.equals("")) {
            try {
                BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();

                if (BA != null) {
                    if (!BA.isEnabled()) {
                        Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.getApplicationContext().startActivity(btIntent);
                    }
                    Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

                    if (pairedDevices != null && mmSocket == null) {
                        //mDeviceList.addAll(pairedDevices);
                        if (pairedDevices.size() > 0) {
                            for (BluetoothDevice device : pairedDevices) {

                                // RPP300 is the name of the bluetooth printer device
                                // we got this name from the list of paired devices
                                if (device.getAddress().equals(printAddres)) {
                                    mmDevice = device;
                                    break;
                                }
                            }
                        }
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                        Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        mmSocket = (BluetoothSocket) m.invoke(mmDevice, 1);
                        BA.cancelDiscovery();

                        mmSocket.connect();
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();
                        beginListenForData();

                    } else {
                        if (mmSocket != null) {
                            mmOutputStream = mmSocket.getOutputStream();
                            mmInputStream = mmSocket.getInputStream();
                        } else {
                            Toast.makeText(activity, "Dispositivo Bluetooth no encontrado.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(activity, "No hay adaptador Bluetooth", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity, "Ocurrio un error al conectar con la impresora", Toast.LENGTH_SHORT).show();

            }
        }else{
            Toast.makeText(activity, "No hay impresora Configurada", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * after opening a connection to bluetooth printer device,
     * we have to listen and check if a data were sent to be printed.
     */
    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            Thread workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                //myLabel.setText(data);
                                                //Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });
            workerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
