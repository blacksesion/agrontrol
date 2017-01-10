package cl.inspira2.myapplication;

import android.provider.BaseColumns;

/**
 * Created by blacksesion on 01-12-2016.
 */

public class CajasContract {

    public static abstract class CajasEntry implements BaseColumns{
        public static final String TABLE_NAME = "cajas";

        public static final String ID_CAJA = "idCaja";
        public static final String NOMBRE = "nombre";
        public static final String TARA = "tara";

    }
}
