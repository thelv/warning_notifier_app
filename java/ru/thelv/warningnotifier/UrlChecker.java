package ru.thelv.warningnotifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlChecker {
    public static final int SUCCESS = 0;
    public static final int ERROR= 1;
    public static final int INTERNET_UNAVAILABLE = 2;

    public static int checkUrl(String url) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            String response = connection.getResponseMessage();

            if (responseCode == 200 ) {// && "OK".equals(response)) {
                return SUCCESS;
            } else {
                return ERROR;
            }
        } catch (IOException e) {
            return ERROR;
        }
    }

    public static int checkUrlExt(String url, String urlGoogle)
    {
        int result = UrlChecker.checkUrl(url);

        if(result!=SUCCESS)
        {
            int resultGoogle=UrlChecker.checkUrl("https://www.google.com/");
            if(resultGoogle!=SUCCESS) result=INTERNET_UNAVAILABLE;
        }

        return result;
    }
} 