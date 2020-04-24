package pt.ulisboa.tecnico.cnv.balancer;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.cnv.dynamo.AWSDynamoDBClient;
import pt.ulisboa.tecnico.cnv.dynamo.dto.DynamoMetricsItem;

/**
 * BalancerMain
 */
public class BalancerMain {

    private static final long DEFAULT_ESTIMATE_LOAD = 200000000; // 200 million basic blocks
    private static final long DEFAULT_ESTIMATE_DURATION = 40000; // milliseconds

    private static final long MAX_SERVER_LOAD = DEFAULT_ESTIMATE_LOAD * 20;
    private static final long ESTIMATE_MULTI_QUERIES_PENALTY = 10000; // milliseconds

    private AWSDynamoDBClient dynamoClient;

    private List<WebServer> serverList;

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

    // TODO use BigInteger.valueOf(DEFAULT_ESTIMATE_LOAD) to create load estimate
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
        //TODO initialize attributes
        dynamoClient = new AWSDynamoDBClient();
    }

    // TODO missing implementation
    public String requestServer(String query) {

        return "server url";
    }

    // TODO implement periodic health check using HealthParameters
    public void healthCheck() {

    }

    // TODO missing implementation
    public int addServer(List<String> urls) {
        return 0;
    }

    // TODO missing implementation
    public int removeServer(List<String> urls) {
        return 0;
    }
}
