package cl.inspira2.myapplication;

import android.provider.BaseColumns;

/**
 * Created by blacksesion on 01-12-2016.
 */

public class CosecherosContract {
    public static abstract class CosecherosEntry implements BaseColumns {
        public static final String TABLE_NAME = "cosecheros";

        public static final String RUT = "rut";
        public static final String NOMBRE = "nombre";
        public static final String CENTRO_COSTO = "centroCosto";
        public static final String COD_TRABAJADOR = "codTrabajador";
        public static final String COD_CAPATAZ = "codCapataz";

    }
}
