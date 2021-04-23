package mcmaster.ilos.datacollectionapp.LoginScreen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.Credentials;
import mcmaster.ilos.datacollectionapp.Utils.Config;
import mcmaster.ilos.datacollectionapp.Utils.InternetManager;

public class LoginManager {

    private static final short MIN_PASSWORD_LENGTH = 2;
    private static final short MAX_PASSWORD_LENGTH = 30;
    private static final short MIN_EMAIL_LENGTH = 2;
    private static final short MAX_EMAIL_LENGTH = 30;
    private Context context;

    public LoginManager(Context context) {
        this.context = context;
    }

    boolean isEmailValid(String email) {
        return email.length() > MIN_EMAIL_LENGTH && email.length() < MAX_EMAIL_LENGTH;
    }

    boolean isPasswordValid(String password) {
        return password.length() > MIN_PASSWORD_LENGTH && password.length() < MAX_PASSWORD_LENGTH && isAlphaNumeric(password);
    }

    boolean isValidUser(Credentials credentials) {

        /* Get url from app_config.properties */
        String urlEndpoint;
        try {
            urlEndpoint = Config.loadProperties(context).getProperty("LOGIN_URL");
        } catch (Exception e) {
            Log.e("CRASH", "Could not find login url", e);
            return false;
        }

        /* Make a GET request to the server, passing the entered credentials */
        LoginUser task = new LoginUser();
        String result;

        InternetManager internetManager = new InternetManager();
        if (internetManager.isNetworkAvailable(context)) {
            try {
                result = task.execute(urlEndpoint, credentials.getEmail(), credentials.getPassword()).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e("CRASH", "Failed to login user from server", e);
                return false;
            }
        } else {
            return false;
        }

        if (result == null) {
            return false;
        }

        /* Parse the JSON response */
        String status;
        String token;
        try {
            JSONObject json = new JSONObject(result);
            status = json.getString("status");
            token = json.getString("token");
        } catch (JSONException e) {
            Log.e("CRASH", "Faile to parse json response", e);
            return false;
        }

        /* Save token if user verified */
        if (status.equals("1")) {
            try {
                saveToken(token);
                return true;
            } catch (Exception e) {
                Log.e("CRASH", "Failed to save login token", e);
                return false;
            }
        } else {
            return false;
        }
    }

    private void saveToken(String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("token", token).apply();
    }

    public String loadToken() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }

    private boolean isAlphaNumeric(String s) {
        return s.matches("^[a-zA-Z0-9]*$");
    }

    void saveCredentials(Credentials credentials) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("email", credentials.getEmail()).apply();
        sharedPreferences.edit().putString("password", credentials.getPassword()).apply();
    }

    private Credentials loadCredentials() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        String retrievedEmail = sharedPreferences.getString("email", "");
        String retrievedPassword = sharedPreferences.getString("password", "");
        return new Credentials(retrievedEmail, retrievedPassword);
    }

    boolean isLoggedIn() {
        Credentials credentials;
        try {
            credentials = loadCredentials();
            return !credentials.getEmail().equals("") && !credentials.getPassword().equals("");
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load credentials from memory", e);
            return false;
        }
    }

    public void logout() {
        try {
            saveCredentials(new Credentials("", ""));
        } catch (Exception e) {
            Log.e("CRASH", "Failed to properly logout user", e);
        }
    }

    private static class LoginUser extends AsyncTask<String, Void, String> {

        private static final String REQUEST_METHOD = "POST";

        /* Amount of time available to perform the download once connection is established */
        private static final int READ_TIMEOUT = 10000;

        /* Amount of time available to establish a connection */
        private static final int CONNECTION_TIMEOUT = 3000;

        @Override
        protected String doInBackground(String... params) {

            String urlString;
            String email;
            String password;

            try {
                urlString = params[0];
                email = params[1];
                password = params[2];
            } catch (Exception e) {
                Log.e("CRASH", "No parameters passed to LoginUser task", e);
                return null;
            }

            HttpURLConnection connection = null;
            DataOutputStream os = null;

            try {
                /* Set-up connection to server */
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(REQUEST_METHOD);
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                /* Insert credentials into request body */
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("password", password);
                os = new DataOutputStream(connection.getOutputStream());
                os.writeBytes(jsonParam.toString());
                try {
                    return getText(connection);
                } catch (IOException e) {
                    Log.e("CRASH", "Failed to connect to server", e);
                    return null;
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to connect to server", e);
                    return null;
                } finally {
                    connection.disconnect();
                    os.flush();
                    os.close();
                }
            } catch (Exception e) {
                Log.e("CRASH", "Failed to interpret response from server", e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                } catch (IOException e) {
                    Log.e("CRASH", "Failed to close output stream", e);
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to close output stream", e);
                }
            }
        }
    }

    private static String getText(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        if (200 <= responseCode && responseCode <= 299) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String currentLine;
        while ((currentLine = in.readLine()) != null) {
            response.append(currentLine);
        }
        in.close();
        return response.toString();
    }
}
