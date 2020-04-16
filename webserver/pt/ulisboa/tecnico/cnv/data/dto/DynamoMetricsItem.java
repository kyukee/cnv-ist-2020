package pt.ulisboa.tecnico.cnv.data.dto;

import java.math.BigInteger;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;

@DynamoDBTable(tableName = "CNV-project-metrics")
public class DynamoMetricsItem {
    private long threadID;
    private long startTimeMillis;
    private long elapsedTimeMillis;
    private String startTimeReadable;
    private BigInteger basicBlocks;
    private String strategy;
    private int max_unassigned_entries;
    private int puzzle_lines;
    private int puzzle_columns;
    private String puzzle_name;

    @DynamoDBHashKey(attributeName = "threadID")
    public long getThreadID() {
        return this.threadID;
    }
    public void setThreadID(long threadID) {
        this.threadID = threadID;
    }

    @DynamoDBRangeKey(attributeName = "startTimeMillis")
    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }
    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    @DynamoDBAttribute(attributeName = "elapsedTimeMillis")
    public long getElapsedTimeMillis() {
        return this.elapsedTimeMillis;
    }
    public void setElapsedTimeMillis(long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    @DynamoDBAttribute(attributeName = "startTimeReadable")
    public String getStartTimeReadable() {
        return this.startTimeReadable;
    }
    public void setStartTimeReadable(String startTimeReadable) {
        this.startTimeReadable = startTimeReadable;
    }

    @DynamoDBAttribute(attributeName = "basicBlocks")
    public BigInteger getBasicBlocks() {
        return this.basicBlocks;
    }
    public void setBasicBlocks(BigInteger basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    @DynamoDBAttribute(attributeName = "strategy")
    public String getStrategy() {
        return this.strategy;
    }
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @DynamoDBAttribute(attributeName = "max_unassigned_entries")
    public int getMax_unassigned_entries() {
        return this.max_unassigned_entries;
    }
    public void setMax_unassigned_entries(int max_unassigned_entries) {
        this.max_unassigned_entries = max_unassigned_entries;
    }

    @DynamoDBAttribute(attributeName = "puzzle_lines")
    public int getPuzzle_lines() {
        return this.puzzle_lines;
    }
    public void setPuzzle_lines(int puzzle_lines) {
        this.puzzle_lines = puzzle_lines;
    }

    @DynamoDBAttribute(attributeName = "puzzle_columns")
    public int getPuzzle_columns() {
        return this.puzzle_columns;
    }
    public void setPuzzle_columns(int puzzle_columns) {
        this.puzzle_columns = puzzle_columns;
    }

    @DynamoDBAttribute(attributeName = "puzzle_name")
    public String getPuzzle_name() {
        return this.puzzle_name;
    }
    public void setPuzzle_name(String puzzle_name) {
        this.puzzle_name = puzzle_name;
    }

    @DynamoDBIgnore
    public String toString() {
        return "{" +
            " threadID='" + getThreadID() + "'" +
            ", startTimeMillis='" + getStartTimeMillis() + "'" +
            ", elapsedTimeMillis='" + getElapsedTimeMillis() + "'" +
            ", startTimeReadable='" + getStartTimeReadable() + "'" +
            ", basicBlocks='" + getBasicBlocks() + "'" +
            ", strategy='" + getStrategy() + "'" +
            ", max_unassigned_entries='" + getMax_unassigned_entries() + "'" +
            ", puzzle_lines='" + getPuzzle_lines() + "'" +
            ", puzzle_columns='" + getPuzzle_columns() + "'" +
            ", puzzle_name='" + getPuzzle_name() + "'" +
            "}";
    }

}
