package pt.ulisboa.tecnico.cnv.dynamo;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.cnv.dynamo.dto.DynamoMetricsItem;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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

    public void writeMetrics(long threadID, long startTimeMillis, long elapsedTimeMillis, BigInteger basicBlocks, String strategy,
            int max_unassigned_entries, int puzzle_lines, int puzzle_columns, String puzzle_name) {

        DynamoMetricsItem item = new DynamoMetricsItem();

        item.setThreadID(threadID);
        item.setStartTimeMillis(startTimeMillis);
        item.setElapsedTimeMillis(elapsedTimeMillis);
        Timestamp timestamp = new Timestamp(startTimeMillis);
        item.setStartTimeReadable(timestamp.toString());
        item.setBasicBlocks(basicBlocks);
        item.setStrategy(strategy);
        item.setMax_unassigned_entries(max_unassigned_entries);
        item.setPuzzle_lines(puzzle_lines);
        item.setPuzzle_columns(puzzle_columns);
        item.setPuzzle_name(puzzle_name);

        mapper.save(item);
    }

    // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.QueryScanExample.html
    public List<DynamoMetricsItem> readMetrics(long threadID, long startTimeMillis) {
        DynamoMetricsItem item = new DynamoMetricsItem();

        DynamoMetricsItem partitionKey = new DynamoMetricsItem();

        partitionKey.setThreadID(threadID);

        DynamoDBQueryExpression<DynamoMetricsItem> queryExpression = new DynamoDBQueryExpression<DynamoMetricsItem>()
                .withHashKeyValues(partitionKey);

        List<DynamoMetricsItem> itemList = mapper.query(DynamoMetricsItem.class, queryExpression);

        return itemList;
    }

    public List<DynamoMetricsItem> getMetricsFromQuery(String strategy, String max_unassigned_entries,
            String puzzle_lines, String puzzle_columns, String puzzle_name) {

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(strategy));
        eav.put(":val2", new AttributeValue().withS(max_unassigned_entries));
        eav.put(":val3", new AttributeValue().withS(puzzle_lines));
        eav.put(":val4", new AttributeValue().withS(puzzle_columns));
        eav.put(":val5", new AttributeValue().withS(puzzle_name));

        DynamoDBQueryExpression<DynamoMetricsItem> queryExpression = new DynamoDBQueryExpression<DynamoMetricsItem>()
                .withKeyConditionExpression("strategy = :val1 and max_unassigned_entries = :val2 and puzzle_lines = :val3 and puzzle_columns = :val4 and puzzle_name = :val5")
                .withExpressionAttributeValues(eav);

        List<DynamoMetricsItem> itemList = mapper.query(DynamoMetricsItem.class, queryExpression);

        return itemList;
    }

}
