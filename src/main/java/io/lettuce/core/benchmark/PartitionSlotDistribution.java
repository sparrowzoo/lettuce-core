package io.lettuce.core.benchmark;

import java.util.List;
import java.util.Map;

public class PartitionSlotDistribution {
    Map<String, List<Integer>> partitionSlotMap;
    Map<Integer, List<String>> partitioned;

    public Map<String, List<Integer>> getPartitionSlotMap() {
        return partitionSlotMap;
    }

    public void setPartitionSlotMap(Map<String, List<Integer>> partitionSlotMap) {
        this.partitionSlotMap = partitionSlotMap;
    }

    public Map<Integer, List<String>> getPartitioned() {
        return partitioned;
    }

    public void setPartitioned(Map<Integer, List<String>> partitioned) {
        this.partitioned = partitioned;
    }
}
