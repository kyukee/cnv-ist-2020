package pt.ulisboa.tecnico.cnv.autoscaler;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Date;

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
import java.util.concurrent.Executors;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ecr.model.DescribeImagesFilter;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;

/**
 * AutoScalerMain
 */
public class AutoScalerMain {

    private static final String INSTANCE_AMI = "Packer-CNV-Webserver-*";
    private static final String INSTANCE_TYPE = "t2.micro";
    private static final int INSTANCE_MIN_COUNT = 1;
    private static final int INSTANCE_MAX_COUNT = 1;
    private static final String INSTANCE_KEY_NAME = "CNV-2020-project-educate";
    private static final String INSTANCE_SEC_GROUP = "CNV-project-security-group";

    private static final int METRIC_PERIOD = 60; // seconds
    private static final String METRIC_NAME = "CPUUtilization";
    private static final String METRIC_STATISTICS = "Average";

    private static AmazonEC2 ec2;
    private static AmazonCloudWatch cloudWatch;
    private static String loadBalancerDNS;
    private static int loadBalancer_port = 8080;

    private static final int CPU_SCALE_UP_TRESHHOLD = 60;
    private static final int CPU_SCALE_DOWN_TRESHHOLD = 40;
    private static final int SCALE_UP_ADJUSTMENT = 1;
    private static final int SCALE_DOWN_ADJUSTMENT = -1;

    private static final int INITIAL_SIZE = 1;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 3;

    private static final int MAX_TIMEOUTS = 3;
    private static final int TIMEOUT_DURATION = 5;

    // key - server url, value - number of consecutive timeouts
    private Map<String, Integer> serverList;

    private static class HttpClient {

        public static String sendGet(String url) throws IOException {
            BufferedReader reader = null;
            StringBuilder stringBuilder;

            HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
            httpClient.setRequestMethod("GET");

            // give it 15 seconds to respond
            httpClient.setReadTimeout(15 * 1000);
            // httpClient.connect();

            // read the output from the server
            reader = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));
            stringBuilder = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            return stringBuilder.toString();
        }

    }

    public AutoScalerMain(){
        serverList = new HashMap<String, Integer>();

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new RuntimeException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
                }

        ec2 = AmazonEC2ClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(credentialsProvider)
            .build();

        cloudWatch = AmazonCloudWatchClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(credentialsProvider)
            .build();

        // find load balancer dns
        try {
            loadBalancerDNS = "http://" + getLoadBalancerDNS("loadbalancer-001") + ":" + loadBalancer_port;
            System.out.println("load balancer dns: " + loadBalancerDNS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create initial instances, if necessary
        int num_running_instances = -2;

        for (Instance instance : listInstances()) {
            String state = instance.getState().getName();
            if (state.equals("running")) {
                num_running_instances++;
                System.out.println("Actually running instances" + num_running_instances);
            }
        }

        if (INITIAL_SIZE - num_running_instances > 0) {
            for (int i = 0; i < INITIAL_SIZE - num_running_instances; i++) {
                launchInstance();
            }
        }
    }

    private String getLoadBalancerDNS(String balancer_name) {

        for (Instance instance : listInstances()) {
            System.out.println("Instance id:" + instance.getInstanceId());

            if (instance.getTags() != null) {
                for (Tag tag : instance.getTags()) {

                    if (tag.getValue().equals(balancer_name)) {
                        return instance.getPublicDnsName();
                    }

                    System.out.println(String.format("%s: %s", tag.getKey(), tag.getValue()));
                }
            }

            // Tag tagName = instance.getTags().stream()
            //             .filter(o -> o.getKey().equals(balancer_name))
            //             .findFirst()
            //             .orElse(new Tag("Name", "name not found"));

            // System.out.println("Found instance with ID: " + instance.getInstanceId()
            //         + ", NAME: " + tagName.getValue()
            //         + ", TYPE: " + instance.getInstanceType());


        }
        return null;
    }

    private void launchInstance() {
        try {
            System.out.println("Starting a new instance.");
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

            runInstancesRequest.withImageId(lookupImageId(INSTANCE_AMI))
                            .withInstanceType(INSTANCE_TYPE)
                            .withMinCount(INSTANCE_MIN_COUNT)
                            .withMaxCount(INSTANCE_MAX_COUNT)
                            .withKeyName(INSTANCE_KEY_NAME)
                            .withSecurityGroups(INSTANCE_SEC_GROUP);

            RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

            // wait for dns
            Thread.sleep(10000);

            Instance instance = runInstancesResult.getReservation().getInstances().get(0);

            String newInstanceId = instance.getInstanceId();
            String newInstanceDNS = instance.getPublicDnsName();

            System.out.println("started instance id = " + newInstanceId + ",  dns = " + newInstanceDNS);

            // HttpClient.sendGet(loadBalancerDNS + "/addserver?dns=" + newInstanceDNS);
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lookupImageId(String imageId) {
        if (imageId == null || imageId.length() == 0)
            return null;

        DescribeImagesRequest req = new DescribeImagesRequest().withFilters(new Filter().withName("tag:Name").withValues(imageId));
        // DescribeImagesRequest req = new DescribeImagesRequest().withFilters(new Filter().withName("image-id").withValues(imageId));
        // DescribeImagesRequest req = new DescribeImagesRequest().withImageIds(imageId);

        DescribeImagesResult result = ec2.describeImages(req);
        if (null != result && null != result.getImages() && !result.getImages().isEmpty()) {
            return result.getImages().get(0).getImageId();
        }

        return null;
    }

    private double getTotalAverageLoad() {
        System.out.println("get total average load");

        double averageLoad = 0;
        int n_active_instances = 0;

        Set<Instance> instances = listInstances();
        for (Instance instance : instances) {
            String name = instance.getInstanceId();
            String state = instance.getState().getName();
            if (state.equals("running")) {
                System.out.println("running instance id = " + name);

                averageLoad += getInstanceAverageLoad(name);
                n_active_instances++;

            } else {
                System.out.println("instance id = " + name);
            }
            System.out.println("Instance State : " + state + ".");
        }

        return averageLoad / n_active_instances;
    }

    private double getInstanceAverageLoad(String instanceId) {
        try {
            /* total observation time in milliseconds */
            long offsetInMilliseconds = 1000 * 60 * 10;

            GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                    .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                    .withNamespace("AWS/EC2")
                    .withPeriod(METRIC_PERIOD)
                    .withMetricName(METRIC_NAME)
                    .withStatistics(METRIC_STATISTICS)
                    .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                    .withEndTime(new Date());
            GetMetricStatisticsResult getMetricStatisticsResult = cloudWatch.getMetricStatistics(request);

            double avgCPUUtilization = 0;

            List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
            for (Datapoint dp : datapoints) {
                System.out.println(" CPU utilization for instance " + instanceId + " = " + dp.getAverage());
                avgCPUUtilization = dp.getAverage();
            }

            return avgCPUUtilization;

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }

        // return -1 on error
        return -1;
    }

    private Instance getLowestLoadInstance() {
        double lowestLoad = 100;
        System.out.println("get instance with lowest load");

        Instance lowestLoadInstance = null;

        Set<Instance> instances = listInstances();
        if (!instances.isEmpty()){
            lowestLoadInstance = instances.iterator().next(); // get first element
            for (Instance instance : instances) {
                String name = instance.getInstanceId();
                String state = instance.getState().getName();
                if (state.equals("running")) {
                    System.out.println("running instance id = " + name);

                    double load = getInstanceAverageLoad(name);

                    if (load < lowestLoad) {
                        lowestLoad = load;
                    }

                } else {
                    System.out.println("instance id = " + name);
                }
                System.out.println("Instance State : " + state + ".");
            }
        }

        return lowestLoadInstance;
    }

    private void terminateInstance(String instanceId) {
        try {
            listInstances();

            System.out.println("Terminating the instance with id = " + instanceId);
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            termInstanceReq.withInstanceIds(instanceId);
            ec2.terminateInstances(termInstanceReq);
            System.out.println("Instance terminated");

            listInstances();

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    private Set<Instance> listInstances() {
        try {
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size()
                    + " Availability Zones.");

            DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesResult.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            System.out.println("total reservations = " + reservations.size());
            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

            return instances;
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
        return null;
    }

    private void scaleUp() {
        for (int i = 0; i < SCALE_UP_ADJUSTMENT; i++) {
            launchInstance();
        }
    }

    private void scaleDown() {
        for (int i = 0; i < SCALE_DOWN_ADJUSTMENT; i++) {
            Instance lowestLoad = getLowestLoadInstance();
            terminateInstance(lowestLoad.getInstanceId());
            // try {
            //     HttpClient.sendGet(loadBalancerDNS + "/removeserver?dns=" + lowestLoad.getPublicDnsName());
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }
        }
    }

    public void autoScale() throws Exception {
        while (true) {
            try {
                System.out.println("Periodic autoscale check start");

                double average_cpu = getTotalAverageLoad();

                // load balancer and autoscaler are also instances, but they shouldn't count
                int num_running_instances = -2;

                for (Instance instance : listInstances()) {
                    String state = instance.getState().getName();
                    if (state.equals("running")) {
                        num_running_instances++;
                        System.out.println("Actually running instances:" + num_running_instances);
                    }
                }

                System.out.println("Periodic autoscale check. cpu avg= " + average_cpu + ", instances= " + num_running_instances);

                if (average_cpu > CPU_SCALE_UP_TRESHHOLD && num_running_instances < MAX_SIZE) {
                    scaleUp();
                }

                if (average_cpu < CPU_SCALE_DOWN_TRESHHOLD && num_running_instances > MIN_SIZE) {
                    scaleDown();
                }

                Thread.sleep(1000 * (long) METRIC_PERIOD);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

