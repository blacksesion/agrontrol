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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecoleccionActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {

    private LinearLayout layoutKilo;
    private Button scanBtn;

    EditText RUT_COSECHERO;
    EditText UNIDADES;
    EditText TARA_CAJA;
    EditText PESO_CAJA;
    TextView LABEL_PESO;
    TextView NOMBRE_COSECHERO;
    // variables del intent anterior
    String rut_recepcionista;
    String cuartel;
    String unidad;
    String tipo_caja;
    String labor;
    String pesaje;
    // variables de este intent
    String rut_cosechero = "";
    String unidades = "0";
    String tara_caja = "0";
    String peso_caja = "0";
    Boolean validate_rut = false;

    Cosechero recepcionista;
    Cosechero cosechero;
    Caja caja;

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
        setContentView(R.layout.activity_recoleccion);

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

        DatabaseOperations DOP = new DatabaseOperations(this);

        // variables del mainactivity
        Intent intent = getIntent();
        rut_recepcionista = intent.getStringExtra(MainActivity.EXTRA_RUT_INPUT);
        cuartel = intent.getStringExtra(MainActivity.EXTRA_CUARTEL);
        unidad = intent.getStringExtra(MainActivity.EXTRA_UNIDAD);
        tipo_caja = intent.getStringExtra(MainActivity.EXTRA_TIPO_CAJA);
        labor = intent.getStringExtra(MainActivity.EXTRA_LABOR);
        pesaje = intent.getStringExtra(MainActivity.EXTRA_TIPO_PESAJE);

        //seteo los input de la vista
        RUT_COSECHERO = (EditText) findViewById(R.id.editText_rut_cosechero);
        UNIDADES = (EditText) findViewById(R.id.editText_unidades);
        TARA_CAJA = (EditText) findViewById(R.id.editText_taraCaja);
        PESO_CAJA = (EditText) findViewById(R.id.editText_pesoCajas);
        LABEL_PESO = (TextView) findViewById(R.id.textView_pesoNeto);
        NOMBRE_COSECHERO = (TextView) findViewById(R.id.textView_cosechero);
        layoutKilo = (LinearLayout) findViewById(R.id.kilo_layout);
        //Se Instancia el botón de Scan
        scanBtn = (Button) findViewById(R.id.button_scan);
        //Se agrega la clase MainActivity.java como Listener del evento click del botón de Scan
        scanBtn.setOnClickListener(this);

        Cursor caja_cursor = DOP.getCajaByName(tipo_caja);
        if (caja_cursor != null && caja_cursor.moveToLast()) {
            caja = new Caja(caja_cursor);
        }

        Cursor cursor = DOP.getCosecheroByRut(rut_recepcionista);
        if (cursor != null && cursor.moveToLast()) {
            recepcionista = new Cosechero(cursor);
        }

        RUT_COSECHERO.setOnFocusChangeListener(this);
        PESO_CAJA.setOnFocusChangeListener(this);
        TARA_CAJA.setOnFocusChangeListener(this);
        UNIDADES.setOnFocusChangeListener(this);

        PESO_CAJA.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                validarPeso(new View(getApplicationContext()));
            }
        });
        TARA_CAJA.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                validarPeso(new View(getApplicationContext()));
            }
        });
        UNIDADES.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                validarPeso(new View(getApplicationContext()));
            }
        });

        if (pesaje.equals("kilo")) {
            layoutKilo.setVisibility(View.VISIBLE);
        } else {
            layoutKilo.setVisibility(View.INVISIBLE);
        }
        if (tipo_caja.contains("PALLET")) {
            TARA_CAJA.setText("0");
        } else {
            TARA_CAJA.setText(caja.getTara().toString());
            TARA_CAJA.setEnabled(false);
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


    public void onStop() {
        super.onStop();
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

    public void goReporte(View view) {
        Intent intent = new Intent(this, ReporteActivity.class);
        startActivity(intent);
    }

    public void saveCapture(View view) {
        List<String> errors = new ArrayList<String>();
        rut_cosechero = RUT_COSECHERO.getText().toString().replaceFirst("^0*", "").toLowerCase();
        validarPeso(view);
        DatabaseOperations DOP = new DatabaseOperations(this);

        Cursor caja_cursor = DOP.getCajaByName(tipo_caja);
        if (caja_cursor != null && caja_cursor.moveToLast()) {
            caja = new Caja(caja_cursor);
        } else {
            errors.add("Error en el tipo de caja \n");
        }

        Cursor recepcionista_cursor = DOP.getCosecheroByRut(rut_recepcionista);
        if (recepcionista_cursor != null && recepcionista_cursor.moveToLast()) {
            recepcionista = new Cosechero(recepcionista_cursor);
        } else {
            errors.add("Error en el Rut de Recepcionista \n");
        }

        if (rut_cosechero.equals("") || rut_cosechero.isEmpty()) {
            errors.add("Debe ingresar Rut o Codigo Recepcionista \n");
        }
        if (unidades.equals("") || unidades.isEmpty()) {
            errors.add("Debe Ingresar Unidades \n");
        }
        if (pesaje.equals("kilo")) {
            if (tara_caja.equals("") || tara_caja.isEmpty()) {
                errors.add("Debe Ingresar Tara Caja \n");
            }
            if (peso_caja.equals("") || peso_caja.isEmpty()) {
                errors.add("Debe Ingresar Peso Caja \n");
            }
        } else {
            tara_caja = "0";
            peso_caja = "0";
        }
        if (!validate_rut) {
            Cursor cursor = DOP.getCosecheroByRut(rut_cosechero);
            if (cursor != null && cursor.moveToLast()) {
                cosechero = new Cosechero(cursor);
                Toast toast = Toast.makeText(this, cosechero.getNombre(), Toast.LENGTH_SHORT);
                toast.show();
                validate_rut = true;
                NOMBRE_COSECHERO.setText(cosechero.getNombre());
            } else {
                errors.add("Rut no valido \n");
            }
        }
        if (errors.isEmpty() && validate_rut) {
            Captura captura;
            // lo que va si pasa las validaciones
            Double correlativo = getCorrelativo();
            String idCapt = "";
            Integer id_caja = Integer.parseInt(caja.getId().toString());
            String cc_cosechero = cosechero.getCentro_costo();
            String cod_cosechero = cosechero.getCod_trabajador();
            String cc_recepcionista = recepcionista.getCentro_costo();
            String cod_recepcionista = recepcionista.getCod_trabajador();
            String cod_capataz = recepcionista.getCod_capataz();
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = df.format(c.getTime());
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            /*
            if (sharedPref.contains(getString(R.string.options_printer))) {
                printAddres = sharedPref.getString(getString(R.string.options_printer), "");
            }
            */
            if (sharedPref.contains(getString(R.string.options_id_capturador))) {
                idCapt = sharedPref.getString(getString(R.string.options_id_capturador), "0");
                idCapt = String.valueOf(Math.round(Double.parseDouble(idCapt)));
            }

            String cor_unpadded = String.valueOf(Math.round(correlativo));
            String cor_padded = "0000000000".substring(cor_unpadded.length()) + cor_unpadded;
            String idc_padded = "00".substring(idCapt.length()) + idCapt;
            String correlativo_full = idc_padded + cor_padded;

            if (pesaje.equals("kilo")) {
                captura = new Captura(rut_recepcionista, pesaje.toUpperCase(), cuartel, labor, unidad, id_caja, rut_cosechero, Double.valueOf(unidades), Double.parseDouble(LABEL_PESO.getText().toString()), formattedDate, Double.parseDouble(correlativo_full), cc_cosechero, cod_cosechero, cc_recepcionista, cod_recepcionista, cod_capataz);
            } else {
                captura = new Captura(rut_recepcionista, pesaje.toUpperCase(), cuartel, labor, unidad, id_caja, rut_cosechero, Double.valueOf(unidades), 0.0, formattedDate, Double.parseDouble(correlativo_full), cc_cosechero, cod_cosechero, cc_recepcionista, cod_recepcionista, cod_capataz);
            }
            try {
                DOP.guardarCaptura(captura);
            } catch (Exception e) {
                Toast.makeText(this, "Error al Guardar:" + e.toString(), Toast.LENGTH_SHORT).show();
            }
            try {
                for (int i = 0; i < 2; i++) {
                    long milis = System.currentTimeMillis();
                    String date = DateUtil.timeMilisToString(milis, "MMM dd, yyyy");
                    String time = DateUtil.timeMilisToString(milis, "hh:mm a");
                    StringBuffer contentBuffer = new StringBuffer(100);

                    contentBuffer.append("\n\n=============================\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify(date, time, DataConstants.RECEIPT_WIDTH) + "\n");
                    //contentBuffer.append(Util.nameLeftValueRightJustify("Correlativo No:", String.valueOf(Math.round(captura.getCorrelativo())), DataConstants.RECEIPT_WIDTH) + "\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify("Correlativo No:", correlativo_full, DataConstants.RECEIPT_WIDTH) + "\n");
                    contentBuffer.append("------------------------------\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify("Recepcionista:", captura.getCod_recepcionista(), DataConstants.RECEIPT_WIDTH) + "\n");
                    if (recepcionista.getNombre().length() > 30) {
                        contentBuffer.append(recepcionista.getNombre().substring(0, 30) + "\n");
                    } else {
                        contentBuffer.append(recepcionista.getNombre() + "\n");
                    }
                    contentBuffer.append(Util.nameLeftValueRightJustify("Cuartel:", captura.getCuartel(), DataConstants.RECEIPT_WIDTH) + "\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify("Labor:", captura.getLabor(), DataConstants.RECEIPT_WIDTH) + "\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify("Unidad:", captura.getUnidad(), DataConstants.RECEIPT_WIDTH) + "\n");
                    contentBuffer.append("------------------------------\n");
                    contentBuffer.append(Util.nameLeftValueRightJustify("Cosechero:", captura.getCod_cosechero(), DataConstants.RECEIPT_WIDTH) + "\n");
                    if (cosechero.getNombre().length() > 30) {
                        contentBuffer.append(cosechero.getNombre().substring(0, 30) + "\n");
                    } else {
                        contentBuffer.append(cosechero.getNombre() + "\n");
                    }
                    if (!pesaje.equals("kilo")) {
                        contentBuffer.append(Util.nameLeftValueRightJustify("No Cajas:", captura.getN_cajas().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Tipo Caja:", caja.getNombre(), DataConstants.RECEIPT_WIDTH) + "\n");
                    } else {
                        contentBuffer.append(Util.nameLeftValueRightJustify("No Cajas:", captura.getN_cajas().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Tipo Caja:", caja.getNombre(), DataConstants.RECEIPT_WIDTH) + "\n");
                        contentBuffer.append(Util.nameLeftValueRightJustify("Peso Neto:", captura.getPeso_cajas().toString(), DataConstants.RECEIPT_WIDTH) + "\n");
                    }
                    String receiptContent = contentBuffer.toString();

                    //2D Bar Code
                    //byte[] formats = {(byte) 0x1d, (byte) 0x6b, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1f};
                    //1D Bar Code
                    byte[] formats = {(byte) 0x1d, (byte) 0x6b, (byte) 0x02, (byte) 0x0d};
                    //byte[] contents = String.valueOf(Math.round(captura.getCorrelativo())).getBytes();
                    byte[] contents = correlativo_full.getBytes();
                    byte[] barcode = new byte[formats.length + contents.length];
                    System.arraycopy(formats, 0, barcode, 0, formats.length);
                    System.arraycopy(contents, 0, barcode, formats.length, contents.length);

                    String receiptWeb = "** Agrontrol ** " + "\n" +
                            "\n" +
                            "\n";

                    byte[] content = Printer.printfont(receiptContent + "\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN2);
                    byte[] web = Printer.printfont("\n" + receiptWeb + "\n" +
                            "\n" +
                            "\n", FontDefine.FONT_32PX, FontDefine.Align_CENTER, (byte) 0x1A, PocketPos.LANGUAGE_SPAIN2);

                    byte[] totaldata = new byte[content.length + barcode.length + web.length];

                    int offset = 0;
                    System.arraycopy(content, 0, totaldata, offset, content.length);
                    offset += content.length;

                    System.arraycopy(barcode, 0, totaldata, offset, barcode.length);
                    offset += barcode.length;

                    System.arraycopy(web, 0, totaldata, offset, web.length);
                    byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totaldata, 0, totaldata.length);

                    //mmOutputStream.write(senddata);
                    // envio los datos
                    mPrintService.write(senddata);
                    // Reset out string buffer to zero and clear the edit text field
                    mOutStringBuffer.setLength(0);
                }
                // tell the user data were sent
                Toast.makeText(this, "Ticket impreso.", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ocurrio un error al Imprimir el ticket :" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(this, "Captura guardada", Toast.LENGTH_SHORT).show();
            RUT_COSECHERO.getText().clear();
            PESO_CAJA.setText("0");
            //TARA_CAJA.getText().clear();
            UNIDADES.setText("0");
            NOMBRE_COSECHERO.setText(null);
            LABEL_PESO.setText("0");
        } else {
            Toast.makeText(this, errors.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (view.getId() == R.id.editText_rut_cosechero) {
            rut_cosechero = RUT_COSECHERO.getText().toString().replaceFirst("^0*", "").toLowerCase();
            DatabaseOperations DOP = new DatabaseOperations(this);
            Cursor cursor = DOP.getCosecheroByRut(rut_cosechero);

            if (cursor != null && cursor.moveToLast()) {
                cosechero = new Cosechero(cursor);
                Toast toast = Toast.makeText(this, cosechero.getNombre(), Toast.LENGTH_SHORT);
                toast.show();
                validate_rut = true;
                NOMBRE_COSECHERO.setText(cosechero.getNombre());

            } else {
                NOMBRE_COSECHERO.setText(null);
                Toast.makeText(this, "Rut No Existe", Toast.LENGTH_SHORT).show();
            }
        }
/*
        if (view.getId() == R.id.editText_pesoCajas || view.getId() == R.id.editText_taraCaja || view.getId() == R.id.editText_unidades) {
            validarPeso(view);
        }
*/
        validarPeso(view);
    }

    public double getCorrelativo() {
        //SharedPreferences sharedPref = this.getPreferences(this.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        double correlativo;

        if (sharedPref.contains(getString(R.string.options_correlativo))) {
            correlativo = Double.parseDouble(sharedPref.getString(getString(R.string.options_correlativo), "")) + 1;
            editor.putString(getString(R.string.options_correlativo), String.valueOf(correlativo));
            editor.commit();
        } else {
            correlativo = 1.0;
            editor.putString(getString(R.string.options_correlativo), String.valueOf(correlativo));
            editor.commit();
        }
        return correlativo;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_scan) {
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
                    RUT_COSECHERO.setText(scanContent);
                    DatabaseOperations DOP = new DatabaseOperations(this);
                    Cursor cursor = DOP.getCosecheroByRut(scanContent);

                    if (cursor != null && cursor.moveToLast()) {
                        cosechero = new Cosechero(cursor);
                        Toast toast = Toast.makeText(this, cosechero.getNombre(), Toast.LENGTH_SHORT);
                        toast.show();
                        validate_rut = true;
                        NOMBRE_COSECHERO.setText(cosechero.getNombre());

                    } else {
                        RUT_COSECHERO.setText(null);
                        NOMBRE_COSECHERO.setText(null);
                        Toast toast = Toast.makeText(this, "Rut No Existe", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                } else {
                    //Quiere decir que NO se obtuvo resultado
                    Toast.makeText(this, "No se ha recibido datos del scaneo!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public void validarPeso(View view) {
        try {
            unidades = UNIDADES.getText().toString();
            tara_caja = TARA_CAJA.getText().toString();
            peso_caja = PESO_CAJA.getText().toString();
            Double peso = (Double.parseDouble(peso_caja) - (Double.parseDouble(tara_caja) * Double.parseDouble(unidades) / 1000.0));
            LABEL_PESO.setText(peso.toString());
        } catch (Exception e) {
            Toast.makeText(this, "Error al calcular el peso:" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
