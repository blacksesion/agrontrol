package cl.inspira2.myapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class OpcionesActivity extends AppCompatActivity {

    EditText ID_CAPTURADOR_INPUT;
    EditText CORRELATIVO_INPUT;
    EditText LICENCIA_INPUT;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the print services
     */
    private BluetoothPrintService mPrintService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opciones);
        Licencia licencia = new Licencia(getApplicationContext());

        ID_CAPTURADOR_INPUT = (EditText) findViewById(R.id.editText_idCapturador);
        CORRELATIVO_INPUT = (EditText) findViewById(R.id.editText_correlativo);
        LICENCIA_INPUT = (EditText) findViewById(R.id.editText_licenciaKey);
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
        /**
         * Aca va lo nuevo para bloutooth como servicio
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            //this.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.test_printer: {
                testPrint();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupPrint() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the print session
        } else if (mPrintService == null) {
            setupPrint();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPrintService != null) {
            mPrintService.stop();
        }
    }

    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPrintService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPrintService.getState() == BluetoothPrintService.STATE_NONE) {
                // Start the Bluetooth print services
                mPrintService.start();
            }
        }
    }

    public void onStop() {
        super.onStop();
    }

    /**
     * Set up the UI and background operations for print.
     */
    private void setupPrint() {
        //Log.d(TAG, "setupPrint()");
        // Initialize the BluetoothPrintService to perform bluetooth connections
        mPrintService = new BluetoothPrintService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothPrintService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothPrintService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothPrintService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothPrintService.STATE_LISTEN:
                        case BluetoothPrintService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    Toast.makeText(getApplicationContext(), "Enviado", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    Toast.makeText(getApplicationContext(), "Recibido", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mPrintService.connect(device, secure);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(intent, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(intent, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a print session
                    setupPrint();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    //Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }
    }

    public void saveSettings(View view) {
        try {
            Licencia licencia = new Licencia(getApplicationContext());
            if (licencia.checkLicencia(LICENCIA_INPUT.getText().toString())) {
                Double correlativo_double = Double.parseDouble(CORRELATIVO_INPUT.getText().toString());
                Double id_capturador_double = Double.parseDouble(ID_CAPTURADOR_INPUT.getText().toString());

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.options_id_capturador), id_capturador_double.toString());
                editor.putString(getString(R.string.options_correlativo), correlativo_double.toString());
                editor.putString(getString(R.string.options_licencia), LICENCIA_INPUT.getText().toString());
                editor.commit();
                Toast.makeText(this, "Datos Guardados", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Licencia Invalida", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al Guardar:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

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
                    new String[]{myFile.getAbsolutePath()}, // "file" was created with "new File(...)"
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

    /**
     * this will send text data to be printed by the bluetooth printer
     */
    private void testPrint() {
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

                    //mmOutputStream.write(senddata);

                    // envio los datos
                    mPrintService.write(senddata);
                    // Reset out string buffer to zero and clear the edit text field
                    mOutStringBuffer.setLength(0);

                } while (cosecheros.moveToNext());
            }
            cosecheros.close();

            // tell the user data were sent
            Toast.makeText(this, "Prueba Enviada.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
