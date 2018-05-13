package com.algoprog.healp;

import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Sotiris Papadopoulos on 13/5/2018.
 */



public class CaloriesExtract {

    public static HashSet<FoodEntry> foods;

    public static void init() {

        if(!Database.connect()) {
            System.out.println("Please check your MySQL connection info in the conf file");
            System.exit(1);
        }

        trustSSL();

        //RedwoodConfiguration.current().clear().apply();
    }

    private static void trustSSL() {
        // Create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        // Activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignored) {}
    }



    public static void extract() {
        Document doc = gethtml("http://www.calories.info/");

        foods = new HashSet<>();

        Elements links = doc.select("ul#menu-calorie-tables a");

        for(Element e : links) {
            //writer.println(e.text());

            Document doc2 = gethtml(e.attr("href"));

            Elements rows = doc2.select("table#calories-table tr.kt-row");
            System.out.println(rows.size());

            for (Element row : rows) {
                FoodEntry fe = new FoodEntry();
                fe.food_name = row.select("td.food").text();
                fe.serving = row.select("td.serving.portion").text();
                fe.calories = row.select("td.kcal.portion").text();
                fe.kilojoule = row.select("td.kj.portion").text();
                foods.add(fe);
                System.out.println(fe.food_name+"\t||\t"+fe.serving+"\t||\t"+fe.calories+"\t||\t"+fe.kilojoule);
            }
        }

    }

    private static class FoodEntry {
        public String food_name, serving, calories, kilojoule;
    }




    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException {
        extract();
    }

    private static Document gethtml(String url) {
        try {
            return Jsoup.connect(url).
                    userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36")
                    .header("Accept-Language", "en")
                    .followRedirects(true)
                    .timeout(40000)
                    .get();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return null;
    }
}
