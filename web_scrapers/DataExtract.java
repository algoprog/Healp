package com.algoprog.newzard.diagnosis;

import org.cyberneko.html.HTMLElements;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Chris on 9/5/2018.
 */
public class DataExtract {
    public static HashSet<String> conditions;
    public static HashSet<String> symptoms;
    public static PrintWriter writer;
    public static PrintWriter writer2;
    public static PrintWriter writer3;

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

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, InterruptedException {
        extract();
        //create_train_data();
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
            e.printStackTrace();
        }
        return null;
    }
}
