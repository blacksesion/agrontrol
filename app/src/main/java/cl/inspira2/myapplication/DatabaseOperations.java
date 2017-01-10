package cl.inspira2.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.Editable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cl.inspira2.myapplication.CuartelesContract.CuartelesEntry;
import cl.inspira2.myapplication.CajasContract.CajasEntry;
import cl.inspira2.myapplication.CosecherosContract.CosecherosEntry;
import cl.inspira2.myapplication.CapturasContract.CapturasEntry;

/**
 * Created by blacksesion on 30-11-2016.
 */

public class DatabaseOperations extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Agrontrol.db";
    private final Context contexto;

    public DatabaseOperations(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.contexto = context;
        Log.d("Database operations", "Database created");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                db.setForeignKeyConstraintsEnabled(true);
            } else {
                db.execSQL("PRAGMA foreign_keys=ON");
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table...
        db.execSQL("CREATE TABLE " + CuartelesEntry.TABLE_NAME + " ( "
                + CuartelesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CuartelesEntry.ID_CUARTEL + "  INTEGER,"
                + CuartelesEntry.NOMBRE + " TEXT,"
                + "UNIQUE (" + CuartelesEntry._ID + "))");
        Log.d("Database operations", "Table " + CapturasEntry.TABLE_NAME + " created");
        db.execSQL("CREATE TABLE " + CajasEntry.TABLE_NAME + " ( "
                + CajasEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CajasEntry.ID_CAJA + " INTEGER,"
                + CajasEntry.NOMBRE + " TEXT,"
                + CajasEntry.TARA + " REAL,"
                + "UNIQUE (" + CajasEntry._ID + "))");
        Log.d("Database operations", "Table " + CajasEntry.TABLE_NAME + " created");
        db.execSQL("CREATE TABLE " + CosecherosEntry.TABLE_NAME + " ( "
                + CosecherosEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CosecherosEntry.RUT + " TEXT,"
                + CosecherosEntry.NOMBRE + " TEXT,"
                + CosecherosEntry.CENTRO_COSTO + " TEXT,"
                + CosecherosEntry.COD_TRABAJADOR + " TEXT,"
                + CosecherosEntry.COD_CAPATAZ + " TEXT,"
                + "UNIQUE (" + CosecherosEntry._ID + "))");
        Log.d("Database operations", "Table " + CosecherosEntry.TABLE_NAME + " created");
        db.execSQL("CREATE TABLE " + CapturasEntry.TABLE_NAME + " ( "
                + CapturasEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CapturasEntry.RUT_RECEPCIONISTA + " TEXT,"
                + CapturasEntry.TIPO_PESAJE + " TEXT,"
                + CapturasEntry.CUARTEL + " TEXT,"
                + CapturasEntry.LABOR + " TEXT,"
                + CapturasEntry.UNIDAD + " TEXT,"
                + CapturasEntry.ID_CAJA + " INTEGER,"
                + CapturasEntry.RUT_COSECHERO + " TEXT,"
                + CapturasEntry.N_CAJAS + " REAL,"
                + CapturasEntry.PESO_CAJAS + " REAL,"
                + CapturasEntry.FECHA + " TEXT,"
                + CapturasEntry.CORRELATIVO + " REAL,"
                + CapturasEntry.CC_COSECHERO + " TEXT,"
                + CapturasEntry.COD_COSECHERO + " TEXT,"
                + CapturasEntry.CC_RECEPCIONISTA + " TEXT,"
                + CapturasEntry.COD_RECEPCIONISTA + " TEXT,"
                + CapturasEntry.COD_CAPATAZ + " TEXT,"
                + "UNIQUE (" + CapturasEntry._ID + "))");
        Log.d("Database operations", "Table " + CapturasEntry.TABLE_NAME + " created");
        Log.d("Database operations", "Cargo los datos");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + CuartelesEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CajasEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CosecherosEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CapturasEntry.TABLE_NAME);
        // Creating tables again
        onCreate(db);
    }

    public List<String> getAllCuartelLista() {
        List<String> labels = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CuartelesEntry.ID_CUARTEL, CuartelesEntry.NOMBRE};
        Cursor cursor = db.query(CuartelesEntry.TABLE_NAME, columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(cursor.getColumnIndex(CuartelesEntry.ID_CUARTEL)) + " " + cursor.getString(cursor.getColumnIndex(CuartelesEntry.NOMBRE)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return labels;
    }

    public List<String> getAllCajaLista() {
        List<String> labels = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CajasEntry.ID_CAJA, CajasEntry.NOMBRE, CajasEntry.TARA};
        Cursor cursor = db.query(CajasEntry.TABLE_NAME, columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(cursor.getColumnIndex(CajasEntry.NOMBRE)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return labels;
    }

    public JSONArray getJsonCapturas() {
        JSONArray resultSet = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"*"};
        Cursor cursor = db.query(CapturasEntry.TABLE_NAME, columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int totalColumn = cursor.getColumnCount();
                JSONObject rowObject = new JSONObject();
                for (int i = 0; i < totalColumn; i++) {
                    if (cursor.getColumnName(i) != null) {
                        try {
                            if (cursor.getString(i) != null) {
                                rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                            } else {
                                rowObject.put(cursor.getColumnName(i), "");
                            }
                        } catch (Exception e) {
                            Log.d("TAG_NAME", e.getMessage());
                        }
                    }
                }
                resultSet.put(rowObject);
            } while (cursor.moveToNext());
        }
        return resultSet;
    }

    public Cursor getCosecheros() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CosecherosEntry.RUT, CosecherosEntry.NOMBRE, CosecherosEntry.CENTRO_COSTO, CosecherosEntry.COD_TRABAJADOR, CosecherosEntry.COD_CAPATAZ};
        Cursor cursor = db.query(CosecherosEntry.TABLE_NAME, columns, null, null, null, null, null);
        return cursor;
    }

    public Cursor getReportMinMax() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"MIN(" + CapturasEntry.CORRELATIVO + ") AS min", "MAX(" + CapturasEntry.CORRELATIVO + ") AS max"};
        Cursor cursor = db.query(CapturasEntry.TABLE_NAME, columns, null, null, null, null, null);
        return cursor;
    }

    public Cursor getCosecherosLimit(Integer limite) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CosecherosEntry.RUT, CosecherosEntry.NOMBRE, CosecherosEntry.CENTRO_COSTO, CosecherosEntry.COD_TRABAJADOR, CosecherosEntry.COD_CAPATAZ};
        Cursor cursor = db.query(CosecherosEntry.TABLE_NAME, columns, null, null, null, null, null, String.valueOf(limite));
        return cursor;
    }

    public Cursor getCajaByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CajasEntry.ID_CAJA, CajasEntry.NOMBRE, CajasEntry.TARA};
        String selection = CajasEntry.NOMBRE + " = ?";
        String[] selectionArgs = {name};
        Cursor cursor = db.query(CajasEntry.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
        return cursor;
    }

    public Cursor getCosecheroByRut(String rut) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CosecherosEntry.RUT, CosecherosEntry.NOMBRE, CosecherosEntry.CENTRO_COSTO, CosecherosEntry.COD_TRABAJADOR, CosecherosEntry.COD_CAPATAZ};
        // Filter results WHERE "rut" = 'mi rut'
        String selection = CosecherosEntry.RUT + " = ?";
        String[] selectionArgs = {rut};

        Cursor cursor = db.query(CosecherosEntry.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
        return cursor;
    }

    public void guardarCosechero(Cosechero cosechero) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.insert(
                CosecherosEntry.TABLE_NAME,
                null,
                cosechero.toContentValues()
        );
    }

    public void guardarCuartel(Cuartel cuartel) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.insert(
                CuartelesEntry.TABLE_NAME,
                null,
                cuartel.toContentValues()
        );
    }

    public void guardarCaptura(Captura captura) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.insert(
                CapturasEntry.TABLE_NAME,
                null,
                captura.toContentValues()
        );
    }

    public void guardarCaja(Caja caja) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.insert(
                CajasEntry.TABLE_NAME,
                null,
                caja.toContentValues()
        );
    }

    public Boolean checkEmptyTable(String TABLE_NAME) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        Boolean rowExists;

        if (mCursor.moveToFirst()) {
            // DO SOMETHING WITH CURSOR
            rowExists = true;

        } else {
            // I AM EMPTY
            rowExists = false;
        }


        return !rowExists;
    }

    public Cursor getReporte(String text, String text1) {

        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {CapturasEntry.CUARTEL, CapturasEntry.UNIDAD, "SUM(" + CapturasEntry.N_CAJAS + ") AS ncajas", "SUM(" + CapturasEntry.PESO_CAJAS + ") AS kgneto"};
        String selection = CapturasEntry.CORRELATIVO + " >= ? AND " + CapturasEntry.CORRELATIVO + " <= ?";
        String[] selectionArgs = {text, text1};

        return db.query(CapturasEntry.TABLE_NAME, columns, selection, selectionArgs, CapturasEntry.CUARTEL + ", " + CapturasEntry.UNIDAD, null, CapturasEntry.CUARTEL + ", " + CapturasEntry.UNIDAD);
    }

    public void importData(JSONObject data) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            JSONArray cuarteles = data.getJSONArray(CuartelesEntry.TABLE_NAME);
            JSONArray cajas = data.getJSONArray(CajasEntry.TABLE_NAME);
            JSONArray cosecheros = data.getJSONArray(CosecherosEntry.TABLE_NAME);

            db.execSQL("DROP TABLE IF EXISTS " + CuartelesEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + CajasEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + CosecherosEntry.TABLE_NAME);
            db.execSQL("CREATE TABLE " + CuartelesEntry.TABLE_NAME + " ( "
                    + CuartelesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CuartelesEntry.ID_CUARTEL + "  INTEGER,"
                    + CuartelesEntry.NOMBRE + " TEXT,"
                    + "UNIQUE (" + CuartelesEntry._ID + "))");
            db.execSQL("CREATE TABLE " + CajasEntry.TABLE_NAME + " ( "
                    + CajasEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CajasEntry.ID_CAJA + " INTEGER,"
                    + CajasEntry.NOMBRE + " TEXT,"
                    + CajasEntry.TARA + " REAL,"
                    + "UNIQUE (" + CajasEntry._ID + "))");
            db.execSQL("CREATE TABLE " + CosecherosEntry.TABLE_NAME + " ( "
                    + CosecherosEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CosecherosEntry.RUT + " TEXT,"
                    + CosecherosEntry.NOMBRE + " TEXT,"
                    + CosecherosEntry.CENTRO_COSTO + " TEXT,"
                    + CosecherosEntry.COD_TRABAJADOR + " TEXT,"
                    + CosecherosEntry.COD_CAPATAZ + " TEXT,"
                    + "UNIQUE (" + CosecherosEntry._ID + "))");

            for (int i = 0; i < cuarteles.length(); i++) {
                JSONObject cuartelObject = cuarteles.getJSONObject(i);
                guardarCuartel(new Cuartel(cuartelObject));
            }
            for (int i = 0; i < cajas.length(); i++) {
                JSONObject cajaObject = cajas.getJSONObject(i);
                guardarCaja(new Caja(cajaObject));
            }
            for (int i = 0; i < cosecheros.length(); i++) {
                JSONObject cosecheroObject = cosecheros.getJSONObject(i);
                guardarCosechero(new Cosechero(cosecheroObject));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean clearCapturas() {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            db.execSQL("DROP TABLE IF EXISTS " + CapturasEntry.TABLE_NAME);
            db.execSQL("CREATE TABLE " + CapturasEntry.TABLE_NAME + " ( "
                    + CapturasEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CapturasEntry.RUT_RECEPCIONISTA + " TEXT,"
                    + CapturasEntry.TIPO_PESAJE + " TEXT,"
                    + CapturasEntry.CUARTEL + " TEXT,"
                    + CapturasEntry.LABOR + " TEXT,"
                    + CapturasEntry.UNIDAD + " TEXT,"
                    + CapturasEntry.ID_CAJA + " INTEGER,"
                    + CapturasEntry.RUT_COSECHERO + " TEXT,"
                    + CapturasEntry.N_CAJAS + " REAL,"
                    + CapturasEntry.PESO_CAJAS + " REAL,"
                    + CapturasEntry.FECHA + " TEXT,"
                    + CapturasEntry.CORRELATIVO + " REAL,"
                    + CapturasEntry.CC_COSECHERO + " TEXT,"
                    + CapturasEntry.COD_COSECHERO + " TEXT,"
                    + CapturasEntry.CC_RECEPCIONISTA + " TEXT,"
                    + CapturasEntry.COD_RECEPCIONISTA + " TEXT,"
                    + CapturasEntry.COD_CAPATAZ + " TEXT,"
                    + "UNIQUE (" + CapturasEntry._ID + "))");
            Log.d("Database operations", "Table " + CapturasEntry.TABLE_NAME + " created");
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
