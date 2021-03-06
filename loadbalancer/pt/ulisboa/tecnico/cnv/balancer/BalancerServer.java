package pt.ulisboa.tecnico.cnv.balancer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BalancerServer {

    private static BalancerMain balancer;

    private static final int instance_port = 8080;

    private static class HttpClient{

        private static String sendPOST(String url, String body) throws IOException {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            // For POST only - START
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();
            // For POST only - END

            int responseCode = con.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                return "POST request not worked";
            }
        }

    }

    public static void main(final String[] args) throws Exception {

        final HttpServer server = HttpServer.create(new InetSocketAddress(instance_port), 0);

        server.createContext("/sudoku", new MyHandler());
        server.createContext("/addserver", new AddServerHandler());
        server.createContext("/removeserver", new RemoveServerHandler());

        // be aware! infinite pool of threads!
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        balancer = new BalancerMain();

        System.out.println("*******************************************************");
        System.out.println(new java.util.Date());
        System.out.println("Started load balancer on address: " + server.getAddress().toString());
        System.out.println("*******************************************************");
    }

    public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is, "utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {

            long threadID = Thread.currentThread().getId();

            // Get the query.
            final String query = t.getRequestURI().getQuery();
            System.out.println("> Query:\t" + query + " with thread_id:" + threadID);

            // we will forward this request
            // String requestMethod = t.getRequestMethod();
            // Headers requestHeaders = t.getRequestHeaders();
            // InputStream requestBody = t.getRequestBody();

            String serverUrl = balancer.requestServer(query, t.hashCode());

            System.out.println("> Redirecting to server:\t" + serverUrl);

            // query content: "s=<strategy>&un=<max_unassigned_entries>&n1=<puzzle_lines>&n2=<puzzle_columns>&i=<puzzle_name>"
            // <public_dns>:8000/sudoku?s=<strategy>&un=<max_unassigned_entries>&n1=<puzzle_lines>&n2=<puzzle_columns>&i=<puzzle_name>
            String redirected_query = "http://" + serverUrl + ":8000/sudoku?" + query;

            System.out.println("> Redirect url:\t" + redirected_query);

            // forward query to a server
            String response = HttpClient.sendPOST(redirected_query, parseRequestBody(t.getRequestBody()));

            System.out.println("> Redirect url response:\t" + response);

            balancer.deleteRequest(serverUrl, t.hashCode());

            // Send response to browser.
            final Headers hdrs = t.getResponseHeaders();
            hdrs.add("Content-Type", "application/json");

            hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
            hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
            hdrs.add("Access-Control-Allow-Headers",
                    "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            t.sendResponseHeaders(200, response.length());

            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(response);
            osw.flush();
            osw.close();

            os.close();

            System.out.println("> Sent response to " + t.getRemoteAddress().toString() + " with thread_id: "
                    + Thread.currentThread().getId() + "\n");
        }
    }

    // receives dns names for servers to add (/addserver?dns=<dns>)
    static class AddServerHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {
            long threadID = Thread.currentThread().getId();

            // Get the query.
            final String query = t.getRequestURI().getQuery();
            System.out.println("> Query:\t" + query + " with thread_id:" + threadID);

            // Break it down into String[].
            final String[] params = query.split("&");

            // Store as if it was a direct call to SolverMain.
            final ArrayList<String> newArgs = new ArrayList<>();
            for (final String p : params) {
                final String[] splitParam = p.split("=");
                newArgs.add(splitParam[1]);
            }

            int status = balancer.addServer(newArgs);

            // reply to the client
            String response = "The addServer method returned " + status;
            t.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            System.out.println("> Sent response to " + t.getRemoteAddress().toString() + " with thread_id: "
                    + Thread.currentThread().getId() + "\n");
        }
    }

    // receives dns names for servers to remove (/removeserver?dns=<dns>)
    static class RemoveServerHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {
            long threadID = Thread.currentThread().getId();

            // Get the query.
            final String query = t.getRequestURI().getQuery();
            System.out.println("> Query:\t" + query + " with thread_id:" + threadID);

            // Break it down into String[].
            final String[] params = query.split("&");

            // Store as if it was a direct call to SolverMain.
            final ArrayList<String> newArgs = new ArrayList<>();
            for (final String p : params) {
                final String[] splitParam = p.split("=");
                newArgs.add(splitParam[1]);
            }

            int status = balancer.removeServer(newArgs);

            // reply to the client
            String response = "The removeServer method returned " + status;
            t.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            System.out.println("> Sent response to " + t.getRemoteAddress().toString() + " with thread_id: "
                    + Thread.currentThread().getId() + "\n");
        }
    }

}
