package cl.inspira2.myapplication;

/**
 * Created by blacksesion on 01-12-2016.
 */

import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONException;
import org.json.JSONObject;

import cl.inspira2.myapplication.CosecherosContract.CosecherosEntry;

public class Cosechero {
    private String rut;
    private String nombre;
    private String centro_costo;
    private String cod_trabajador;
    private String cod_capataz;

    public Cosechero(String rut, String nombre, String centro_costo, String cod_trabajador, String cod_capataz) {
        this.rut = rut;
        this.nombre = nombre;
        this.centro_costo = centro_costo;
        this.cod_trabajador = cod_trabajador;
        this.cod_capataz = cod_capataz;
    }

    public Cosechero(Cursor cursor) {
        this.rut = cursor.getString(cursor.getColumnIndex(CosecherosEntry.RUT));
        this.nombre = cursor.getString(cursor.getColumnIndex(CosecherosEntry.NOMBRE));
        this.centro_costo = cursor.getString(cursor.getColumnIndex(CosecherosEntry.CENTRO_COSTO));
        this.cod_trabajador = cursor.getString(cursor.getColumnIndex(CosecherosEntry.COD_TRABAJADOR));
        this.cod_capataz = cursor.getString(cursor.getColumnIndex(CosecherosEntry.COD_CAPATAZ));

    }

    public Cosechero(JSONObject cosecheroObject) {
        try {
            this.rut = cosecheroObject.getString(CosecherosEntry.RUT);
            this.nombre = cosecheroObject.getString(CosecherosEntry.NOMBRE);
            this.centro_costo = cosecheroObject.getString(CosecherosEntry.CENTRO_COSTO);
            this.cod_trabajador = cosecheroObject.getString(CosecherosEntry.COD_TRABAJADOR);
            this.cod_capataz = cosecheroObject.getString(CosecherosEntry.COD_CAPATAZ);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCentro_costo() {
        return centro_costo;
    }

    public void setCentro_costo(String centro_costo) {
        this.centro_costo = centro_costo;
    }

    public String getCod_trabajador() {
        return cod_trabajador;
    }

    public void setCod_trabajador(String cod_trabajador) {
        this.cod_trabajador = cod_trabajador;
    }

    public String getCod_capataz() {
        return cod_capataz;
    }

    public void setCod_capataz(String cod_capataz) {
        this.cod_capataz = cod_capataz;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CosecherosEntry.RUT, rut);
        values.put(CosecherosEntry.NOMBRE, nombre);
        values.put(CosecherosEntry.CENTRO_COSTO, centro_costo);
        values.put(CosecherosEntry.COD_TRABAJADOR, cod_trabajador);
        values.put(CosecherosEntry.COD_CAPATAZ, cod_capataz);
        return values;
    }

}
