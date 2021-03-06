import concurrentBuffer.*;
import pseudoCond.PseudoCond;
import timeMeasure.TimeMeter;

import java.util.Random;
import java.util.Scanner;

/**
 * implementation of producers and consumers problem with two and four conditions with tracing threads' work
 * @author Łukasz Dubiel
 * */


public class MainTimeMeasures {


    static class Worker implements Runnable {

        private final String name;
        private final int index;
        private final String role;
        private final Buffer buffer;
        private final Random random = new Random();


        Worker(String name, String role, Buffer buffer, int index) {
            this.name = name;
            this.role = role;
            this.buffer = buffer;
            this.index = index;
        }

        private int getRandSize() {
            if (index > alterPoint)
                return random.nextInt(dataSizeUpperBound_2 - dataSizeLowerBound_2 + 1) + dataSizeLowerBound_2;
            else
                return random.nextInt(dataSizeUpperBound_1 - dataSizeLowerBound_1 + 1) + dataSizeLowerBound_1;
        }

        private int[] genRandData() {
            Random random = new Random();
            int[] data = new int[getRandSize()];
            for (int i = 0; i < data.length; i++) {
                data[i] = random.nextInt(dataBound);
            }
            return data;
        }

        @Override
        public void run() throws IllegalArgumentException {
            int[] data;
            int size;
            float start, end;

            while (!pseudoCond.end) {
                sleep(workersDelay);
                if (!pseudoCond.stop) {

                    if (role.equals("producer")) {
                        start = System.nanoTime();
                        data = genRandData();
                        buffer.produce(data);
                        end = System.nanoTime();
                        timeMeter.logProducerTime(index, end - start);
                    } else if (role.equals("consumer")) {
                        start = System.nanoTime();
                        size = getRandSize();
                        data = buffer.consume(size);
                        end = System.nanoTime();
                        timeMeter.logConsumerTime(index, end - start);
                    } else {
                        throw new IllegalArgumentException("Incorrect role for worker");
                    }

                } else {
                    pseudoCond.wait_();
                }
            }
        }
    }


    static private PseudoCond pseudoCond = new PseudoCond();
    static private int dataSizeUpperBound_1;
    static private int dataSizeLowerBound_1;
    static private int dataSizeUpperBound_2;
    static private int dataSizeLowerBound_2;

    static private int dataBound;
    static private int workersDelay;
    static private int alterPoint = Integer.MAX_VALUE;

    static private TimeMeter timeMeter;

    public static void main(String[] args) {

        /*
         * set of parameters
         * */

        int producersNumb = 20;
        int consumersNumb = 5;
        int bufferSize = 100;
        dataSizeUpperBound_1 = 40;
        dataSizeLowerBound_1 = 1;

        alterPoint = 10;

        dataSizeUpperBound_2 = 45;
        dataSizeLowerBound_2 = 40;

        dataBound = 1;
        workersDelay = 1;
        String filePath = "log1.txt";


        boolean savingHistory = false;

        Buffer buffer2 = new BufferTwoCond(bufferSize, pseudoCond);
        Buffer buffer4 = new BufferFourCond(bufferSize, pseudoCond);
        Buffer buffer3 = new BufferThreeLocks(bufferSize, pseudoCond);

        Buffer buffer = buffer4;
        /**
         * end of set of parameters
         * */


        Worker[] producers = initWorkers(producersNumb, "producer", buffer);
        Worker[] consumers = initWorkers(consumersNumb, "consumer", buffer);

        Thread[] producersThreads = declareWorkersThreads(producers);
        Thread[] consumersThreads = declareWorkersThreads(consumers);

        startThreads(producersThreads);
        startThreads(consumersThreads);

        timeMeter = new TimeMeter(producersNumb, consumersNumb, producersThreads, consumersThreads);

        Scanner scanner = new Scanner(System.in);
        String input = "";
        while (!input.equals("end")) {
            input = scanner.nextLine();
            System.out.println("command: <" + input + ">");
            pseudoCond.stop = true;
            String[] commandAndParams = input.split(" ");

            sleep(500);
            switch (commandAndParams[0]) {
                case "continue":
                    System.out.println("Continuing");
                case "end":
                    pseudoCond.stop = false;
                    pseudoCond.notifyAll_();
                    break;
                case "state":
                    System.out.println(timeMeter.toStringTimes());
                    break;
                case "buffer":
                    buffer.printBufferState();
                    break;
                case "save":
                    break;

            }
        }

        System.out.println("out of loop");
        pseudoCond.end = true;
        sleep(1000);

        joinThreads(consumersThreads);
        System.out.println("Consumers joined");
        joinThreads(producersThreads);
        System.out.println("Producers joined");
    }

    static private int getTail(String[] commandAndParams) {
        int tail = 10;
        try {
            if (commandAndParams.length > 1) {
                tail = Integer.parseInt(commandAndParams[1]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.out.println("program is still working");
        }
        return tail;
    }


    private static String dataToString(int[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int a : data) {
            stringBuilder.append(a);
            stringBuilder.append(", ");
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static Worker[] initWorkers(int n, String role, Buffer buffer) {
        Worker[] workers = new Worker[n];
        for (int i = 0; i < n; i++) {
            workers[i] = new Worker(role + i, role, buffer, i);
        }
        return workers;
    }

    private static Thread[] declareWorkersThreads(Worker[] workers) {
        Thread[] workersThreads = new Thread[workers.length];
        for (int i = 0; i < workers.length; i++) {
            workersThreads[i] = new Thread(workers[i]);
        }
        return workersThreads;
    }

    private static void startThreads(Thread[] threads) {
        for (Thread th : threads)
            th.start();
    }

    private static void joinThreads(Thread[] threads) {
        for (Thread th : threads)
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }

}
