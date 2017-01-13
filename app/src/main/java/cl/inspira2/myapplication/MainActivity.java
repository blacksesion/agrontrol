package cl.inspira2.myapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.londatiga.android.bluebamboo.pockdata.PocketPos;
import net.londatiga.android.bluebamboo.util.DataConstants;
import net.londatiga.android.bluebamboo.util.DateUtil;
import net.londatiga.android.bluebamboo.util.FontDefine;
import net.londatiga.android.bluebamboo.util.Printer;
import net.londatiga.android.bluebamboo.util.Util;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    /**
     * Llaves para los campos extra de intents
     */
    public final static String EXTRA_RUT_INPUT = "cl.inspira2.agrontrol.rut_cosechero";
    public final static String EXTRA_LABOR = "cl.inspira2.agrontrol.labor";
    public final static String EXTRA_CUARTEL = "cl.inspira2.agrontrol.cuartel";
    public final static String EXTRA_UNIDAD = "cl.inspira2.agrontrol.unidad";
    public final static String EXTRA_TIPO_CAJA = "cl.inspira2.agrontrol.tipo_caja";
    public final static String EXTRA_TIPO_PESAJE = "cl.inspira2.agrontrol.tipo_pesaje";
    private Button scanBtn;

    Cosechero recepcionista;

    EditText RUT_INPUT;
    EditText LABOR;
    Spinner CUARTEL;
    Spinner UNIDAD;
    Spinner TIPO_CAJA;
    RadioGroup TIPO_PESAJE;
    TextView NOMBRE_RECEPCIONISTA;

    String rut_recepcionista = "";
    String cuartel = "";
    String unidad = "";
    String tipo_caja = "";
    String labor = "";
    String pesaje = "";
    Boolean validate_rut = false;

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
        setContentView(R.layout.activity_main);
        // primero checo la licencia
        String llave;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPref.contains(getString(R.string.options_licencia))) {
            llave = sharedPref.getString(getString(R.string.options_licencia), "");
            Licencia licencia = new Licencia(getApplicationContext());
            if (!licencia.checkLicencia(llave)) {
                Toast.makeText(getApplicationContext(), "Falla en la validacion de clave", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OpcionesActivity.class);
                startActivity(intent);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No ha ingresado activacion", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, OpcionesActivity.class);
            startActivity(intent);
        }
        RUT_INPUT = (EditText) findViewById(R.id.editText_rut);
        LABOR = (EditText) findViewById(R.id.editText_labor);
        CUARTEL = (Spinner) findViewById(R.id.spinner_cuartel);
        UNIDAD = (Spinner) findViewById(R.id.spinner_unidad);
        TIPO_PESAJE = (RadioGroup) findViewById(R.id.tipoPesaje_group);
        TIPO_CAJA = (Spinner) findViewById(R.id.spinner_tipo_caja);
        NOMBRE_RECEPCIONISTA = (TextView) findViewById(R.id.nombre_recepcionista);
        //Se Instancia el botón de Scan
        scanBtn = (Button) findViewById(R.id.button_scan2);
        //Se agrega la clase MainActivity.java como Listener del evento click del botón de Scan
        scanBtn.setOnClickListener(this);
        //rut cambio de foco
        RUT_INPUT.setOnFocusChangeListener(this);
        loadSpinnerData();

        SharedPreferences.Editor editor = sharedPref.edit();
        if (!sharedPref.contains(getString(R.string.options_id_capturador))) {
            editor.putString(getString(R.string.options_id_capturador), "1.0");
            editor.apply();
            editor.commit();
        }
        if (!sharedPref.contains(getString(R.string.options_correlativo))) {
            editor.putString(getString(R.string.options_correlativo), "1.0");
            editor.apply();
            editor.commit();
        }
        editor.commit();
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
        String llave;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPref.contains(getString(R.string.options_licencia))) {
            llave = sharedPref.getString(getString(R.string.options_licencia), "");
            Licencia licencia = new Licencia(getApplicationContext());
            if (!licencia.checkLicencia(llave)) {
                Toast.makeText(getApplicationContext(), "Falla en la validacion de clave", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OpcionesActivity.class);
                startActivity(intent);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No ha ingresado activacion", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, OpcionesActivity.class);
            startActivity(intent);
        }
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
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
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
                    Toast.makeText(getApplicationContext(), "Enviado",Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    Toast.makeText(getApplicationContext(), "Recibido",Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Conectado a " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    public void onStop() {
        super.onStop();
    }

    /**
     * Funcion para carga el Spinner desde la base de datos SQLite
     */
    private void loadSpinnerData() {
        // database handler
        DatabaseOperations db = new DatabaseOperations(getApplicationContext());
        // Spinner Drop down elements
        List<String> cuarteLabels = db.getAllCuartelLista();
        List<String> cajaLabels = db.getAllCajaLista();
        // Creating adapter for spinner
        ArrayAdapter<String> dataCuartelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cuarteLabels);
        ArrayAdapter<String> dataCajaAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cajaLabels);
        // Drop down layout style - list view with radio button
        dataCuartelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataCajaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        CUARTEL.setAdapter(dataCuartelAdapter);
        TIPO_CAJA.setAdapter(dataCajaAdapter);
    }

    /**
     * llamado para ir a opciones
     *
     * @param view
     */
    public void goOptions(View view) {
        Intent intent = new Intent(this, ClaveActivity.class);
        startActivity(intent);
    }

    /**
     * llamado para ir a datos variables
     *
     * @param view
     */
    public void goRecoleccion(View view) {
        List<String> errors = new ArrayList<String>();
        rut_recepcionista = RUT_INPUT.getText().toString().replaceFirst("^0*", "").toLowerCase();
        labor = LABOR.getText().toString();
        cuartel = CUARTEL.getSelectedItem().toString();
        unidad = UNIDAD.getSelectedItem().toString();
        tipo_caja = TIPO_CAJA.getSelectedItem().toString();
        int selectPesaje = TIPO_PESAJE.getCheckedRadioButtonId();
        if (selectPesaje == R.id.radioButton_caja) {
            pesaje = "caja";
        } else if (selectPesaje == R.id.radioButton_kilo) {
            pesaje = "kilo";
        }
        if (rut_recepcionista.equals("") || rut_recepcionista.isEmpty()) {
            errors.add("Debe ingresar Rut o Codigo Recepcionista \n");
        }
        if (labor.equals("") || labor.isEmpty()) {
            errors.add("Debe ingresar Labor \n");
        }
        if (cuartel.equals("") || cuartel.equals("- Seleccione Cuartel -")) {
            errors.add("Debe seleccionar Cuartel \n");
        }
        if (unidad.equals("") || unidad.equals("- Seleccione Unidad -")) {
            errors.add("Debe seleccionar Unidad \n");
        }
        if (tipo_caja.equals("") || tipo_caja.equals("- Seleccione Tipo de Caja -")) {
            errors.add("Debe seleccionar Tipo de Caja \n");
        }
        if (!validate_rut) {
            DatabaseOperations DOP = new DatabaseOperations(this);
            Cursor cursor = DOP.getCosecheroByRut(rut_recepcionista);
            if (cursor != null && cursor.moveToLast()) {
                Cosechero cosechero = new Cosechero(cursor);
                Toast toast = Toast.makeText(this, cosechero.getNombre(), Toast.LENGTH_SHORT);
                toast.show();
                validate_rut = true;
                NOMBRE_RECEPCIONISTA.setText(cosechero.getNombre());
            } else {
                errors.add("Rut no valido \n");
            }
        }
        if (errors.isEmpty() && validate_rut) {
            Intent intent = new Intent(this, RecoleccionActivity.class);
            intent.putExtra(EXTRA_RUT_INPUT, rut_recepcionista);
            intent.putExtra(EXTRA_LABOR, labor);
            intent.putExtra(EXTRA_CUARTEL, cuartel);
            intent.putExtra(EXTRA_UNIDAD, unidad);
            intent.putExtra(EXTRA_TIPO_CAJA, tipo_caja);
            intent.putExtra(EXTRA_TIPO_PESAJE, pesaje);
            startActivity(intent);
            //finish();
        } else {
            Toast toast = Toast.makeText(this, errors.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }


    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (view.getId() == R.id.editText_rut) {
            rut_recepcionista = RUT_INPUT.getText().toString().replaceFirst("^0*", "").toLowerCase();
            DatabaseOperations DOP = new DatabaseOperations(this);
            Cursor cursor = DOP.getCosecheroByRut(rut_recepcionista);

            if (cursor != null && cursor.moveToLast()) {
                Cosechero cosechero = new Cosechero(cursor);
                Toast toast = Toast.makeText(this, cosechero.getNombre(), Toast.LENGTH_SHORT);
                toast.show();
                validate_rut = true;
                NOMBRE_RECEPCIONISTA.setText(cosechero.getNombre());

            } else {
                NOMBRE_RECEPCIONISTA.setText(null);
                Toast toast = Toast.makeText(this, "Rut No Existe", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_scan2) {
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            scanIntegrator.initiateScan();
        }
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
                break;
            default:
                IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                String scanContent = scanningResult.getContents();
                if (scanContent != null) {
                    scanContent = scanContent.replaceFirst("^0*", "").toLowerCase();
                    //Desplegamos en pantalla el contenido del código de barra scaneado
                    RUT_INPUT.setText(scanContent);
                    DatabaseOperations DOP = new DatabaseOperations(this);
                    Cursor cursor = DOP.getCosecheroByRut(scanContent);

                    if (cursor != null && cursor.moveToLast()) {
                        recepcionista = new Cosechero(cursor);
                        Toast toast = Toast.makeText(this, recepcionista.getNombre(), Toast.LENGTH_SHORT);
                        toast.show();
                        validate_rut = true;
                        NOMBRE_RECEPCIONISTA.setText(recepcionista.getNombre());

                    } else {
                        RUT_INPUT.setText(null);
                        NOMBRE_RECEPCIONISTA.setText(null);
                        Toast toast = Toast.makeText(this, "Rut No Existe", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                } else {
                    //Quiere decir que NO se obtuvo resultado
                    Toast.makeText(this, "No se ha recibido datos del scaneo!", Toast.LENGTH_SHORT).show();
                }

        }
        //onResume();
    }

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
