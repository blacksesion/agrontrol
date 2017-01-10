package cl.inspira2.myapplication;

/**
 * Created by blacksesion on 01-12-2016.
 */

import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONException;
import org.json.JSONObject;

import cl.inspira2.myapplication.CuartelesContract.CuartelesEntry;

public class Cuartel {
    private Integer id;
    private String nombre;

    public Cuartel (Integer id, String nombre){
        this.id = id;
        this.nombre = nombre;
    }

    public Cuartel (Cursor cursor) {
        this.id = cursor.getInt(cursor.getColumnIndex(CuartelesEntry.ID_CUARTEL));
        this.nombre = cursor.getString(cursor.getColumnIndex(CuartelesEntry.NOMBRE));
    }

    public Cuartel(JSONObject cuartelObject) {
        try {
            this.id = cuartelObject.getInt(CuartelesEntry.ID_CUARTEL);
            this.nombre = cuartelObject.getString(CuartelesEntry.NOMBRE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CuartelesEntry.ID_CUARTEL, String.valueOf(id));
        values.put(CuartelesEntry.NOMBRE, nombre);
        return values;
    }
}
