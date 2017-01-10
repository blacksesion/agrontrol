package cl.inspira2.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.londatiga.android.bluebamboo.pockdata.PocketPos;
import net.londatiga.android.bluebamboo.util.DataConstants;
import net.londatiga.android.bluebamboo.util.DateUtil;
import net.londatiga.android.bluebamboo.util.FontDefine;
import net.londatiga.android.bluebamboo.util.Printer;
import net.londatiga.android.bluebamboo.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class OpcionesActivity extends AppCompatActivity {


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

    EditText ID_CAPTURADOR_INPUT;
    EditText CORRELATIVO_INPUT;
    EditText LICENCIA_INPUT;
    Spinner SELECT_PRINT;
    Button CONECT_PRINT;
    Button TEST_PRINT;

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opciones);
        Licencia licencia = new Licencia(getApplicationContext());

        ID_CAPTURADOR_INPUT = (EditText) findViewById(R.id.editText_idCapturador);
        CORRELATIVO_INPUT = (EditText) findViewById(R.id.editText_correlativo);
        LICENCIA_INPUT = (EditText) findViewById(R.id.editText_licenciaKey);
        SELECT_PRINT = (Spinner) findViewById(R.id.spinner_selectPrint);
        CONECT_PRINT = (Button) findViewById(R.id.button_conectPrint);
        TEST_PRINT = (Button) findViewById(R.id.button_testPrint);
        TextView deviceId_text = (TextView) findViewById(R.id.device_id_label);
        deviceId_text.setText(licencia.getDeviceId());

        //SharedPreferences sharedPref = this.getPreferences(this.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (sharedPref.contains(getString(R.string.options_correlativo))) {
            CORRELATIVO_INPUT.setText(sharedPref.getString(getString(R.string.options_correlativo), "1.0"));
        }
        if (sharedPref.contains(getString(R.string.options_id_capturador))) {
            ID_CAPTURADOR_INPUT.setText(sharedPref.getString(getString(R.string.options_id_capturador), "0"));
        }
        if (sharedPref.contains(getString(R.string.options_licencia))) {
            LICENCIA_INPUT.setText(sharedPref.getString(getString(R.string.options_licencia), ""));
        }

        try {
            BA = BluetoothAdapter.getDefaultAdapter();

            if (BA != null) {
                if (!BA.isEnabled()) {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, 100);
                }
                pairedDevices = BA.getBondedDevices();

                if (pairedDevices != null) {
                    mDeviceList.addAll(pairedDevices);
                    updateDeviceList();

                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth device not found.", Toast.LENGTH_LONG).show();
                    SELECT_PRINT.setEnabled(false);
                    TEST_PRINT.setEnabled(false);
                    CONECT_PRINT.setEnabled(false);
                }
            } else {
                Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
                SELECT_PRINT.setEnabled(false);
                TEST_PRINT.setEnabled(false);
                CONECT_PRINT.setEnabled(false);
            }
            updateDeviceList();
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (sharedPref.contains(getString(R.string.options_licencia))) {
            if (!licencia.checkLicencia(sharedPref.getString(getString(R.string.options_licencia), ""))) {
                Toast.makeText(getApplicationContext(), "Falla en la validacion de clave", Toast.LENGTH_LONG).show();
                LICENCIA_INPUT.requestFocus();
                LICENCIA_INPUT.selectAll();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No ha ingresado activacion", Toast.LENGTH_LONG).show();
            LICENCIA_INPUT.requestFocus();
            LICENCIA_INPUT.selectAll();
        }
    }

    public void udpateSelectPrint(View view) {
        try {
            BA = BluetoothAdapter.getDefaultAdapter();

            if (BA != null) {
                if (!BA.isEnabled()) {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, 1);
                }
                pairedDevices = BA.getBondedDevices();

                if (pairedDevices != null) {
                    mDeviceList.addAll(pairedDevices);
                    updateDeviceList();
                    Toast.makeText(getApplicationContext(), "Lista Actualizada", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth device not found.", Toast.LENGTH_LONG).show();
                    SELECT_PRINT.setEnabled(false);
                    TEST_PRINT.setEnabled(false);
                    CONECT_PRINT.setEnabled(false);
                }
            } else {
                Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
                SELECT_PRINT.setEnabled(false);
                TEST_PRINT.setEnabled(false);
                CONECT_PRINT.setEnabled(false);
            }
            updateDeviceList();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ocurrio un error al actualizar lista", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDeviceList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getArray(mDeviceList));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SELECT_PRINT.setAdapter(adapter);
        SELECT_PRINT.setSelection(0);
    }

    private String[] getArray(ArrayList<BluetoothDevice> data) {
        String[] list = new String[0];

        if (data == null) return list;

        int size = data.size();
        list = new String[size];

        for (int i = 0; i < size; i++) {
            list[i] = data.get(i).getName() + " - " + data.get(i).getAddress();
        }

        return list;
    }

    public void saveSettings(View view) {
        try {
            Licencia licencia = new Licencia(getApplicationContext());
            if (licencia.checkLicencia(LICENCIA_INPUT.getText().toString())) {
                String selectPrinter = "";
                if (SELECT_PRINT.getSelectedItem() != null) {
                    String[] select = SELECT_PRINT.getSelectedItem().toString().split("-");
                    selectPrinter = select[1].trim();
                }
                Double correlativo_double = Double.parseDouble(CORRELATIVO_INPUT.getText().toString());
                Double id_capturador_double = Double.parseDouble(ID_CAPTURADOR_INPUT.getText().toString());

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.options_id_capturador), id_capturador_double.toString());
                editor.putString(getString(R.string.options_correlativo), correlativo_double.toString());
                editor.putString(getString(R.string.options_licencia), LICENCIA_INPUT.getText().toString());
                editor.putString(getString(R.string.options_printer), selectPrinter);
                editor.commit();
                Toast.makeText(this, "Datos Guardados", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Licencia Invalida", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al Guardar:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    public void conectPrint(View view) throws IOException {
        String selectPrinter = "";
        if (SELECT_PRINT.getSelectedItem() != null) {
            String[] select = SELECT_PRINT.getSelectedItem().toString().split("-");
            selectPrinter = select[1].trim();
        }
        if (!selectPrinter.equals("")) {
            // tries to open a connection to the bluetooth printer device
            try {
                String[] select = SELECT_PRINT.getSelectedItem().toString().split("-");


                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {

                        if (device.getAddress().equals(select[1].trim())) {
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

                Toast.makeText(this, "Bluetooth Opened", Toast.LENGTH_SHORT).show();
                //myLabel.setText("Bluetooth Opened");

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ocurrio un error al conectar con la impresora", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * this will send text data to be printed by the bluetooth printer
     */
    public void testPrint(View view) throws IOException {

        if (mmOutputStream != null) {
            try {

                DatabaseOperations DOP = new DatabaseOperations(this);

                Cursor cosecheros = DOP.getCosecherosLimit(3);

                if (cosecheros.moveToFirst()) {
                    do {
                        String receiptHead = "\n"
                                + "************************"
                                + "\n"
                                + "Agrontrol" + "\n"
                                + "************************"
                                + "\n";

                        long milis = System.currentTimeMillis();

                        String date = DateUtil.timeMilisToString(milis, "MMM dd, yyyy");
                        String time = DateUtil.timeMilisToString(milis, "hh:mm a");

                        String rut = cosecheros.getString(cosecheros.getColumnIndex(CosecherosContract.CosecherosEntry.RUT));
                        String name = cosecheros.getString(cosecheros.getColumnIndex(CosecherosContract.CosecherosEntry.NOMBRE));
                        String cc = cosecheros.getString(cosecheros.getColumnIndex(CosecherosContract.CosecherosEntry.CENTRO_COSTO));
                        String cod_c = cosecheros.getString(cosecheros.getColumnIndex(CosecherosContract.CosecherosEntry.COD_TRABAJADOR));
                        String cod_cz = cosecheros.getString(cosecheros.getColumnIndex(CosecherosContract.CosecherosEntry.COD_CAPATAZ));

                        StringBuffer contentBuffer = new StringBuffer(100);

                        contentBuffer.append(Util.nameLeftValueRightJustify(date, time, DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Nombre:", name, DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Rut:", rut, DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Cod Cosechero", cod_c, DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Centro Costo:", cc, DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Cod Capataz:", cod_cz, DataConstants.RECEIPT_WIDTH) + "\n");
                        String receiptContent = contentBuffer.toString();

                        //2D Bar Code
                        //byte[] formats = {(byte) 0x1d, (byte) 0x6b, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1f};
                        //1D Bar Code
                        byte[] formats = {(byte) 0x1d, (byte) 0x6b, (byte) 0x02, (byte) 0x0d};
                        byte[] contents = rut.getBytes();
                        byte[] barcode = new byte[formats.length + contents.length];
                        System.arraycopy(formats, 0, barcode, 0, formats.length);
                        System.arraycopy(contents, 0, barcode, formats.length, contents.length);

                        /*********** print Tail*******/
                        String receiptTail = "\n" + "Test Completed" + "\n"
                                + "************************" + "\n";

                        String receiptWeb = "** www.inspira2.cl ** " + "\n\n\n";

                        byte[] header = Printer.printfont(receiptHead + "\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN1);
                        byte[] content = Printer.printfont(receiptContent + "\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN1);
                        byte[] foot = Printer.printfont(receiptTail, FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN1);
                        byte[] web = Printer.printfont(receiptWeb, FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN1);

                        byte[] totaldata = new byte[header.length + content.length + barcode.length + foot.length + web.length];
                        int offset = 0;
                        System.arraycopy(header, 0, totaldata, offset, header.length);
                        offset += header.length;

                        System.arraycopy(content, 0, totaldata, offset, content.length);
                        offset += content.length;

                        System.arraycopy(barcode, 0, totaldata, offset, barcode.length);
                        offset += barcode.length;

                        System.arraycopy(foot, 0, totaldata, offset, foot.length);
                        offset += foot.length;

                        System.arraycopy(web, 0, totaldata, offset, web.length);

                        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totaldata, 0, totaldata.length);

                        mmOutputStream.write(senddata);

                    } while (cosecheros.moveToNext());
                }
                cosecheros.close();

                // tell the user data were sent
                Toast.makeText(this, "Data sent.", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show();
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

    public void createExportFile(View view) {
        try {
            DatabaseOperations DOP = new DatabaseOperations(this);
            JSONArray capturas = DOP.getJsonCapturas();
            writeToFile(capturas);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void writeToFile(JSONArray data) {
        try {
            File myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "agrontrol-import.json");
            if (myFile.exists()) {
                myFile.delete();
            }
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data.toString());
            myOutWriter.close();

            fOut.close();
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{ myFile.getAbsolutePath() }, // "file" was created with "new File(...)"
                    null,
                    null);
            Toast.makeText(getApplicationContext(), "Done writing SD 'capture_export.json'", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Nullable
    private JSONObject readFromFile() {
        try {

            File myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "agrontrol.json");
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow = "";
            String aBuffer = "";
            while ((aDataRow = myReader.readLine()) != null) {
                aBuffer += aDataRow;
            }
            myReader.close();
            JSONObject data = new JSONObject(aBuffer);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteCapturas(View view) {
        DatabaseOperations DOP = new DatabaseOperations(this);
        if (DOP.clearCapturas()) {
            Toast.makeText(getApplicationContext(), "Capturas Eliminadas", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Fallo el eliminar", Toast.LENGTH_SHORT).show();
        }
    }

    public void importData(View view) {
        try {
            DatabaseOperations DOP = new DatabaseOperations(this);
            JSONObject data = readFromFile();
            if (data != null) {
                DOP.importData(data);
            } else {
                Toast.makeText(getApplicationContext(), "No se encuentra Archivo o Formato incorrecto", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
