package cl.inspira2.myapplication;

/**
 * Created by blacksesion on 01-12-2016.
 */

import android.content.ContentValues;
import android.database.Cursor;
import java.text.NumberFormat;

import cl.inspira2.myapplication.CapturasContract.CapturasEntry;

public class Captura {
    private String rut_recepcionista;
    private String tipo_pesaje;
    private String cuartel;
    private String labor;
    private String unidad;
    private Integer id_caja;
    private String rut_cosechero;
    private Double n_cajas;
    private Double peso_cajas;
    private String fecha;
    private Double correlativo;
    private String cc_cosechero;
    private String cod_cosechero;
    private String cc_recepcionista;
    private String cod_recepcionista;
    private String cod_capataz;

    public Captura(){

    }

    public Captura(String rut_recepcionista, String tipo_pesaje, String cuartel, String labor, String unidad, Integer id_caja, String rut_cosechero, Double n_cajas, Double peso_cajas, String fecha, Double correlativo, String cc_cosechero, String cod_cosechero, String cc_recepcionista, String cod_recepcionista, String cod_capataz) {
        this.rut_recepcionista = rut_recepcionista;
        this.tipo_pesaje = tipo_pesaje;
        this.cuartel = cuartel;
        this.labor = labor;
        this.unidad = unidad;
        this.id_caja = id_caja;
        this.rut_cosechero = rut_cosechero;
        this.n_cajas = n_cajas;
        this.peso_cajas = peso_cajas;
        this.fecha = fecha;
        this.correlativo = correlativo;
        this.cc_cosechero = cc_cosechero;
        this.cod_cosechero = cod_cosechero;
        this.cc_recepcionista = cc_recepcionista;
        this.cod_recepcionista = cod_recepcionista;
        this.cod_capataz = cod_capataz;
    }

    public Captura (Cursor cursor) {
        this.rut_recepcionista = cursor.getString(cursor.getColumnIndex(CapturasEntry.RUT_RECEPCIONISTA));
        this.tipo_pesaje = cursor.getString(cursor.getColumnIndex(CapturasEntry.TIPO_PESAJE));
        this.cuartel = cursor.getString(cursor.getColumnIndex(CapturasEntry.CUARTEL));
        this.labor = cursor.getString(cursor.getColumnIndex(CapturasEntry.LABOR));
        this.unidad = cursor.getString(cursor.getColumnIndex(CapturasEntry.UNIDAD));
        this.id_caja = cursor.getInt(cursor.getColumnIndex(CapturasEntry.ID_CAJA));
        this.rut_cosechero = cursor.getString(cursor.getColumnIndex(CapturasEntry.RUT_COSECHERO));
        this.n_cajas = cursor.getDouble(cursor.getColumnIndex(CapturasEntry.N_CAJAS));
        this.peso_cajas = cursor.getDouble(cursor.getColumnIndex(CapturasEntry.PESO_CAJAS));
        this.fecha = cursor.getString(cursor.getColumnIndex(CapturasEntry.FECHA));
        this.correlativo = cursor.getDouble(cursor.getColumnIndex(CapturasEntry.CORRELATIVO));
        this.cc_cosechero = cursor.getString(cursor.getColumnIndex(CapturasEntry.CC_COSECHERO));
        this.cod_cosechero = cursor.getString(cursor.getColumnIndex(CapturasEntry.COD_COSECHERO));
        this.cc_recepcionista = cursor.getString(cursor.getColumnIndex(CapturasEntry.CC_RECEPCIONISTA));
        this.cod_recepcionista = cursor.getString(cursor.getColumnIndex(CapturasEntry.COD_RECEPCIONISTA));
        this.cod_capataz = cursor.getString(cursor.getColumnIndex(CapturasEntry.COD_CAPATAZ));
    }

    public String getRut_recepcionista() {
        return rut_recepcionista;
    }

    public void setRut_recepcionista(String rut_recepcionista) {
        this.rut_recepcionista = rut_recepcionista;
    }

    public String getTipo_pesaje() {
        return tipo_pesaje;
    }

    public void setTipo_pesaje(String tipo_pesaje) {
        this.tipo_pesaje = tipo_pesaje;
    }

    public String getCuartel() {
        return cuartel;
    }

    public void setCuartel(String cuartel) {
        this.cuartel = cuartel;
    }

    public String getLabor() {
        return labor;
    }

    public void setLabor(String labor) {
        this.labor = labor;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Integer getId_caja() {
        return id_caja;
    }

    public void setId_caja(Integer id_caja) {
        this.id_caja = id_caja;
    }

    public String getRut_cosechero() {
        return rut_cosechero;
    }

    public void setRut_cosechero(String rut_cosechero) {
        this.rut_cosechero = rut_cosechero;
    }

    public Double getN_cajas() {
        return n_cajas;
    }

    public void setN_cajas(Double n_cajas) {
        this.n_cajas = n_cajas;
    }

    public Double getPeso_cajas() {
        return peso_cajas;
    }

    public void setPeso_cajas(Double peso_cajas) {
        this.peso_cajas = peso_cajas;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Double getCorrelativo() {
        return correlativo;
    }

    public void setCorrelativo(Double correlativo) {
        this.correlativo = correlativo;
    }

    public String getCc_cosechero() {
        return cc_cosechero;
    }

    public void setCc_cosechero(String cc_cosechero) {
        this.cc_cosechero = cc_cosechero;
    }

    public String getCod_cosechero() {
        return cod_cosechero;
    }

    public void setCod_cosechero(String cod_cosechero) {
        this.cod_cosechero = cod_cosechero;
    }

    public String getCc_recepcionista() {
        return cc_recepcionista;
    }

    public void setCc_recepcionista(String cc_recepcionista) {
        this.cc_recepcionista = cc_recepcionista;
    }

    public String getCod_recepcionista() {
        return cod_recepcionista;
    }

    public void setCod_recepcionista(String cod_recepcionista) {
        this.cod_recepcionista = cod_recepcionista;
    }

    public String getCod_capataz() {
        return cod_capataz;
    }

    public void setCod_capataz(String cod_capataz) {
        this.cod_capataz = cod_capataz;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CapturasEntry.RUT_RECEPCIONISTA, rut_recepcionista);
        values.put(CapturasEntry.TIPO_PESAJE, tipo_pesaje);
        values.put(CapturasEntry.CUARTEL, cuartel);
        values.put(CapturasEntry.LABOR, labor);
        values.put(CapturasEntry.UNIDAD, unidad);
        values.put(CapturasEntry.ID_CAJA, String.valueOf(id_caja));
        values.put(CapturasEntry.RUT_COSECHERO, rut_cosechero);
        values.put(CapturasEntry.N_CAJAS, String.valueOf(n_cajas));
        values.put(CapturasEntry.PESO_CAJAS, String.valueOf(peso_cajas));
        values.put(CapturasEntry.FECHA, fecha);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        System.out.println(nf.format(correlativo));

        //values.put(CapturasEntry.CORRELATIVO, String.valueOf(correlativo));
        values.put(CapturasEntry.CORRELATIVO, nf.format(correlativo));
        values.put(CapturasEntry.CC_COSECHERO, cc_cosechero);
        values.put(CapturasEntry.COD_COSECHERO, cod_cosechero);
        values.put(CapturasEntry.CC_RECEPCIONISTA, cc_recepcionista);
        values.put(CapturasEntry.COD_RECEPCIONISTA, cod_recepcionista);
        values.put(CapturasEntry.COD_CAPATAZ, cod_capataz);
        return values;
    }

}
