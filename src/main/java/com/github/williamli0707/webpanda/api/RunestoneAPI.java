package com.github.williamli0707.webpanda.api;

import com.github.sisyphsu.dateparser.DateParserUtils;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.helger.commons.csv.CSVWriter;
import com.helger.commons.string.util.LevenshteinDistance;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunestoneAPI {
    private static OkHttpClient client, noRedirectClient;
    private static OkHttpClient client2;
    private static CustomCookieJar cookiejar;

    private static void initClients() {
        cookiejar = new CustomCookieJar();
        client = new OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(cookiejar)
                .build();
        noRedirectClient = client.newBuilder()
                .followRedirects(false)
                .cookieJar(cookiejar)
                .build();
        client2 = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();
    }
    public static void reset() {
        initClients();
        try {
            resetCookie();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initNameCache();
        initProblemCache();
    }

    private final static Hashtable<String, String> studentnamescache = new Hashtable<>();
    private final static Hashtable<String, String[]> problemnamescache = new Hashtable<>();
    private final static Hashtable<String, String> defaultcodetemplatecache = new Hashtable<>();
    private static String cookie, access_token, session_id;
    private static final String loginURL = "https://runestone.academy/user/login?_next=/runestone/admin";
    private static final String gradeURL = "https://runestone.academy/ns/assessment/gethist";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern pform = Pattern.compile("<input name=\"_formkey\" type=\"hidden\" value=\"(.*?)\" />"),
                                 psessionid = Pattern.compile("session_id_runestone=(.*?);"),
                                 pproblems = Pattern.compile("<script type=\"application/json\" id=\"getassignmentinfo\">([\\s\\S]*?)</script>"),
                                 paccess_token = Pattern.compile("access_token=(.*?);");


    static {
        reset();
//        System.out.println("follow redirects? " + client.followRedirects());
//        cookiejar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor());
    }

    public RunestoneAPI() {

    }

    public RunestoneAPI(String names) {
        this();
        JSONObject respjson = new JSONObject(names);
        for (Map.Entry<String, Object> entry : respjson.toMap().entrySet()) {
            studentnamescache.putIfAbsent(entry.getKey(), (String) entry.getValue());
        }
    }

    private static void initProblemCache() {
        try {
            Response resp = client.newCall(new Request.Builder()
                    .url("https://runestone.academy/runestone/admin/grading")
                    .get()
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .build()
            ).execute();
            Matcher m = pproblems.matcher(resp.body().string()); //closed
            if(!m.find()) throw new IOException("Could not find problems");
            JSONObject problems = new JSONObject(m.group(1));
            for(String set: problems.keySet()) {
                JSONArray problemset = problems.getJSONArray(set);
                problemnamescache.put(set, new String[problemset.length()]);
                int ind = 0;
                for(Object problem: problemset) {
                    String probname = (String) problem;
                    if(probname.endsWith("+")) probname = probname.substring(0, probname.length() - 1);
                    problemnamescache.get(set)[ind++] = probname;
                }
//                System.out.println(Arrays.toString(problemnamescache.get(set)));
            }
//            System.out.println(problemnamescache);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initNameCache() {
        Hashtable<String, String> newnames = new Hashtable<>();

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create("", mediaType);
        Request request = new Request.Builder()
                .url("https://runestone.academy/runestone/admin/course_students")
                .method("POST", body)
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Length", "0")
                .addHeader("Cookie", cookie)
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"107\", \"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build();

        String resp;

        try (Response response = client.newCall(request).execute()) {
            resp = response.body().string(); //closed
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JSONObject respjson = new JSONObject(resp);

        for (Map.Entry<String, Object> entry : respjson.toMap().entrySet()) {
            if (studentnamescache.putIfAbsent(entry.getKey(), (String) entry.getValue()) == null) {
                newnames.put(entry.getKey(), (String) entry.getValue());
            }
        }
    }

    public Hashtable<String, String> getNames() {
        return studentnamescache;
    }

    public Hashtable<String, String[]> getProblems() {
        return problemnamescache;
    }

    public static void resetCookie() throws IOException {
        Response response = noRedirectClient.newCall(new Request.Builder()
                .url(new URL("https://runestone.academy/"))
                .get()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
//                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Connection", "keep-alive")
                .addHeader("Host", "runestone.academy")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "none")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
                .build()
        ).execute();

        Matcher match = psessionid.matcher(Objects.requireNonNull(response.header("Set-Cookie")));
        if(!match.find()) throw new IOException("Could not find session id");
        String sessionID = match.group(1);
        response.close(); //closed

        cookie = "session_id_runestone=" + sessionID + "; " +
                "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd;" +
//                "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDgzODI2ODB9.xKWZFNYVtTzYe-106qBtsFsKvWy8oUVXSx7gHxFlVSs; " +
//                "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDg4OTEyNTZ9.6_yRcDx1sbo48xHi5EQqwFzP3TWZeKOnj6IR-COfXgw;" +
                //TODO check if access token works on Feburary 19, 2024
                "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; " +
                "RS_info=\"{\\\"tz_offset\\\": 8.0}\"";

        response = noRedirectClient.newCall(new Request.Builder()
                .url(loginURL)
                .get()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
//                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Connection", "keep-alive")
                .addHeader("Host", "runestone.academy")
                .addHeader("Referer", "https://landing.runestone.academy/")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "same-site")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
                .build()
        ).execute();
//        System.out.println(response.body().string());
        match = pform.matcher(response.body().string()); //closed
        if(!match.find()) throw new IOException("Could not find form key");
        String formkey = match.group(1);

        RequestBody body = new FormBody.Builder()
                .addEncoded("username", "wli223")
                .addEncoded("password", "***REMOVED***")
                .addEncoded("_next", "/runestone/admin")
                .addEncoded("_formkey", formkey)
                .addEncoded("_formname", "login")
        .build();

        //Login request
        response = client.newCall(new Request.Builder()
                .url(loginURL)
                .post(body)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
//                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Connection", "keep-alive")
                //Content-Length, Content-Type ??
                .addHeader("Content-Length", "1000")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Host", "runestone.academy")
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/user/login?_next=/runestone/admin")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
                .build()
        ).execute();

        response.close(); //closed

        response = noRedirectClient.newCall(new Request.Builder()
                .url("https://runestone.academy/")
                .get().build()).execute();
        match = paccess_token.matcher(Objects.requireNonNull(response.header("Set-Cookie")));
        if(!match.find()) throw new IOException("Could not find access token");
//        System.out.println(response.body().string());
        cookie = "session_id_runestone=" + sessionID + "; " +
                "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd;" +
                "access_token=" + match.group(1) + ";" +
                "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; " +
                "RS_info=\"{\\\"tz_offset\\\": 8.0}\"";
        response.close();
    }

    public static String getName(String sid) {
        return studentnamescache.get(sid);
    }

    public int requestGrade(String sid, String pid) {
        String content = (String) request(new Request.Builder()
                .url("https://runestone.academy/runestone/admin/getGradeComments?acid=" + pid + "&sid=" + sid)
                .get()
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", cookie)
                .addHeader("DNT", "1")
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"105\", \"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"105\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build());
//        System.out.println(content);
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("test.txt")));
            pw.write(content);
            pw.close();
        } catch(Exception ignored) {}
        return new JSONObject(content).getInt("grade");
    }

    public LinkedList<Attempt> requestHistory(String sid, String pid) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        JSONObject body = new JSONObject().put("acid", pid).put("sid", sid);
        RequestBody reqbody = RequestBody.create(body.toString(), mediaType);

        Request request = new Request.Builder()
                .url("https://runestone.academy/ns/assessment/gethist")
                .post(reqbody)
                .addHeader("Accept", "application/json")
                //ignore accept-encoding
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cookie", "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd; __utmz=28105279.1699043992.22.15.utmcsr=landing.runestone.academy|utmccn=(referral)|utmcmd=referral|utmcct=/; session_id_runestone=52245321:fe40ec64-0315-4bcf-9d1a-8793213d7c94; access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDg4OTEyNTZ9.6_yRcDx1sbo48xHi5EQqwFzP3TWZeKOnj6IR-COfXgw; __utma=28105279.603586017.1694036376.1699766648.1699819267.25; RS_info=\"{\\\"readings\\\": []\\054 \\\"tz_offset\\\": 8.0}\"; __utmt=1; __utmb=28105279.3.10.1699819267")
                .addHeader("Host", "runestone.academy")
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"")
                .addHeader("Sec-Ch-Ua-Mobile", "?0")
                .addHeader("Sec-Ch-Ua-Platform", "\"macOS\"")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .addHeader("accept", "application/json")
                .build();

        String resp = (String) request(request);

        JSONObject respjson = new JSONObject(resp).getJSONObject("detail");
        JSONArray history = respjson.getJSONArray("history");
        JSONArray timestamps = respjson.getJSONArray("timestamps");

        Iterator<Object> historyiter = history.iterator();
        Iterator<Object> timestampsiter = timestamps.iterator();

        LinkedList<Attempt> sortlist = new LinkedList<>();

        while (historyiter.hasNext()) {
            // we opt to use DateParserUtils because I'm too lazy to convert dates.
            sortlist.add(new Attempt(DateParserUtils.parseDate(timestampsiter.next().toString()).getTime(), (String) historyiter.next(), 0));
        }
        sortlist.sort(Attempt::compareTo);

//        HashMap<Attempt, Integer> returnMap = new HashMap<>();
//
//        int index = 2; // lines up with runestone viewer
//        for (Attempt a : sortlist) {
////            returnMap.put(new Attempt(a.timestamp(), a.code(), index++), requestGrade(sid, pid));
//            returnMap.put(new Attempt(a.timestamp(), a.code(), index++), 0);
//        }
//        return returnMap;

        LinkedList<Attempt> returnlist = new LinkedList<>();

        int index = 2; // lines up with runestone viewer
        for (Attempt a : sortlist) {
            returnlist.add(new Attempt(a.timestamp(), a.code(), index++));
        }

        return returnlist;
    }

    private Object request(Request request) { // no param 3 retries
        return request(request, 3);
    }

    private Object request(Request request, int retries) {
        if (retries == 0) {
            throw new RuntimeException(" requests failed for " + request.url());
        }

        try (Response resp = client2.newCall(request).execute()) {
            String out = resp.body().string();

            resp.close();

            return out;
        } catch (Exception e) {
            return request(request, retries - 1);
        }
    }

    public void writeStudentData(String pid, String sid, String path) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(path));
            LinkedList<Attempt> l = requestHistory(sid, pid);
            String[] r1 = new String[l.size()], r2 = new String[l.size()], r3 = new String[l.size()];
            String prev = "";
            int ind = 0;
            for (Attempt i : l) {
                r1[ind] = i.code();
                r2[ind] = String.valueOf(i.timestamp());
                r3[ind] = String.valueOf(LevenshteinDistance.getDistance(prev, i.code()));
                ind++;
                prev = i.code();
            }
            writer.writeNext(r1);
            writer.writeNext(r2);
            writer.writeNext(r3);
            writer.close();
        }
        catch (Exception ignored) {}
    }

    //TODO
    public void getStudentData(String pid, String sid) {
        LinkedList<Attempt> l = requestHistory(sid, pid);
        String[] r2 = new String[l.size()], r3 = new String[l.size()];
        String prev = "";
        int ind = 0;
        for (Attempt i : l) {
            r2[ind] = String.valueOf(i.timestamp());
            r3[ind] = String.valueOf(LevenshteinDistance.getDistance(prev, i.code()));
            ind++;
            prev = i.code();
        }
    }

    /**
     * Returns the edit distance / second * 10000 metric for all students for a given problem.
     * @param pid the problem id of the problem to be analyzed
     * @return a HashMap with the key values of student ID's and values of metrics, average and maximum in that order.
     */
    public HashMap<String, double[]> getAllScores(String pid) {
        Hashtable<String, String> names = getNames();
        HashMap<String, double[]> scores = new HashMap<>();
        for (String key : names.keySet()) {
//            System.out.println(names.get(key) + " (" + key + ")");
            LinkedList<Attempt> history = requestHistory(key, pid);
//            System.out.println(history);
            double min = 0, max = 0, sum = 0, num = 0;
            Attempt prev = null;
//            System.out.println("num submissions: " + history.size());
            for (Attempt attempt : history) {
                num++;
                if(num == 1) {
                    prev = attempt;
                    continue;
                }
                double diff = 1000d * LevenshteinDistance.getDistance(prev.code(), attempt.code()) / (attempt.timestamp() - prev.timestamp());
//                System.out.println("dist: " + LevenshteinDistance.getDistance(prev.code(), attempt.code()) + " time: " + (attempt.timestamp() - prev.timestamp()) + " diff: " + diff);
                min = Math.min(min, diff);
                max = Math.max(max, diff);
                sum += diff;
                prev = attempt;
            }
            if(num == 1) {
//                System.out.println("N/A - one submission");
                continue;
            }
            sum /= (num - 1);
//            System.out.printf("Average edit distance per second: %.5f \n", sum);
//            System.out.printf("Maximum edit distance per second: %.5f \n", max);
            scores.put(key, new double[]{sum, max});
        }
        return scores;
    }

    /**
     * Returns the edit distance / second * 1000 metric for all students for given problems.
     * @param pids the problem IDs of the problem to be analyzed
     * @return a HashMap with the key values of student ID's and values of metrics, average and maximum in that order.
     */
    public HashMap<String, ArrayList<Double>> getMultipleProblems(String[] pids) {
        Hashtable<String, String> names = getNames();
        HashMap<String, ArrayList<Double>> scores = new HashMap<>();
        for (String key : names.keySet()) scores.put(key, new ArrayList<>());
        for(String i: pids) {
//            System.out.println("Problem: " + i);
            for (String key : names.keySet()) {
//            System.out.println(names.get(key) + " (" + key + ")");
                LinkedList<Attempt> history = requestHistory(key, i);
//            System.out.println(history);
                double min = 0, max = 0, sum = 0, num = 0;
                Attempt prev = null;
//            System.out.println("num submissions: " + history.size());
                for (Attempt attempt : history) {
                    num++;
                    if(num == 1) {
                        prev = attempt;
                        continue;
                    }
                    double diff = 1000d * LevenshteinDistance.getDistance(prev.code(), attempt.code()) / (attempt.timestamp() - prev.timestamp());
//                System.out.println("dist: " + LevenshteinDistance.getDistance(prev.code(), attempt.code()) + " time: " + (attempt.timestamp() - prev.timestamp()) + " diff: " + diff);
                    min = Math.min(min, diff);
                    max = Math.max(max, diff);
                    sum += diff;
                    prev = attempt;
                    scores.get(key).add(diff);
                }
                if(num == 1) {
//                    System.out.println("\tN/A - one submission");
                    continue;
                }
            }
        }
        return scores;
    }

    public HashMap<String, ArrayList<Double>> getMultipleProblemsND(String[] pids) {
        Hashtable<String, String> names = getNames();
        HashMap<String, ArrayList<Double>> scores = new HashMap<>();
        for (String key : names.keySet()) scores.put(key, new ArrayList<>());
        for(String i: pids) {
//            System.out.println("Problem: " + i);
            for (String key : names.keySet()) {
//                System.out.println(names.get(key) + " (" + key + ")");
                LinkedList<Attempt> history = requestHistory(key, i);
//            System.out.println(history);
                double min = 0, max = 0, sum = 0, num = 0;
                Attempt prev = null;
//            System.out.println("num submissions: " + history.size());
                for (Attempt attempt : history) {
                    num++;
                    if(num == 1) {
                        prev = attempt;
                        continue;
                    }
                    double diff = LevenshteinDistance.getDistance(prev.code(), attempt.code());
//                System.out.println("dist: " + LevenshteinDistance.getDistance(prev.code(), attempt.code()) + " time: " + (attempt.timestamp() - prev.timestamp()) + " diff: " + diff);
                    min = Math.min(min, diff);
                    max = Math.max(max, diff);
                    sum += diff;
                    prev = attempt;
                    scores.get(key).add(diff);
                }
                if(num == 1) {
//                    System.out.println("\tN/A - one submission");
                    continue;
                }
            }
        }
        return scores;
    }

    public HashMap<String, LinkedList<Attempt>> getAllCode(String pid) {
        Hashtable<String, String> names = getNames();
        HashMap<String, LinkedList<Attempt>> ret = new HashMap<>();
        for (String key : names.keySet()) {
//            System.out.println(names.get(key) + " (" + key + ")");
            ret.put(key, requestHistory(key, pid));
        }
        return ret;
    }

    public HashMap<String, LinkedList<Attempt>> getAllCode(String pid, Callback callback, int currPercent, int nextPercent) {
        Hashtable<String, String> names = getNames();
        HashMap<String, LinkedList<Attempt>> ret = new HashMap<>();
        int numStudents = 0;
        for (String key : names.keySet()) {
            callback.call((int) (currPercent + (numStudents++ * 1.0 * (nextPercent - currPercent) / names.size())), "Getting data for problem " + pid);
//            System.out.println(names.get(key) + " (" + key + ")");
            ret.put(key, requestHistory(key, pid));
        }
        return ret;
    }

    public HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> getAllCodeMultiple(Callback callback, String... pids) {
        return getAllCodeMultiple(callback, Arrays.asList(pids));
    }

    public HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> getAllCodeMultiple(Callback callback, Collection<String> pids) {
        HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> ret = new LinkedHashMap<>(); //sid: pid: attempts
        int ind = 0;
        for(String pid: pids) {
            int currPercent = (int) (ind++ * 100.0 / pids.size()), nextPercent = (int) (ind * 100.0 / pids.size());
            callback.call(currPercent, "Getting data for problem " + pid);
            int numStudents = 0;
            HashMap<String, LinkedList<Attempt>> hm = getAllCode(pid, callback, currPercent, nextPercent);
            for(String sid: hm.keySet()) {
//                callback.call((int) (currPercent + (numStudents++ * 1.0 * (nextPercent - currPercent) / hm.size())), "Getting data for problem " + pid);
                if(!ret.containsKey(sid)) {
                    ret.put(sid, new LinkedHashMap<>());
                }
                if(!ret.get(sid).containsKey(pid)) {
                    ret.get(sid).put(pid, new ArrayList<>());
                }
                LinkedList<Attempt> cur = hm.get(sid);
                if(cur.size() == 0) continue;
                for (Attempt attempt : cur) {
                    ret.get(sid).get(pid).add(attempt);
                }
            }
        }
        return ret;
    }


    public ArrayList<DiffBetweenProblems> minTimeDiff(HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data, int n, Callback callback) {
        ExecutorService service = Executors.newFixedThreadPool(10);
        Hashtable<String, String> names = getNames();
        AtomicInteger numStudents = new AtomicInteger();
        ArrayList<DiffBetweenProblems> smallest = new ArrayList<>();
        for(String name: names.keySet()) {
            service.submit(() -> {
                callback.call((int) (numStudents.getAndIncrement() * 100.0 / names.size()), "Processing student " + name + " (" + names.get(name) + ")");
//            System.out.println("processing " + names.get(name));
                Attempt[][] times = new Attempt[n][]; // times[problem][attempt]
                String[] pids = new String[n];
                int ind = 0;
                for(String pid: data.get(name).keySet()) {
                    times[ind] = data.get(name).get(pid).toArray(new Attempt[0]);
                    pids[ind] = pid;
                    Arrays.sort(times[ind++]); // just in case it's not sorted
                }
//            System.out.println(Arrays.deepToString(times));
                double min = 1e11;
                DiffBetweenProblems minDiff = null;
//            String res = "Nothing found";
                for(int i = 0;i < n;i++) {
                    for(int j = 0;j < n;j++) {
                        if(i == j) continue;
                        //start from 1, so that we can compare with the previous attempt to eliminate small changes
                        for(int a = 1;a < times[i].length;a++) {
                            for(int b = 1;b < times[j].length;b++) {
                                if(times[i][a].timestamp() > times[j][b].timestamp()) {
                                    int distance = LevenshteinDistance.getDistance(times[i][a - 1].code(), times[i][a].code());
                                    if (distance * 1.0 / times[i][a - 1].code().length() < 0.1) continue;
                                    double curr = Math.abs(times[i][a].timestamp() - times[j][b].timestamp()) / 1000.0;
                                    if (curr < min) { //TODO tunable
                                        min = curr;
                                        minDiff = new DiffBetweenProblems(name, pids[j], pids[i], b + 2, a + 2, min, distance / min);
                                    }
                                }
                            }
                        }
                    }
                }
                if(minDiff != null) smallest.add(minDiff);
//            System.out.println(name + " " + names.get(name) + ": " + res);
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {throw new RuntimeException(e);}
        return smallest;
    }

    //n is num of problems to analyze
    public ArrayList<Diff> findLargeEdits(HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data, int n, Callback callback) {
        Hashtable<String, String> names = getNames();
        HashMap<String, ArrayList<Diff>> diffs = new HashMap<>(); //one entry for each problem, values are combined diffs from all students for that problem
        int ind = 0;
        for(String sid: data.keySet()) {
            callback.call((int) (ind++ * 100.0 / names.size()), "Processing student " + sid + " (" + names.get(sid) + ")");
            for(String pid: data.get(sid).keySet()) {
                if(!diffs.containsKey(pid)) diffs.put(pid, new ArrayList<>());
                ArrayList<Attempt> attempts = data.get(sid).get(pid);
                for(int i = 1;i < attempts.size();i++) {
                    if(LevenshteinDistance.getDistance(attempts.get(i - 1).code(), attempts.get(i).code()) * 1.0f / attempts.get(i - 1).code().length() < 0.3) continue; //TODO tunable
                    diffs.get(pid).add(new Diff(sid, pid, i + 2, LevenshteinDistance.getDistance(attempts.get(i - 1).code(), attempts.get(i).code())));
                }
            }
        }
        ArrayList<Diff> suspicious = new ArrayList<>();
        ind = 0;
        for(String pid: diffs.keySet()) {
            callback.call((int) (ind++ * 100.0 / n), "Processing problem " + pid);
//            System.out.println(pid);
            diffs.get(pid).sort(Comparator.comparingDouble(Diff::score));
            int num = diffs.get(pid).size();
            double avg = 0, ret = 0;
            for(Diff i: diffs.get(pid)) avg += i.score();
            avg /= num;
            for(Diff i: diffs.get(pid)) ret += Math.pow(i.score() - avg, 2);
            double stdev = Math.sqrt(ret / (num - 1));
//            System.out.println(diffs.get(pid));
//            System.out.println("stdev: " + stdev + " avg: " + avg);
            for(Diff i: diffs.get(pid)) {
//                System.out.print(i.score() + ", ");
                if((i.score() - avg) / stdev >= 3) { //TODO tunable
                    suspicious.add(i);
                }
            }
//            System.out.println();
        }
//        for(Diff i: suspicious) System.out.print(i.score() + ", ");
//        System.out.println();
        return suspicious;
    }
}

