package io.lettuce.core.benchmark;

class TopPercentile {
    private int tp999;
    private int tp99;
    private int tp95;
    private int max;
    private int sum;

    public TopPercentile() {
    }

    public TopPercentile(int tp99, int tp95,int tp999, int max) {
        this.tp99 = tp99;
        this.tp95 = tp95;
        this.tp999=tp999;
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

    @Override
    public String toString() {
        return "TopPercentile{" +
                "tp999=" + tp999 +
                ", tp99=" + tp99 +
                ", tp95=" + tp95 +
                ", max=" + max +
                ", sum=" + sum +
                '}';
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
