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
 * Created by Chris on 9/5/2018.
 */
public class DataExtract {
    public static HashSet<String> conditions;
    public static HashSet<String> symptoms;
    public static PrintWriter writer;
    public static PrintWriter writer2;
    public static PrintWriter writer3;

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

    static class Worker extends Thread {

        public Element e2;

        @Override
        public void run() {
            Document doc3 = gethtml(e2.attr("href"));

            synchronized (writer2) {

                String symptoms_str = e2.text();

                if (symptoms.contains(symptoms_str)) return;

                System.out.println("New symptom set: " + symptoms_str);
                symptoms.add(symptoms_str);

                writer2.print(symptoms_str + "::");

                Elements links3 = doc3.select("ul.results_list li");

                for (Element e3 : links3) {
                    String condition = e3.select("a").text();
                    String condition_description = e3.select("p").text();

                    if (!conditions.contains(condition)) {
                        conditions.add(condition);
                        writer3.println(condition + "|" + condition_description);
                    }

                    writer2.print(condition + "|");
                }

                writer2.print("\n");

            }
        }
    }

    public static void extract() throws InterruptedException {
        Document doc = gethtml("https://symptomchecker.webmd.com/symptoms-a-z");

        writer = null;
        writer2 = null;
        writer3 = null;
        try {
            writer = new PrintWriter("symptoms.txt", "UTF-8");
            writer2 = new PrintWriter("conditions_symptoms.txt", "UTF-8");
            writer3 = new PrintWriter("conditions.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        conditions = new HashSet<>();
        symptoms = new HashSet<>();

        Elements links = doc.select("ol.alpha_list a");

        for(Element e : links) {
            writer.println(e.text());

            int page = 1;
            while (true) {
                System.out.println(e.text() + " -- page " + page);

                Document doc2 = gethtml(e.attr("href") + "&page=" + page);

                Elements links2 = doc2.select("table.results_table a");

                if (links2 == null || links2.size() == 0) break;

                Worker[] workers = new Worker[links2.size()];
                int finished = 0;
                int threads = 70;
                while (finished < links2.size()) {
                    for(int j=0;j<threads && finished+j<links2.size();j++) {
                        workers[j] = new Worker();
                        workers[j].e2 = links2.get(finished+j);
                        workers[j].start();
                    }

                    for(int j=0;j<links2.size();j++) {
                        if(workers[j]!=null) workers[j].join();
                    }

                    finished+=threads;
                }

                page++;
            }
        }

        writer.close();
        writer2.close();
        writer3.close();
    }

    public static void create_train_data() throws FileNotFoundException, UnsupportedEncodingException {
        HashMap<String, Integer> symptoms_ids = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("symptoms.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                symptoms_ids.put(line, symptoms_ids.size()+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, Integer> conditions_ids = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("conditions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                conditions_ids.put(p[0], conditions_ids.size()+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        PrintWriter w = new PrintWriter("data.txt", "UTF-8");

        try (BufferedReader br = new BufferedReader(new FileReader("conditions_symptoms.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("::");

                String[] s = p[0].split(", ");

                if(p.length==1) continue;

                String[] c = p[1].split("\\|");

                HashSet<Integer> ss = new HashSet<>();
                for(String symptom : s) {
                    ss.add(symptoms_ids.get(symptom));
                }

                HashSet<Integer> cc = new HashSet<>();
                for(String condition : c) {
                    cc.add(conditions_ids.get(condition));
                }

                for(int i=1;i<=symptoms_ids.size();i++) {
                    if(ss.contains(i)) {
                        w.print("1");
                    } else {
                        w.print("0");
                    }
                    if(i<symptoms_ids.size()) {
                        w.print(",");
                    }
                }

                w.print("|");

                for(int i=1;i<=conditions_ids.size();i++) {
                    if(cc.contains(i)) {
                        w.print("1");
                    } else {
                        w.print("0");
                    }
                    if(i<conditions_ids.size()) {
                        w.print(",");
                    }
                }

                w.print("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        w.close();
    }

    public static void create_train_data2() throws FileNotFoundException, UnsupportedEncodingException {
        HashMap<String, Integer> symptoms_ids = new HashMap<>();

        //HashSet<String> symptoms = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader("symptoms.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                symptoms_ids.put(line, symptoms_ids.size()+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, Integer> conditions_ids = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("conditions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                conditions_ids.put(p[0], conditions_ids.size()+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        PrintWriter w = new PrintWriter("data_s.txt", "UTF-8");

        try (BufferedReader br = new BufferedReader(new FileReader("conditions_symptoms.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("::");

                String[] s = p[0].split(", ");

                if(p.length==1) continue;

                HashSet<Integer> ss = new HashSet<>();
                for(String symptom : s) {
                    ss.add(symptoms_ids.get(symptom));
                    //symptoms.add(symptom);
                }

                for(String symptom : s) {
                    int id = symptoms_ids.get(symptom);

                    for(int i=1;i<=symptoms_ids.size();i++) {
                        if(i==id) {
                            w.print("1");
                        } else {
                            w.print("0");
                        }
                        if(i<symptoms_ids.size()) {
                            w.print(",");
                        }
                    }

                    w.print(",");

                    for(int i=1;i<=symptoms_ids.size();i++) {
                        if(ss.contains(i) && i!=id) {
                            w.print("1");
                        } else {
                            w.print("0");
                        }
                        if(i<symptoms_ids.size()) {
                            w.print(",");
                        }
                    }

                    w.print("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        w.close();

        /*
        PrintWriter w2 = new PrintWriter("symptoms.txt", "UTF-8");
        for(String s : symptoms) {
            w2.println(s);
        }
        w2.close();
        */
    }

    public static void getSymptoms() throws FileNotFoundException, UnsupportedEncodingException {
        HashMap<String, HashSet<String>> cs = new HashMap<>();

        PrintWriter w = new PrintWriter("conditions_symptoms_2.txt", "UTF-8");

        try (BufferedReader br = new BufferedReader(new FileReader("conditions_symptoms.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("::");

                String[] s = p[0].split(", ");

                if(p.length==1) continue;

                String[] c = p[1].split("\\|");

                for(String condition : c) {
                    for(String symptom : s) {
                        if(!cs.containsKey(condition)) {
                            cs.put(condition, new HashSet<String>());
                        }
                        cs.get(condition).add(symptom);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Map.Entry e : cs.entrySet()) {
            w.print(e.getKey()+"::");
            HashSet<String> s = (HashSet<String>) e.getValue();
            for(String symptom : s) {
                w.print(symptom+"|");
            }
            w.print("\n");
        }

        w.close();
    }


    public static void getInfo() {

        try (BufferedReader br = new BufferedReader(new FileReader("conditions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");

                Document doc = gethtml("https://www.nhs.uk/search?collection=nhs-meta&query="+p[0]);

                //url
                if(doc.select("ul[id=search-results] li a").first()==null) {
                    Database.updateQuery("INSERT INTO conditions SET name = '"+ StringEscapeUtils.escapeSql(p[0])+"', overview = '"+StringEscapeUtils.escapeSql(p[1])+"';");
                    continue;
                }

                String url = "";
                for(Element eu : doc.select("ul[id=search-results] li a")) {
                    url = eu.attr("title");
                    if(url.contains("conditions/") && !url.endsWith(".pdf")) {
                        String[] ps = url.replace("https://www.nhs.uk/","").split("/");
                        if(ps[1].equals("Pages")) continue;
                        url = "https://www.nhs.uk/conditions/"+ps[1]+"/";
                        break;
                    }
                }
                System.out.println("URL: \n"+url);

                if(!url.contains("conditions/") || url.endsWith(".pdf")) {
                    Database.updateQuery("INSERT INTO conditions SET name = '"+StringEscapeUtils.escapeSql(p[0])+"', overview = '"+StringEscapeUtils.escapeSql(p[1])+"';");
                    continue;
                }

                doc = gethtml(url);

                //name
                String name = doc.select("h1.local-header__title").first().text().trim();
                System.out.println("NAME: "+name);

                //overview
                String overview = doc.select("div.article section").first().text();
                System.out.println("OVERVIEW: \n"+overview);

                //when to see a doctor
                String when_doctor = "";
                Element e = doc.select("div.article section[id=when-to-see-a-doctor]").first();
                if(e!=null) {
                    e.select("h2").first().remove();
                    when_doctor = e.text();
                }
                System.out.println("WHEN TO SEE A DOCTOR: \n"+when_doctor);

                //living with
                String living_with = "";
                e = doc.select("div.article section[id*=living-with]").first();
                if(e!=null) {
                    e.select("h2").first().remove();
                    living_with = e.text();
                }
                System.out.println("LIVING WITH: \n"+living_with);

                doc = gethtml(url+"symptoms/");

                //symptoms
                String symptoms = "";
                if(doc!=null) {
                    for(Element e2 : doc.select("div.article section").first().select("li")) {
                        symptoms += e2.text().trim().replace(",", "").replaceAll("\u00A0","") + ", ";
                    }
                    if(doc.select("div.article section[id*=symptoms] li")!=null) {
                        for(Element e2 : doc.select("div.article section[id*=symptoms] li")) {
                            symptoms += e2.text().trim().replace(",", "").replaceAll("\u00A0","") + ", ";
                        }
                    }
                    if(doc.select("div.article section[id*=signs] li")!=null) {
                        for(Element e2 : doc.select("div.article section[id*=signs] li")) {
                            symptoms += e2.text().trim().replace(",", "").replaceAll("\u00A0","") + ", ";
                        }
                    }
                }

                System.out.println("SYMPTOMS: \n"+symptoms);

                //when to seek help
                String when_seek_help = "";
                if(doc!=null) {
                    e = doc.select("div.article section[id=when-to-seek-help] p").first();
                    if(e!=null) {
                        when_seek_help = e.text();
                    }
                }

                System.out.println("WHEN TO SEEK HELP: \n"+when_seek_help);

                doc = gethtml(url+"treatment/");
                String treating = "";
                if(doc!=null) {
                    treating = doc.select("div.article section").first().text();
                }
                System.out.println("TREATING: "+treating);

                Database.updateQuery("INSERT INTO conditions SET " +
                        "name = '"+ StringEscapeUtils.escapeSql(name)+"', " +
                        "overview = '"+StringEscapeUtils.escapeSql(overview)+"', " +
                        "symptoms = '"+StringEscapeUtils.escapeSql(symptoms)+"', " +
                        "when_seek_help = '"+StringEscapeUtils.escapeSql(when_seek_help)+"', " +
                        "when_see_doctor = '"+StringEscapeUtils.escapeSql(when_doctor)+"', " +
                        "treating = '"+StringEscapeUtils.escapeSql(treating)+"', " +
                        "living_with = '"+StringEscapeUtils.escapeSql(living_with)+"';");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException {
        //extract();
        //create_train_data2();
        //getSymptoms();
        getInfo();
    }

    private static Document gethtml(String url) {
        if(!url.startsWith("http")) {
            url = "https://symptomchecker.webmd.com/" + url;
        }
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
