package com.vincios.musicstreamer2.connectors.pleer;

import android.util.Log;

import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.pleer.token.ExpiredTokenException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

class PleerConnection {


    private static final String LOGTAG = "PleerConnection";

    public JSONObject executeRequest(Map<String, String> params) throws PleerException {

        String methodInvocated = params.get(PleerConstants.POST_KEY_METHOD);
        String connectionURL;
        boolean connectionSuccess = false;
        JSONObject response;

        URL url;
        HttpURLConnection conn = null;

        if (methodInvocated.equals(PleerConstants.METHOD_GET_TOKEN))
            connectionURL = PleerConstants.TOKEN_URL;
        else
            connectionURL = PleerConstants.REQUEST_URL;

        try {
            url = new URL(connectionURL);

            Log.d(LOGTAG, "Trying connection to url " + url.toString());

            conn = (HttpURLConnection) url.openConnection();

            Log.d(LOGTAG, "Connection success");
            Log.d(LOGTAG, "Setting connection properties");
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");

            Log.d(LOGTAG, "Trying server connection");
            conn.connect();
            connectionSuccess = true;
            Log.d(LOGTAG, "Connection success");
        } catch (ProtocolException e) {
            Log.d(LOGTAG, "Error on setting POST property: " + e.getMessage());
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_REQUEST_PREPARATION);
        } catch (MalformedURLException e) {
            Log.d(LOGTAG, "Error malformed URL: " + connectionURL);
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_REQUEST_PREPARATION);
        } catch (IOException e) {
            Log.d(LOGTAG, "Error on server connection: " + e.getMessage());
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_SERVER_CONNECTION);
        }finally {
            if( !connectionSuccess && conn != null){
                conn.disconnect();
            }
        }


        try {
            Log.d(LOGTAG, "Trying to sending request");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            String toSend = prepareRequest(params);
            out.write(toSend);
            out.close();
            Log.d(LOGTAG, "Sending success");
        } catch (UnsupportedEncodingException e) {
            Log.d(LOGTAG, "Error on encoding: " + e.getMessage());
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_REQUEST_PREPARATION);
        } catch (IOException e) {
            Log.d(LOGTAG, "Error on sending request: " + e.getMessage());
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_SEND);
        }


        BufferedReader in = null;

        try{
            Log.d(LOGTAG, "Trying to receive response");
            StringBuilder sb = new StringBuilder();
            String line;
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while((line = in.readLine()) != null){
                sb.append(line);
            }

            String result = sb.toString();

            response = new JSONObject(result);

            Log.d(LOGTAG, result);
            if(result.contains("invalid_token"))
                throw new ExpiredTokenException(response.getString(PleerConstants.RESPONSE_JSON_ERROR_DESCRIPTION));
                //throw new PleerException(response.getString(PleerConstants.RESPONSE_JSON_ERROR_DESCRIPTION), PleerConstants.RESPONSE_INVALID_TOKEN);
            else if(result.contains("\"success\":false")) {
                String message = response.getString(PleerConstants.RESPONSE_JSON_INSUCCESS_MESSAGE);
                throw new PleerException(message, ConnectorException.RESPONSE_INSUCCESS);
            }else if(result.contains("\"error\"")) {
                String error = response.getString(PleerConstants.RESPONSE_JSON_ERROR);
                String errorDescr = response.getString(PleerConstants.RESPONSE_JSON_ERROR_DESCRIPTION);
                throw new PleerException(error + ": " + errorDescr, ConnectorException.RESPONSE_INSUCCESS);
            }

            Log.d(LOGTAG, "Received!");
        } catch (IOException | JSONException e) {
            Log.d(LOGTAG, "Error on receive response: " + e.getMessage());
            e.printStackTrace();
            throw new PleerException(e.getMessage(), e, ConnectorException.ERROR_RESPONSE_RECEIVE);
        } finally {
            try {
                if(in != null)
                    in.close();

                conn.disconnect();
            } catch (Exception ignore) {

            }
        }


        return response;
    }

    private String prepareRequest(final Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = params.keySet().iterator();

        while (iterator.hasNext()){
            String key = iterator.next();
            String value = params.get(key);
            sb.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(value, "UTF-8"))
                    .append("&");
        }

        return sb.toString();
    }

}
