package com.iot.nariz.utils;

/**
 * Created by armando.castillo on 28/02/2017.
 */

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

public class RESTClient
{
    public static JSONObject request(String requestURL, String params,
                                     boolean contentTypeJson, HashMap<String,String> headers, String method)
            throws Exception
    {
        URL url;
        String response = "";
        url = new URL(requestURL);
        JSONObject json = new JSONObject();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //conn.setReadTimeout(15000);
        //conn.setConnectTimeout(15000);
        conn.setRequestMethod(method);

        if(headers!=null)
        {
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();

                conn.setRequestProperty(key,value);

            }
        }

        if(contentTypeJson)conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");


        if(method.equalsIgnoreCase("POST")
                || method.equalsIgnoreCase("PUT")
                || method.equalsIgnoreCase("DELETE"))
        {
            conn.setDoOutput(true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
            bw.write(params);

            bw.flush();
            bw.close();
        }



        int responseCode=conn.getResponseCode();

        if ( responseCode == HttpsURLConnection.HTTP_OK
                || responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED
                || responseCode == HttpsURLConnection.HTTP_CONFLICT ) {

            BufferedReader br;
            String line;

            if(responseCode == HttpsURLConnection.HTTP_OK)
            {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            }
            else
            {
                br  =new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            if(br!=null)
            {
                while ((line=br.readLine()) != null)
                {
                    response += line;
                }
            }

        }
        else
        {
            response = "";
        }

        if(responseCode!=HttpsURLConnection.HTTP_OK)
        {
            if(responseCode == HttpsURLConnection.HTTP_CONFLICT)
            {
                response = "{\"Error\":\""+responseCode+"\"}";
            }
            else
            {
                response = "{\"Error\":\""+responseCode+"\"}";
            }
        }

        try
        {
            json = new JSONObject(response);
        }
        catch (Exception e)
        {
            System.out.println("WebServices http json object exception: " + e);
        }

        return json;
    }
}
