package pt.ulisboa.tecnico.cnv.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;

import pt.ulisboa.tecnico.cnv.data.AWSDynamoDBClient;
import pt.ulisboa.tecnico.cnv.data.LocalDatabase;
import pt.ulisboa.tecnico.cnv.data.dto.DynamoMetricsItem;
import pt.ulisboa.tecnico.cnv.solver.Solver;
import pt.ulisboa.tecnico.cnv.solver.SolverArgumentParser;
import pt.ulisboa.tecnico.cnv.solver.SolverFactory;
import pt.ulisboa.tecnico.cnv.solver.SolverMain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {

    private static AWSDynamoDBClient dynamoClient;

	public static void main(final String[] args) throws Exception {

		//final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);

		final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

		server.createContext("/sudoku", new MyHandler());
        server.createContext("/health", new HealthHandler());

		// be aware! infinite pool of threads!
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

        dynamoClient = new AWSDynamoDBClient();

		System.out.println("*******************************************************");
        System.out.println(new java.util.Date());
        System.out.println("Started server on address: " + server.getAddress().toString());
        System.out.println("*******************************************************");

	}

	public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
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

			// Break it down into String[].
			final String[] params = query.split("&");

			// Store as if it was a direct call to SolverMain.
			final ArrayList<String> newArgs = new ArrayList<>();
			for (final String p : params) {
				final String[] splitParam = p.split("=");
				newArgs.add("-" + splitParam[0]);
				newArgs.add(splitParam[1]);
			}
			newArgs.add("-b");
			newArgs.add(parseRequestBody(t.getRequestBody()));

			newArgs.add("-d");

			// Store from ArrayList into regular String[].
			final String[] args = new String[newArgs.size()];
			int i = 0;
			for(String arg: newArgs) {
				args[i] = arg;
				i++;
            }

            final long startTimeMillis = System.currentTimeMillis();
            JSONArray solution = SolverMain.solve(args);
            final long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;

			// Send response to browser.
			final Headers hdrs = t.getResponseHeaders();

            //t.sendResponseHeaders(200, responseFile.length());

			///hdrs.add("Content-Type", "image/png");
            hdrs.add("Content-Type", "application/json");

			hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
			hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            t.sendResponseHeaders(200, solution.toString().length());


            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(solution.toString());
            osw.flush();
            osw.close();

            os.close();

            // print metrics
            System.out.println("> metrics: ");

            Map<String,String> paramsMap = new HashMap<String,String>();

            for (final String p : params) {
                final String[] splitParam = p.split("=");
                paramsMap.put(splitParam[0], splitParam[1]);
			}

            for (Map.Entry entry : paramsMap.entrySet()){
                System.out.println("key: " + entry.getKey() + "; value: " + entry.getValue());
            }

            BigInteger bb_count = LocalDatabase.getBBCount(threadID);

            System.out.println("basic blocks: " + bb_count);
            System.out.println("Solution found in " + elapsedTimeMillis);
            System.out.println("threadID: " + threadID);

            // store metrics in dynamoDB

            String strategy = paramsMap.get("s");
            int undefined_entries = Integer.parseInt(paramsMap.get("un"));
            int puzzle_lines = Integer.parseInt(paramsMap.get("n1"));
            int puzzle_columns = Integer.parseInt(paramsMap.get("n2"));
            String puzzle_name = paramsMap.get("i");

            dynamoClient.writeMetrics(threadID, startTimeMillis, elapsedTimeMillis, bb_count, strategy, undefined_entries, puzzle_lines, puzzle_columns, puzzle_name);
            System.out.println("Wrote metrics to dynamoDB");


            List<DynamoMetricsItem> result = dynamoClient.readMetrics(threadID, startTimeMillis);
            System.out.println("> dynamoDB read response: ");
            for (DynamoMetricsItem item : result) {
                System.out.println(item.toString());
            }


			System.out.println("> Sent response to " + t.getRemoteAddress().toString() + " with thread_id: " + Thread.currentThread().getId() + "\n");
		}
    }

    static class HealthHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {
            String response = "This was the query:" + t.getRequestURI().getQuery() + "##";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
		}
	}
}
