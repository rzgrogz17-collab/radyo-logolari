package ogzapp.wordgame.activity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Ülkeyi ISO Alpha-2 olarak bulur. SIM / şebeke / sistem bölgesi ile
 * IP coğrafi konumunu birlikte okur.
 */
public final class CountryDetector {

    public interface Callback {
        void onCountry(String isoAlpha2);
    }

    private static final String IP_URL = "https://ipwho.is/";
    private static final int TIMEOUT_MS = 2500;

    private CountryDetector() {
    }

    public static void detect(final Context context, final Callback callback) {
        final String deviceCountry = readDeviceCountry(context);
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ipCountry = readIpCountry();
                final String resolved = pick(deviceCountry, ipCountry);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCountry(resolved);
                    }
                });
            }
        }, "country-detector").start();
    }

    private static String pick(String deviceCountry, String ipCountry) {
        boolean hasDevice = isIso2(deviceCountry);
        boolean hasIp = isIso2(ipCountry);
        if (hasDevice && hasIp) {
            if (deviceCountry.equals(ipCountry)) return deviceCountry;
            // SIM / şebeke, yalnızca dil bölgesinden daha kesindir.
            return deviceCountry;
        }
        if (hasIp) return ipCountry;
        if (hasDevice) return deviceCountry;
        return "US";
    }

    private static String readDeviceCountry(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String sim = normalize(tm.getSimCountryIso());
                if (isIso2(sim)) return sim;
                String network = normalize(tm.getNetworkCountryIso());
                if (isIso2(network)) return network;
            }
        } catch (Exception ignored) {
        }
        return normalize(Locale.getDefault().getCountry());
    }

    private static String readIpCountry() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(IP_URL).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();
            JSONObject json = new JSONObject(body.toString());
            return normalize(json.optString("country_code", ""));
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.US);
    }

    private static boolean isIso2(String value) {
        return value != null && value.length() == 2;
    }
}
