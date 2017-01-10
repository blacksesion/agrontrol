package cl.inspira2.myapplication;

/**
 * Created by blacksesion on 01-12-2016.
 */

import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONException;
import org.json.JSONObject;

import cl.inspira2.myapplication.CajasContract.CajasEntry;

public class Caja {
    private Integer id;
    private String nombre;
    private Double tara;

    public Caja(Integer id, String nombre, Double tara) {
        this.id = id;
        this.nombre = nombre;
        this.tara = tara;
    }

    public Caja (Cursor cursor){
        this.id = cursor.getInt(cursor.getColumnIndex(CajasEntry.ID_CAJA));
        this.nombre = cursor.getString(cursor.getColumnIndex(CajasEntry.NOMBRE));
        this.tara = cursor.getDouble(cursor.getColumnIndex(CajasEntry.TARA));
    }

    public Caja(JSONObject cajaObject) {
        try {
            this.id = cajaObject.getInt(CajasEntry.ID_CAJA);
            this.nombre = cajaObject.getString(CajasEntry.NOMBRE);
            this.tara = cajaObject.getDouble(CajasEntry.TARA);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getTara() {
        return tara;
    }

    public void setTara(Double tara) {
        this.tara = tara;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CajasEntry.ID_CAJA, String.valueOf(id));
        values.put(CajasEntry.NOMBRE, nombre);
        values.put(CajasEntry.TARA, String.valueOf(tara));
        return values;
    }

}
