package cl.inspira2.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {
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
    }

    public void onResume() {
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
        super.onResume();
    }

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
        //Se obtiene el resultado del proceso de scaneo y se parsea
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
        //onResume();
    }
}
