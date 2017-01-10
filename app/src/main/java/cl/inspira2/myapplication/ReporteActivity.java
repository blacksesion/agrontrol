package cl.inspira2.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import net.londatiga.android.bluebamboo.pockdata.PocketPos;
import net.londatiga.android.bluebamboo.util.DataConstants;
import net.londatiga.android.bluebamboo.util.DateUtil;
import net.londatiga.android.bluebamboo.util.FontDefine;
import net.londatiga.android.bluebamboo.util.Printer;
import net.londatiga.android.bluebamboo.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ReporteActivity extends AppCompatActivity {
    EditText CORRELATIVO_INICIAL;
    EditText CORRELATIVO_FINAL;
    TableLayout GRID_LAYOUT;
    //para la impresion
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    BluetoothDevice mmDevice;
    BluetoothSocket mmSocket;
    // needed for communication to bluetooth device / network
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    String printAddres = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        CORRELATIVO_INICIAL = (EditText) findViewById(R.id.editText_correlativoInicio);
        CORRELATIVO_FINAL = (EditText) findViewById(R.id.editText_correlativoFin);
        GRID_LAYOUT = (TableLayout) findViewById(R.id.grid);

        DatabaseOperations DOP = new DatabaseOperations(this);
        Cursor min_max = DOP.getReportMinMax();
        if (min_max.moveToFirst()) {
            CORRELATIVO_INICIAL.setText(min_max.getString(min_max.getColumnIndex("min")).replace(",",""));
            CORRELATIVO_FINAL.setText(min_max.getString(min_max.getColumnIndex("max")).replace(",",""));
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //para la impresion
        if (sharedPref.contains(getString(R.string.options_correlativo))) {
            printAddres = sharedPref.getString(getString(R.string.options_printer), "");
        }
        if (!printAddres.equals("")) {
            try {
                BA = BluetoothAdapter.getDefaultAdapter();

                if (BA != null) {
                    if (!BA.isEnabled()) {
                        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBluetooth, 100);
                    }
                    pairedDevices = BA.getBondedDevices();

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
                            Toast.makeText(getApplicationContext(), "Bluetooth device not found.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ocurrio un error al conectar con la impresora", Toast.LENGTH_SHORT).show();

            }
        }else{
            Toast.makeText(this, "No hay impresora Configurada", Toast.LENGTH_SHORT).show();
        }

    }

    public void onStop() {

        try {
            if (mmInputStream != null) {
                mmInputStream.close();
            }
            if (mmOutputStream != null) {
                mmOutputStream.close();
            }
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    public void getReporte(View view) {
        DatabaseOperations DOP = new DatabaseOperations(this);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        String text1 = nf.format(Double.valueOf(CORRELATIVO_INICIAL.getText().toString()));
        String text2 = nf.format(Double.valueOf(CORRELATIVO_FINAL.getText().toString()));
        //Cursor reporte = DOP.getReporte(CORRELATIVO_INICIAL.getText().toString(), CORRELATIVO_FINAL.getText().toString());
        Cursor reporte = DOP.getReporte(text1, text2);
        if (reporte.moveToFirst()) {
            GRID_LAYOUT.removeAllViews();
            TableRow primera = new TableRow(this);
            TextView etiqueta1 = new TextView(this);
            TextView etiqueta2 = new TextView(this);
            TextView etiqueta3 = new TextView(this);
            TextView etiqueta4 = new TextView(this);
            etiqueta1.setText("Cuartel");
            etiqueta2.setText("Unidad");
            etiqueta3.setText("N Cajas");
            etiqueta4.setText("Kg Neto");
            primera.addView(etiqueta1);
            primera.addView(etiqueta2);
            primera.addView(etiqueta3);
            primera.addView(etiqueta4);
            GRID_LAYOUT.addView(primera);
            do {
                TableRow row = new TableRow(this);
                for (int j = 0; j < reporte.getColumnCount(); j++) {
                    TextView actualData = new TextView(this);
                    //set properties
                    actualData.setText(reporte.getString(j));
                    row.addView(actualData);
                }
                GRID_LAYOUT.addView(row);
            } while (reporte.moveToNext());
        } else {
            GRID_LAYOUT.removeAllViews();
            TableRow primera = new TableRow(this);
            TextView etiqueta1 = new TextView(this);
            TextView etiqueta2 = new TextView(this);
            TextView etiqueta3 = new TextView(this);
            TextView etiqueta4 = new TextView(this);
            etiqueta1.setText("Cuartel");
            etiqueta2.setText("Unidad");
            etiqueta3.setText("N Cajas");
            etiqueta4.setText("Kg Neto");
            primera.addView(etiqueta1);
            primera.addView(etiqueta2);
            primera.addView(etiqueta3);
            primera.addView(etiqueta4);
            GRID_LAYOUT.addView(primera);
            Toast.makeText(this, "No existen capturas entre los correlativos ingresados", Toast.LENGTH_SHORT).show();
        }
        reporte.close();
    }

    public void printReport(View view) {
        DatabaseOperations DOP = new DatabaseOperations(this);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        String text1 = nf.format(Double.valueOf(CORRELATIVO_INICIAL.getText().toString()));
        String text2 = nf.format(Double.valueOf(CORRELATIVO_FINAL.getText().toString()));

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPref.contains(getString(R.string.options_correlativo))) {
            printAddres = sharedPref.getString(getString(R.string.options_printer), "");
        }
        if (!printAddres.equals("")) {
            try {
                long milis = System.currentTimeMillis();
                String date = DateUtil.timeMilisToString(milis, "MMM dd, yyyy");
                String time = DateUtil.timeMilisToString(milis, "hh:mm a");
                StringBuffer contentBuffer = new StringBuffer(100);

                contentBuffer.append("\n\nREPORTE PARCIAL\n");
                contentBuffer.append(Util.nameLeftValueRightJustify(date, time, DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("Desde:", CORRELATIVO_INICIAL.getText().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("Hasta:", CORRELATIVO_FINAL.getText().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append("------------------------------\n");
                contentBuffer.append("CUARTEL-UNIDAD-N CAJAS-KG NETO\n");
                contentBuffer.append("------------------------------\n");
                Double totalKilos = 0.0;
                Integer totalCajas = 0;
                Cursor reporte = DOP.getReporte(text1, text2);
                if (reporte.moveToFirst()) {
                    do {
                        contentBuffer.append(reporte.getString(0) + " - " + reporte.getString(1) + " - " + reporte.getString(2) + " - " + reporte.getString(3) + "\n");
                        totalCajas += Integer.parseInt(reporte.getString(2));
                        totalKilos += Double.parseDouble(reporte.getString(3));
                    } while (reporte.moveToNext());
                }else{
                    contentBuffer.append("\n\n- NO HAY CAPTURAS -\n\n");
                }
                reporte.close();
                contentBuffer.append("------------------------------\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("TOTAL CAJAS:", totalCajas.toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append(Util.nameLeftValueRightJustify("TOTAL KILOS:", totalKilos.toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                contentBuffer.append("** Agrontrol **\n");

                String receiptContent = contentBuffer.toString();


                byte[] content = Printer.printfont(receiptContent + "\n\n\n\n\n\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN2);
                byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, content, 0, content.length);

                mmOutputStream.write(senddata);
                GRID_LAYOUT.removeAllViewsInLayout();
                // tell the user data were sent
                Toast.makeText(this, "Reporte impreso.", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ocurrio un error al Imprimir" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay impresora Configurada", Toast.LENGTH_SHORT).show();
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

            workerThread = new Thread(new Runnable() {
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
                                                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
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
