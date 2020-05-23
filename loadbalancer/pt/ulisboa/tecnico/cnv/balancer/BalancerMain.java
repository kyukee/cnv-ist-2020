package pt.ulisboa.tecnico.cnv.balancer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.cnv.dynamo.AWSDynamoDBClient;
import pt.ulisboa.tecnico.cnv.dynamo.dto.DynamoMetricsItem;

/**
 * BalancerMain
 */
public class BalancerMain {

    private static final long DEFAULT_ESTIMATE_LOAD = 200000000; // 200 million basic blocks
    private static final long DEFAULT_ESTIMATE_DURATION = 40000; // milliseconds

    private static final long MAX_SERVER_LOAD = DEFAULT_ESTIMATE_LOAD * 20;

    private static final long ESTIMATE_MULTI_QUERIES_PENALTY_LOAD = DEFAULT_ESTIMATE_LOAD / 4; // basic blocks
    private static final long ESTIMATE_MULTI_QUERIES_PENALTY_DURATION = DEFAULT_ESTIMATE_DURATION / 4; // milliseconds

    private AWSDynamoDBClient dynamoClient;

    private Map<String, WebServer> serverList;

    private class WebServer {
        int total_load;
        String url;
        Map<Integer, WebServerRequest> running_queries; // map key is HttpExchange.hashCode()
    }

    private class WebServerRequest {
        String query;
        Estimate query_estimate;
        long start_time;
    }

    private class Estimate {
        BigInteger load;
        long duration;
    }

    private static final class HealthParameters {
        private static final int healthy_threshold = 2;
        private static final int unhealthy_threshold = 5;
        private static final int timeout = 5;
        private static final int interval = 30;
        private static final int target_port = 8000;
        private static final String target_endpoint = "/health";
    }

    public BalancerMain(){
        dynamoClient = new AWSDynamoDBClient();
        serverList = new HashMap<String, WebServer>();
    }

    public Estimate getEstimateFromMetrics(List<DynamoMetricsItem> metrics_list){
        long averageDuration = 0;
        BigInteger averageLoad = BigInteger.valueOf(0);
        int count = 0;

        for (DynamoMetricsItem item : metrics_list){
            averageDuration += item.getElapsedTimeMillis();
            averageLoad.add(item.getBasicBlocks());
            count++;
        }

        Estimate metricsEstimate = new Estimate();
        metricsEstimate.duration = averageDuration / count;
        metricsEstimate.load = averageLoad.divide(BigInteger.valueOf(count));
        return metricsEstimate;
    }


    private Map<String, String> queryToMap(String query){
        Map<String, String> paramsMap = new HashMap<String, String>();

        final String[] params = query.split("&");

        for (final String p : params) {
            final String[] splitParam = p.split("=");
            paramsMap.put(splitParam[0], splitParam[1]);
        }

        return paramsMap;
    }

    public Estimate estimateCost( String query ){
        Map<String, String> paramsMap = queryToMap(query);

        String strategy = paramsMap.get("s");
        String max_unassigned_entries = paramsMap.get("un");
        String puzzle_lines = paramsMap.get("n1");
        String puzzle_columns = paramsMap.get("n2");
        String puzzle_name = paramsMap.get("i");

        List<DynamoMetricsItem> metrics_list = dynamoClient.getMetricsFromQuery(strategy, max_unassigned_entries,
            puzzle_lines, puzzle_columns, puzzle_name);

        if (metrics_list.isEmpty()){
            Estimate defaultEstimate = new Estimate();

            defaultEstimate.duration = DEFAULT_ESTIMATE_DURATION;
            defaultEstimate.load = BigInteger.valueOf(DEFAULT_ESTIMATE_LOAD);

            return defaultEstimate;
        }
        return getEstimateFromMetrics(metrics_list);
    }

    public WebServer getServerWithLowestLoad() {

        // use first value found as the initial minimum
        Set<String> keys = serverList.keySet();

        if (keys.isEmpty())
            return null;

        WebServer min_load_server = serverList.get(keys.toArray()[0]);
        BigInteger min_load = BigInteger.valueOf(MAX_SERVER_LOAD);

        for (WebServer server : serverList.values()) {
            BigInteger load = BigInteger.ZERO;

            for (WebServerRequest server_request : server.running_queries.values()){

                // check if request is finished, according to its estimate
                long start_time = server_request.start_time;
                final long current_time = System.currentTimeMillis();
                long duration = server_request.query_estimate.duration;

                long time_left = start_time + duration - current_time;

                // only add requests that we assume are still running
                if ( time_left > 0){
                    load.add(server_request.query_estimate.load);
                }
            }

            // foo.compareTo(bar) - -1, 0 or 1 as foo is numerically less than, equal to, or greater than bar
            if (load.compareTo(min_load) == -1) {
                min_load = load;
                min_load_server = server;
            }
        }

        return min_load_server;
    }

    public String requestServer(String query, int queryHash) throws IOException {

        Estimate estimate = estimateCost(query);
        WebServer min_load_server = getServerWithLowestLoad();

        if (min_load_server == null){
            throw new IOException("No servers available");
        }

        // increase estimate if there already are queries running on the server
        int num_running_queries = min_load_server.running_queries.size();
        estimate.duration += num_running_queries * ESTIMATE_MULTI_QUERIES_PENALTY_DURATION;
        estimate.load.add(BigInteger.valueOf(num_running_queries).multiply(BigInteger.valueOf(ESTIMATE_MULTI_QUERIES_PENALTY_LOAD)));

        // save request data in the load balancer
        WebServerRequest new_server_request = new WebServerRequest();
        new_server_request.query = query;
        new_server_request.query_estimate = estimate;
        new_server_request.start_time = System.currentTimeMillis();

        min_load_server.running_queries.put(queryHash, new_server_request);
        // min_load_server.total_load += estimate.load;

        return min_load_server.url;
    }

    public void deleteRequest(String url, int queryHash) {
        serverList.get(url).running_queries.remove(queryHash);
    }

    // TODO implement periodic health check using HealthParameters
    public void healthCheck() {

    }

    public int addServer(List<String> urls) {

        for (String url :urls) {

            WebServer server = new WebServer();
            server.url = url;
            server.running_queries = new HashMap<Integer, WebServerRequest>();
            server.total_load = 0;

            serverList.put(url, server);
        }

        return 0;
    }

    public int removeServer(List<String> urls) {

        for (String url : urls) {
            serverList.remove(url);
        }

        return 0;
    }
}
