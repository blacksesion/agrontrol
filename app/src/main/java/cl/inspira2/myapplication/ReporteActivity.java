package cl.inspira2.myapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.text.NumberFormat;

public class ReporteActivity extends AppCompatActivity {
    EditText CORRELATIVO_INICIAL;
    EditText CORRELATIVO_FINAL;
    TableLayout GRID_LAYOUT;

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
        setContentView(R.layout.activity_reporte);

        CORRELATIVO_INICIAL = (EditText) findViewById(R.id.editText_correlativoInicio);
        CORRELATIVO_FINAL = (EditText) findViewById(R.id.editText_correlativoFin);
        GRID_LAYOUT = (TableLayout) findViewById(R.id.grid);

        DatabaseOperations DOP = new DatabaseOperations(this);
        Cursor min_max = DOP.getReportMinMax();
        if (min_max.moveToFirst()) {
            CORRELATIVO_INICIAL.setText(min_max.getString(min_max.getColumnIndex("min")).replace(",", ""));
            CORRELATIVO_FINAL.setText(min_max.getString(min_max.getColumnIndex("max")).replace(",", ""));
        }

        /**
         * Aca va lo nuevo para bloutooth como servicio
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no esta disponible", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(), "Conectado a " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
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
            TableRow totalc = new TableRow(this);
            TextView a = new TextView(this);
            TextView b = new TextView(this);
            TableRow totalk = new TableRow(this);
            TextView c = new TextView(this);
            TextView d = new TextView(this);
            a.setText("Total Cajas:");
            b.setText(totalCajas.toString());
            c.setText("Total Kilos:");
            d.setText(totalKilos.toString());
            totalc.addView(a);
            totalc.addView(b);
            totalk.addView(c);
            totalk.addView(d);
            GRID_LAYOUT.addView(totalc);
            GRID_LAYOUT.addView(totalk);
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
            } else {
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

            //mmOutputStream.write(senddata);

            // envio los datos
            mPrintService.write(senddata);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);

            GRID_LAYOUT.removeAllViewsInLayout();
            // tell the user data were sent
            Toast.makeText(this, "Reporte impreso.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ocurrio un error al Imprimir" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
