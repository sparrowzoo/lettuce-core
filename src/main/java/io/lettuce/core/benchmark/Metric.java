package io.lettuce.core.benchmark;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

class Metric {
    public static final String FORMAT_YYYY_MM_DD_HH_MM_SS_MS = "yyyy-MM-dd HH:mm:ss SSS";
    private String condition;
    private String startTime;
    private String endTime;
    private int tp999;
    private int tp99;
    private int tp95;
    private int max;
    private int sum;
    private int avg;
    private int allCount;

    public Metric() {
    }

    public Metric(int tp99, int tp95, int tp999, int avg, int max) {
        this.tp99 = tp99;
        this.tp95 = tp95;
        this.tp999 = tp999;
        this.avg = avg;
        this.max = max;
    }

    public int getTp99() {
        return tp99;
    }

    public void setTp99(int tp99) {
        this.tp99 = tp99;
    }

    public int getTp95() {
        return tp95;
    }

    public void setTp95(int tp95) {
        this.tp95 = tp95;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getTp999() {
        return tp999;
    }

    public void setTp999(int tp999) {
        this.tp999 = tp999;
    }

    public int getAvg() {
        return avg;
    }

    public void setAvg(int avg) {
        this.avg = avg;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        DateFormat sdf = new SimpleDateFormat(FORMAT_YYYY_MM_DD_HH_MM_SS_MS);
        this.startTime = sdf.format(new Date(startTime));
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        DateFormat sdf = new SimpleDateFormat(FORMAT_YYYY_MM_DD_HH_MM_SS_MS);
        this.endTime = sdf.format(new Date(endTime));
    }


    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public int getAllCount() {
        return allCount;
    }

    public void setAllCount(int allCount) {
        this.allCount = allCount;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public float getQps() {
        return allCount / (sum / 1000F);
    }

    @Override
    public String toString() {
        return this.condition + "|" + this.startTime + "|" + this.endTime + "|" + tp999 + "|" + tp99 + "|" + tp95 + "|" + max + "|" + sum + "|" + avg + "|" + this.allCount + "|" + this.getQps();
    }
}
