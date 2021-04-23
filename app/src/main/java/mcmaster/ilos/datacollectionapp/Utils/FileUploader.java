package mcmaster.ilos.datacollectionapp.Utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.SaveFile;

/* Class responsible for uploading PBF files to the server. Includes a listener which updates progress and onFinish() event */
public class FileUploader {

    // TIME_OUT is set to 3 min (3min * 60sec * 1000ms)
    private static final int TIME_OUT = 3 * 60 * 1000;
    private static final String CHARSET = "utf-8";
    private static final String PREFIX = "--";
    private static final String LINE_END = "\r\n";
    private static final String CONTENT_TYPE = "multipart/form-data";
    private static final String BOUNDARY = UUID.randomUUID().toString();

    public static void upload(String host, File file, Map<String,String> params, SaveFile saveFile, FileUploadListener listener) {

        HttpURLConnection connection = null;
        InputStream is = null;
        DataOutputStream dos = null;
        BufferedReader br = null;

        try {
            URL url = new URL(host);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(TIME_OUT);
            connection.setConnectTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Charset", CHARSET);
            connection.setRequestProperty("connectionection", "keep-alive");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
            connection.setRequestProperty("Authorization", params.get("Authorization"));
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            if (file != null) {
                dos = new DataOutputStream(connection.getOutputStream());
                StringBuilder sb = new StringBuilder();
                sb.append(LINE_END);
                if (!params.isEmpty()) {
                    for (Map.Entry<String, String> entry: params.entrySet()) {
                        sb.append(PREFIX);
                        sb.append(BOUNDARY);
                        sb.append(LINE_END);
                        sb.append("Content-Disposition: form-data; name=\"");
                        sb.append(entry.getKey());
                        sb.append("\"");
                        sb.append(LINE_END);
                        sb.append("Content-Type: text/plain; charset=");
                        sb.append(CHARSET);
                        sb.append("Content-Transfer-Encoding: 8bit");
                        sb.append(LINE_END);
                        sb.append(LINE_END);
                        sb.append(entry.getValue());
                        sb.append(LINE_END);
                    }
                }
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINE_END);
                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
                sb.append(file.getName());
                sb.append("\"");
                sb.append(LINE_END);
                sb.append("Content-Type: application/octet-stream; charset=");
                sb.append(CHARSET);
                sb.append(LINE_END);
                sb.append(LINE_END);

                /* Write file info to the stream */
                dos.write(sb.toString().getBytes());
                is = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                long totalbytes = file.length();
                long curbytes = 0;
                int len;

                while ((len = is.read(bytes)) != -1) {
                    curbytes += len;
                    dos.write(bytes, 0, len);
                    listener.onProgress(curbytes,1.0d * curbytes / totalbytes);
                }

                is.close();
                dos.write(LINE_END.getBytes());

                byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
                dos.write(end_data);
                dos.flush();

                /* Get Response */
                int code = connection.getResponseCode();
                sb.setLength(0);
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                listener.onFinish(code, sb.toString(), connection.getHeaderFields(), saveFile);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to upload datapack", e);
            listener.onFail();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (is != null) {
                    is.close();
                }
                if (dos != null) {
                    dos.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e("CRASH", "Failed to close readers after data pack upload", e);
                listener.onFail();
            } catch (Exception e) {
                Log.e("CRASH", "Failed to close readers after data pack upload", e);
                listener.onFail();
            }
        }
    }

    public interface FileUploadListener {
        void onFail();
        void onProgress(long pro, double percent);
        void onFinish(int code, String res, Map<String, List<String>> headers, SaveFile saveFile);
    }
}