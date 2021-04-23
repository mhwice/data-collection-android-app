package mcmaster.ilos.datacollectionapp.Utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.MapDownloadReturnType;

/* Asynchronous class used to download a (GeoJSON) map file from the server.
 * The class computes the SHA256 hash of the file as it is downloaded.
 * In future iterations, this could be replaced by the FileSyncAPI */
public class DownloadMapFile extends AsyncTask<String, Void, MapDownloadReturnType> {

    private static final String REQUEST_METHOD = "GET";

    // Amount of time available to perform the download once connection is established
    private static final int READ_TIMEOUT = 10000;

    // Amount of time available to establish a connection
    private static final int CONNECTION_TIMEOUT = 3000;

    // Callback interface
    private BuildingManager.AsyncResponse delegate;

    public DownloadMapFile(BuildingManager.AsyncResponse asyncResponse) {
        // Assigning callback interface through constructor
        delegate = asyncResponse;
    }

    @Override
    protected MapDownloadReturnType doInBackground(String... params) {

        String stringUrl;
        String token;
        String buildingName;
        String floorNumber;
        String outputFilepath;
        String rowNumber;

        MapDownloadReturnType ret = new MapDownloadReturnType(false, params[5]);

        try {
            stringUrl = params[0];
            token = params[1];
            buildingName = params[2];
            floorNumber = params[3];
            outputFilepath = params[4];
            rowNumber = params[5];
        } catch (Exception e) {
            Log.e("CRASH", "Failed to set initial params", e);
            return ret;
        }

        HttpURLConnection connection = null;
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            URL myUrl = new URL(stringUrl);
            connection =(HttpURLConnection) myUrl.openConnection();
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            /* Add headers */
            connection.setRequestProperty("Authorization", "Token " + token);
            connection.setRequestProperty("building", buildingName);
            connection.setRequestProperty("floor", floorNumber);
            connection.connect();

            /* Get hash from header (on response) */
            String fileHash = connection.getHeaderField("Content-Disposition");

            /* Set path to save file to */
            String fileName = buildingName + "_" + floorNumber + ".geojson";
            File outputFile = new File(outputFilepath, fileName);

            /* Create SHA-256 algorithm */
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                Log.e("CRASH", "Exception while getting digest", e);
                return ret;
            } catch (Exception e) {
                Log.e("CRASH", "Exception while getting digest", e);
                return ret;
            }

            /* Read file in chunks. For each chunk update SHA256 hash and save the chunk to file */
            // MediaScannerConnection.scanFile(context, new String[]{outputFile.getAbsolutePath()}, null, null);
            fos = new FileOutputStream(outputFile);
            is = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int len1;
            try {
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                    digest.update(buffer, 0, len1);
                }
                byte[] hashsum = digest.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hashsum) {
                    sb.append(String.format("%02X", b));
                }
                String output = sb.toString().toLowerCase();
                if (fileHash.equalsIgnoreCase(output)) {
                    ret.setRowNumber(rowNumber);
                    ret.setSuccess(true);
                    return ret;
                } else {
                    boolean deleted = outputFile.delete();
                    if (!deleted) {
                        Log.e("CRASH", "Failed to remove corrupted file from map download");
                    }
                    return ret;
                }
            } catch (IOException e) {
                Log.e("CRASH", "Unable to process file for SHA256", e);
                return ret;
            } catch (Exception e) {
                Log.e("CRASH", "Unable to process file for SHA256", e);
                return ret;
            } finally {
                try {
                    is.close();
                    fos.close();
                    connection.disconnect();
                } catch (IOException e) {
                    Log.e("CRASH", "Exception on closing SHA input stream", e);
                } catch (Exception e) {
                    Log.e("CRASH", "Exception on closing SHA input stream", e);
                }
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to download map from server", e);
            return ret;
        } catch (Exception e) {
            Log.e("CRASH", "Failed to download map from server", e);
            return ret;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e("CRASH", "Failed to close readers after map file download failed", e);
            } catch (Exception e) {
                Log.e("CRASH", "Failed to close readers after map file download failed", e);
            }
        }
    }

    protected void onPostExecute(MapDownloadReturnType result) {
        super.onPostExecute(result);
        delegate.processFinish(result);
    }
}