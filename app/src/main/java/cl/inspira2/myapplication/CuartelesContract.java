package cl.inspira2.myapplication;

import android.provider.BaseColumns;

/**
 * Created by blacksesion on 01-12-2016.
 */

public class CuartelesContract {
    public static abstract class CuartelesEntry implements BaseColumns {
        public static final String TABLE_NAME = "cuarteles";

        public static final String ID_CUARTEL = "idCuartel";
        public static final String NOMBRE = "nombre";

    }
}
