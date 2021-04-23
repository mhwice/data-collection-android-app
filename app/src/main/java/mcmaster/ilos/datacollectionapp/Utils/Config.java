package mcmaster.ilos.datacollectionapp.Utils;

import android.content.Context;
import android.util.Log;

import java.util.Properties;

/* Used to load data from app_config.properties file */
public class Config {
    public static Properties loadProperties(Context context) {
        Properties props = new Properties();
        try {
            props.load(context.getAssets().open("app_config.properties"));
        } catch (Exception e) {
            Log.e( "Config", "", e);
        }
        return props;
    }
}
