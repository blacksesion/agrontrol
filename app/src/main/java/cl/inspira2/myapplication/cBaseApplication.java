package cl.inspira2.myapplication;

import android.app.Application;

/**
 * Created by blacksesion on 13-01-2017.
 */

public class cBaseApplication extends Application {
    BluetoothPrinterService printer;

    @Override
    public void onCreate() {
        super.onCreate();
        printer = new BluetoothPrinterService();
    }
}
