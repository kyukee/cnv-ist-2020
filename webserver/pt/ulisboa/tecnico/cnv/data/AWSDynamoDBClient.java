package pt.ulisboa.tecnico.cnv.data;

import java.math.BigInteger;
import java.util.List;

import pt.ulisboa.tecnico.cnv.data.dto.DynamoMetricsItem;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;

public class AWSDynamoDBClient {

    AmazonDynamoDB dynamoClient;
    DynamoDBMapper mapper;

    public AWSDynamoDBClient() {
        init();
    }

    void init() {
        /*
         * The ProfileCredentialsProvider will return your [default] credential profile
         * by reading from the credentials file located at (~/.aws/credentials).
         */
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new RuntimeException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }
        dynamoClient = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(Regions.US_EAST_1)
            .build();

        mapper = new DynamoDBMapper(dynamoClient);
    }

    public void writeMetrics(long threadID, long startTime, double elapsedTime, BigInteger basicBlocks, String strategy,
            String max_unassigned_entries, String puzzle_lines, String puzzle_columns, String puzzle_name) {

        DynamoMetricsItem item = new DynamoMetricsItem();

        item.setThreadID(threadID);
        item.setStartTime(startTime);
        item.setElapsedTime(elapsedTime);
        item.setBasicBlocks(basicBlocks);
        item.setStrategy(strategy);
        item.setMax_unassigned_entries(max_unassigned_entries);
        item.setPuzzle_lines(puzzle_lines);
        item.setPuzzle_columns(puzzle_columns);
        item.setPuzzle_name(puzzle_name);

        mapper.save(item);
    }

    public List<DynamoMetricsItem> readMetrics(long threadID) {
        DynamoMetricsItem item = new DynamoMetricsItem();

        DynamoMetricsItem partitionKey = new DynamoMetricsItem();

        partitionKey.setThreadID(threadID);

        DynamoDBQueryExpression<DynamoMetricsItem> queryExpression = new DynamoDBQueryExpression<DynamoMetricsItem>()
                .withHashKeyValues(partitionKey);

        List<DynamoMetricsItem> itemList = mapper.query(DynamoMetricsItem.class, queryExpression);

        return itemList;
    }

}
