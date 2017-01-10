package cl.inspira2.myapplication;

import android.content.Context;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Created by blacksesion on 11-12-2016.
 */

public class Licencia {

    private String deviceId;

    public Licencia(final Context context) {
        final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            if (mTelephony.getDeviceId() == null) {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } else {
                deviceId = mTelephony.getDeviceId();
            }
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean checkLicencia(String key) {
        String llave = null;
        try {
            llave = getLlave();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (key.equals(llave)) {
            return true;
        }
        return false;
    }


    private String getLlave() throws UnsupportedEncodingException, NoSuchAlgorithmException {

        byte[] bytes = deviceId.getBytes("UTF-8");
        MessageDigest m = MessageDigest.getInstance("SHA-1");
        byte[] digest = m.digest(bytes);
        String hash = new BigInteger(1, digest).toString(16);

        StringBuilder stringBuilder2 = new StringBuilder();
        int num = 1;
        for (char ch : hash.toCharArray()) {
            try {
                stringBuilder2.append(Integer.parseInt(String.valueOf(ch)));
            } catch (Exception ex) {
                stringBuilder2.append(String.valueOf(num));
                ++num;
            }
        }
        return stringBuilder2.toString().substring(0, 15);

    }
}
