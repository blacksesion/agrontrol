package cl.inspira2.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ClaveActivity extends AppCompatActivity {

    private static String accessKey = "1234";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clave);
        String llave;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPref.contains(getString(R.string.options_licencia))) {
            llave = sharedPref.getString(getString(R.string.options_licencia), "");
            Licencia licencia = new Licencia(getApplicationContext());
            if(!licencia.checkLicencia(llave)){
                Toast.makeText(getApplicationContext(), "Falla en la validacion de clave", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OpcionesActivity.class);
                startActivity(intent);
            }
        }else{
            Toast.makeText(getApplicationContext(), "No ha ingresado activacion", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, OpcionesActivity.class);
            startActivity(intent);
        }
    }

    public void goOptions(View view){
        EditText editText = (EditText) findViewById(R.id.passOptInput);
        String message = editText.getText().toString();
        if(message.equals(accessKey)){
            Intent intent = new Intent(this, OpcionesActivity.class);
            startActivity(intent);
        }else{
            Context context = getApplicationContext();
            CharSequence mensaje = "Contraseña Incorrecta!";
            Toast toast = Toast.makeText(context, mensaje, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
