package cl.inspira2.myapplication;

import android.provider.BaseColumns;

/**
 * Created by blacksesion on 01-12-2016.
 */

public class CapturasContract {
    public static abstract class CapturasEntry implements BaseColumns {
        public static final String TABLE_NAME = "capturas";

        public static final String RUT_RECEPCIONISTA = "rutRecepcionista";
        public static final String TIPO_PESAJE = "tipoPesaje";
        public static final String CUARTEL = "cuartel";
        public static final String LABOR = "labor";
        public static final String UNIDAD = "unidad";
        public static final String ID_CAJA = "idCaja";
        public static final String RUT_COSECHERO = "rutCosechero";
        public static final String N_CAJAS = "NCajas";
        public static final String PESO_CAJAS = "pesoCajas";
        public static final String FECHA = "fecha";
        public static final String CORRELATIVO = "correlativo";
        public static final String CC_COSECHERO = "ccCosechero";
        public static final String COD_COSECHERO = "codCosechero";
        public static final String CC_RECEPCIONISTA = "ccRecepcionista";
        public static final String COD_RECEPCIONISTA = "codRecepcionista";
        public static final String COD_CAPATAZ = "codCapataz";

    }
}
