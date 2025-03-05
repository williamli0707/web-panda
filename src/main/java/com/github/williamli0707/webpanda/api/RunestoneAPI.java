package com.github.williamli0707.webpanda.api;

import com.github.sisyphsu.dateparser.DateParserUtils;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
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

/**
 * API for accessing Runestone, getting student data, and calculations such
 * as time differences and large edits.
 *
 */
public class RunestoneAPI {
    //Sensitivity settings, read and set from SettingsView.java
    public static int timeDiffSensitivity;
    public static int largeEditSensitivity;

    /**
     * 3 clients are needed. client is used to get the Runestone cookies, because for some reason when
     * manually getting the cookies and setting them in the header, the subsequent requests fail. Using
     * an automatic cookie handler fixes this; however, the automatic cookie handler does not take the
     * access token cookie for some reason. The access token is the cookie that is used to access student
     * code history.
     * Therefore, we use client and noRedirectClient with automatically handled cookie jars, both used
     * in the login process, to intercept the cookies from the login process and use them in subsequent
     * requests with client2. client2 is used for all other requests, like getting history for a student.
     */
    private static OkHttpClient client, noRedirectClient;
    private static OkHttpClient client2;
    private static CustomCookieJar cookiejar;

    /**
     * Stores the username and password for Runestone. These are taken from user on startup.
     */
    public static String user, password;

    /**
     * Stores the lock status. This is used to prevent the thread from resetting the API cokies while the user is
     * requesting problems to be analyzed.
     */
    public static volatile boolean lock = false;

    /**
     * Initializes the three OKHttpClients used for accessing Runestone. client and noRedirectClient are used
     * in the login process and need automatic cookie jars for the reasons described above.
     */
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

    /**
     * Resets the clients, resets the cookies, and then resets the name and problem cache. When Session ID cookies
     * expire, they need to be re-requested, so we create new clients and generate the cookies again.
     */
    public static void reset() {
        while(lock) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock = true;

        initClients();
        try {
            resetCookie();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Cookie was reset. This can happen multiple times.");
        initNameCache();
        initProblemCache();

        lock = false;
    }

    private final static Hashtable<String, String> studentnamescache = new Hashtable<>();
    private final static Hashtable<String, String[]> problemnamescache = new Hashtable<>();
    private final static Hashtable<String, String> defaultcodetemplatecache = new Hashtable<>();
    // Use this cookie for all requests involving client2
    private static String cookie, access_token, session_id;
    private static final String loginURL = "https://runestone.academy/user/login?_next=/runestone/admin";
    private static final String gradeURL = "https://runestone.academy/ns/assessment/gethist";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern pform = Pattern.compile("<input name=\"_formkey\" type=\"hidden\" value=\"(.*?)\" />"),
                                 psessionid = Pattern.compile("session_id_runestone=(.*?);"),
                                 pproblems = Pattern.compile("<script type=\"application/json\" id=\"getassignmentinfo\">([\\s\\S]*?)</script>"),
                                 paccess_token = Pattern.compile("access_token=(.*?);");


    /**
     * Gets all the problems from Runestone. The problems come from
     * <a href="https://runestone.academy/runestone/admin/grading">...</a> and the problem list is embedded
     * in the HTML in a script object with id "getassignmentinfo". Who knows why they did it this way. The
     * problem list is in the format of a JSON object, for example:
     * {
     *      "Practice - Conditionals 2": ["lhs_7_16+"],
     *      "Practice - Conditionals": ["lhs_7_15+"],
     *      "Unit 4 Reading": [
     *              "4. Python Turtle Graphics/4.1 Hello Little Turtles!",
     *              "4. Python Turtle Graphics/4.2 Our First Turtle Program",
     *              ...
     *       ],
     *       ...
     *  }
     * The progblems which we care about have a + at the end, so we remove that. We then store the problems
     * in the private field problemnamescache.
     *
     * Another way to get the problems is to use the same HTTP request that Runestone sends to get the summary
     * of the problems. However, some of the problem names come with HTML embedded in them, which makes it more
     * annoying to isolate the problem names. For example, some problems would be listed as lhs_4_3 while some
     * would look like
     * "<a href=\"/runestone/dashboard/exercisemetrics?id=ex_3_10&chapter=PythonTurtle\">ex_3_10</a>"
     * If this method fails in the future, whoever is fixing this can try
     * sending a request to
     * https://runestone.academy/runestone/admin/get_assignment_release_states
     * to get the section/assignment lists, and then
     * https://runestone.academy/runestone/assignments/get_summary?assignment=Unit%208%20Problem%20Set
     * or similar (get_summary?[url encoded problemset name]) to get the problem list.
     */
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the name cache. This process is much simpler than the problem cache. The list of students
     * is stored in JSON format at https://runestone.academy/runestone/admin/course_students. We parse the JSON
     * and store it in a Hashtable.
     */
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

    /**
     * Returns the cached list of students.
     * @return the cached list of students
     */
    public static Hashtable<String, String> getNames() {
        return studentnamescache;
    }

    /**
     * Returns the cached list of problems.
     * @return the cached list of problems
     */
    public static Hashtable<String, String[]> getProblems() {
        return problemnamescache;
    }

    /**
     * Resets the cookie used for requests. This is needed because the session ID cookie expires after a while, and is
     * also needed when the application starts up. The process is as follows:
     * 1. Visit https://runestone.academy/ to get the session ID cookie, which is then stored in the cookie jar so
     * that the log in request is associated with that cookie.
     * 2. Visit https://runestone.academy/user/login?_next=/runestone/admin to get the form key. This page is the login
     * page, and the login form has a form key which is used in the final request to log in.
     * 3. Send a POST request to https://runestone.academy/user/login?_next=/runestone/admin with the username and
     * password, which as far as I know associates the session ID cookie with a logged in state.
     * 4. Visit https://runestone.academy/ to get the access token cookie, which is then stored in the cookie jar so
     * that we can access student history with client2.
     * @throws IOException in case the requests fail.
     */
    private static void resetCookie() throws IOException {
        Response response = noRedirectClient.newCall(new Request.Builder()
                .url(new URL("https://runestone.academy"))
                .get()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Connection", "keep-alive")
//                .addHeader("Host", "runestone.academy")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "none")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0")
                .build()
        ).execute();
        Matcher match = psessionid.matcher(response.headers("Set-Cookie").toString());
        if(!match.find()) throw new IOException("Could not find session id");
        String sessionID = match.group(1);
        System.out.println("sessionID: " + sessionID);
        response.close(); //closed

        cookie = "session_id_runestone=" + sessionID + "; " +
                "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd;" +
                "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; " +
                "RS_info=\"{\\\"tz_offset\\\": 8.0}\"";

        response = noRedirectClient.newCall(new Request.Builder()
                .url(loginURL)
                .get()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
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
        match = pform.matcher(response.body().string()); //closed
        if(!match.find()) throw new IOException("Could not find form key");
        String formkey = match.group(1);

        RequestBody body = new FormBody.Builder()
                .addEncoded("username", user)
                .addEncoded("password", password)
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
        match = paccess_token.matcher(response.headers("Set-Cookie").toString());
        if(!match.find()) throw new IOException("Could not find access token");
//        System.out.println(response.body().string());
        cookie = "session_id_runestone=" + sessionID + "; " +
                "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd;" +
                "access_token=" + match.group(1) + ";" +
                "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; " +
                "RS_info=\"{\\\"tz_offset\\\": 8.0}\"";
        response.close();

        System.out.println("Refreshed login successfully - " + new Date().getTime());
    }

    /**
     * Returns the name of a student given their ID.
     * @param sid the student ID
     * @return the name of the student
     */
    public static String getName(String sid) {
        return studentnamescache.get(sid);
    }

    /**
     * Returns the grade given to a student for a problem. Although this method hasn't been used, it may be useful in
     * the future.
     * @param sid the student ID
     * @param pid the problem ID
     * @return the grade given to the student for the problem
     */
    public static int requestGrade(String sid, String pid) {
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

    /**
     * Returns the code history for a problem for a particular student. As the Runestone Admin website uses
     * https://runestone.academy/ns/assessment/gethist under the hood to access history, we do the same.
     * @param sid the student ID
     * @param pid the problem ID
     * @return the code history for the problem for the student, in the format of an ordered ArrayList of Attempts.
     */
    public static ArrayList<Attempt> requestHistory(String sid, String pid) {
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
                .addHeader("Cookie", cookie)
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

        ArrayList<Attempt> sortlist = new ArrayList<>();

        while (historyiter.hasNext()) {
            // we opt to use DateParserUtils because I'm too lazy to convert dates.
            sortlist.add(new Attempt(DateParserUtils.parseDate(timestampsiter.next().toString()).getTime(), (String) historyiter.next()));
        }
        sortlist.sort(Attempt::compareTo);

        return sortlist;
    }

    private static Object request(Request request) { // no param 3 retries
        return request(request, 3);
    }

    private static Object request(Request request, int retries) {
        if (retries == 0) {
            throw new RuntimeException(" requests failed for " + request.url());
        }
        while(lock) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock = true;
        try (Response resp = client2.newCall(request).execute()) {
            String out = resp.body().string();

            resp.close();
            lock = false;
            return out;
        } catch (Exception e) {
            lock = false;
            return request(request, retries - 1);
        }
    }

    /**
     * Gets all code for a given problem from all students.
     * @param pid the problem ID
     * @param callback a callback for letting the GUI know the progress of the request.
     * @param currPercent the current percent of the stage for the callback.
     * @param nextPercent the next percent of the stage for the callback.
     * @return a HashMap of student IDs to an ArrayList of Attempts, which are the code history for the problem for
     * the student.
     */
    public static HashMap<String, ArrayList<Attempt>> getAllCode(String pid, Callback callback, int currPercent, int nextPercent) {
        Hashtable<String, String> names = getNames();
        HashMap<String, ArrayList<Attempt>> ret = new HashMap<>();
        int numStudents = 0;
        for (String key : names.keySet()) {
            callback.call((int) (currPercent + (numStudents++ * 1.0 * (nextPercent - currPercent) / names.size())), "Getting data for problem " + pid);
            ret.put(key, requestHistory(key, pid));
        }
        return ret;
    }

    /**
     * Gets all code for given problems from all students. Although this method is not used it may be useful in the
     * future for getting data for multiple problems at once using a String[] or just String instead of a List.
     * @param callback a callback for letting the GUI know the progress of the request.
     * @param pids the problem IDs
     * @return a HashMap of student IDs to a HashMap of problem IDs to an ArrayList of Attempts
     */
    public static HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> getAllCodeMultiple(Callback callback, String... pids) {
        return getAllCodeMultiple(callback, Arrays.asList(pids));
    }

    /**
     * Gets all code for given problems from all students.
     * @param callback a callback for letting the GUI know the progress of the request.
     * @param pids the problem IDs
     * @return a HashMap of student IDs to a HashMap of problem IDs to an ArrayList of Attempts
     */
    public static HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> getAllCodeMultiple(Callback callback, Collection<String> pids) {
        HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> ret = new LinkedHashMap<>(); //sid: pid: attempts
        int ind = 0;
        for(String pid: pids) {
            int currPercent = (int) (ind++ * 100.0 / pids.size()), nextPercent = (int) (ind * 100.0 / pids.size());
            callback.call(currPercent, "Getting data for problem " + pid);
            int numStudents = 0;
            HashMap<String, ArrayList<Attempt>> hm = getAllCode(pid, callback, currPercent, nextPercent);
            for(String sid: hm.keySet()) {
                if(!ret.containsKey(sid)) {
                    ret.put(sid, new LinkedHashMap<>());
                }
                if(!ret.get(sid).containsKey(pid)) {
                    ret.get(sid).put(pid, new ArrayList<>());
                }
                ArrayList<Attempt> cur = hm.get(sid);
                if(cur.size() == 0) continue;
                for (Attempt attempt : cur) {
                    ret.get(sid).get(pid).add(attempt);
                }
            }
        }
        return ret;
    }

    /**
     * Calculates the minimum time differences between submissions of problems. Multithreading implemented because
     * it is very slow. For a given list of problems, the process is as follows:
     * 1. Go through all submissions and cut out ones which were minor edits or which took too long. Minor edits will
     * drag down the mean, and submissions which took too long will lower the mean as well. They also mess around with
     * the standard deviation making it harder to find outliers using z-scores. The amount of submissions cut out
     * are dependent on the timeDiffSensitivity field.
     * 2. Using each submission which meets the requirements, calculate the mean and standard deviation for each
     * problem being analyzed.
     * 3. Go through each submission again and calculate the z-score for each submission. If the z-score is higher
     * than a calculated value using the timeDiffSensitivity, then it is considered an outlier.
     * @param data the data for the problem IDs given, in the format which getAllCodeMultiple() returns. The HashMap
     *             has a key of student ID, and a value of a HashMap with a key of problem ID and a value of the list
     *             of submissions for that problem for that student.
     * @param pids the problem IDs to analyze
     * @param callback a callback for letting the GUI know the progress of the request.
     * @return an ArrayList of DiffBetweenProblems, with found suspicious submissions.
     */
    public static ArrayList<DiffBetweenProblems> minTimeDiff(HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data, String[] pids, Callback callback) {
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Hashtable<String, String> names = getNames();
        ArrayList<DiffBetweenProblems> suspicious = new ArrayList<>();
        AtomicInteger numStudents = new AtomicInteger();
        HashMap<String, ArrayList<DiffBetweenProblems>> diffs = new HashMap<>();
        for(String pid: pids) {
            diffs.putIfAbsent(pid, new ArrayList<>());
        }

        for(String name: names.keySet()) {
            service.submit(() -> {
                callback.call((int) (numStudents.getAndIncrement() * 100.0 / names.size()), "Processing student " + name + " (" + names.get(name) + ")");
                long start = System.currentTimeMillis();
                Attempt[][] times = new Attempt[pids.length][]; // times[problem][attempt]
                int ind = 0;
                for(String pid: pids) {
                    times[ind] = data.get(name).get(pid).toArray(new Attempt[0]);
                    Arrays.sort(times[ind++]);
                }

                for(int i = 0;i < pids.length;i++) {
                    for(int j = 0;j < pids.length;j++) {
                        if(i == j) continue;
                        //start from 1, so that we can compare with the previous attempt to eliminate small changes
                        for(int a = 1;a < times[i].length;a++) {
                            for(int b = 1;b < times[j].length;b++) {
                                if(times[i][a].timestamp() > times[j][b].timestamp()) {
                                    int distance = LevenshteinDistance.getDistance(times[i][a - 1].code(), times[i][a].code());
                                    if (distance * 1.0 / times[i][a - 1].code().length() < 1 - timeDiffSensitivity / 100.0) continue;
                                    double curr = Math.abs(times[i][a].timestamp() - times[j][b].timestamp()) / 1000.0;
                                    if(curr > 2000 * timeDiffSensitivity / 100.0) continue; //tunable from 0-2000 seconds

                                    diffs.get(pids[i]).add(new DiffBetweenProblems(name, pids[j], pids[i], b + 2, a + 2, curr, distance / curr));
                                }
                            }
                        }
                    }
                }

                System.out.println("Finished " + name + " " + names.get(name) + " in " + (System.currentTimeMillis() - start) + "ms");
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {throw new RuntimeException(e);}

        callback.call(100, "Processing results");

        for(String pid: diffs.keySet()) {
            System.out.println(pid);

            diffs.get(pid).sort(Comparator.comparingDouble(DiffBetweenProblems::score));
            int num = diffs.get(pid).size();
            double avg = 0, ret = 0;
            for(DiffBetweenProblems i: diffs.get(pid)) avg += i.score();
            avg /= num;
            for(DiffBetweenProblems i: diffs.get(pid)) ret += Math.pow(i.score() - avg, 2);
            double stdev = Math.sqrt(ret / (num - 1));
            System.out.println(avg + " " + stdev);

            for(DiffBetweenProblems i: diffs.get(pid)) {
                if((i.score() - avg) / stdev >= timeDiffSensitivity / 10.0) { //tunable from 0-10
                    suspicious.add(i);
                }
            }
        }

        return suspicious;

    }

    //n is num of problems to analyze

    /**
     * Finds large edits in code. This is done by calculating the Levenshtein distance between each submission and the
     * previous submission, and if the distance is greater than a certain threshold dependent on largeEditSensitivity,
     * then it is considered a large edit.
     * @param data the data for the problem IDs given, in the format which getAllCodeMultiple() returns. The HashMap
     *             has a key of student ID, and a value of a HashMap with a key of problem ID and a value of the list
     *             of submissions for that problem for that student.
     * @param n the number of problems to analyze
     * @param callback a callback for letting the GUI know the progress of the request.
     * @return an ArrayList of Diff, with found suspicious submissions.
     */
    public static ArrayList<Diff> findLargeEdits(HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data, int n, Callback callback) {
        Hashtable<String, String> names = getNames();
        HashMap<String, ArrayList<Diff>> diffs = new HashMap<>(); //one entry for each problem, values are combined diffs from all students for that problem
        int ind = 0;
        for(String sid: data.keySet()) {
            callback.call((int) (ind++ * 100.0 / names.size()), "Processing student " + sid + " (" + names.get(sid) + ")");
            for(String pid: data.get(sid).keySet()) {
                if(!diffs.containsKey(pid)) diffs.put(pid, new ArrayList<>());
                ArrayList<Attempt> attempts = data.get(sid).get(pid);
                for(int i = 1;i < attempts.size();i++) {
                    if(LevenshteinDistance.getDistance(attempts.get(i - 1).code(), attempts.get(i).code()) * 1.0f / attempts.get(i - 1).code().length() < 0.3) continue;
                    diffs.get(pid).add(new Diff(sid, pid, i + 2, LevenshteinDistance.getDistance(attempts.get(i - 1).code(), attempts.get(i).code())));
                }
            }
        }
        ArrayList<Diff> suspicious = new ArrayList<>();
        ind = 0;
        for(String pid: diffs.keySet()) {
            callback.call((int) (ind++ * 100.0 / n), "Processing problem " + pid);
            diffs.get(pid).sort(Comparator.comparingDouble(Diff::score));
            int num = diffs.get(pid).size();
            double avg = 0, ret = 0;
            for(Diff i: diffs.get(pid)) avg += i.score();
            avg /= num;
            for(Diff i: diffs.get(pid)) ret += Math.pow(i.score() - avg, 2);
            double stdev = Math.sqrt(ret / (num - 1));
            for(Diff i: diffs.get(pid)) {
                if((i.score() - avg) / stdev >= 10 - largeEditSensitivity / 10) { //tunable from 1-10
                    suspicious.add(i);
                }
            }
        }
        return suspicious;
    }
}

