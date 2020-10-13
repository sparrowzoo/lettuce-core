package io.lettuce.core.benchmark;

import java.util.Random;

public class Random15Test {
    public static void main(String[] args) {
        for (int i = 1; i <= 100; i++) {
            int random = generate15();
            int randomSign = new Random().nextInt(2); //generate15();
            int r7 = random * 7;
            if (randomSign % 2 == 0) {
                System.out.println(Math.ceil(r7 / 5F));
            } else {
                System.out.println(Math.round(r7 / 5F));
            }
        }
    }

    public static int generate15() {
        return new Random().nextInt(5) + 1;
    }
}
