package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.Gennaker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class PongMain {
    public static void main(String[] args) {
        final var gennaker = new Gennaker();

        final var pongPublisher = gennaker.publisher(Pong.class);

        final var pingHandler = new PingHandler(pongPublisher);
        gennaker.subscribe(Ping.class, pingHandler);

        while (!Thread.interrupted()) {
            System.out.println("Sent " + pingHandler.getPongCount() + " pongs");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        }
    }

    private static class PingHandler implements Ping {
        private final Pong pongPublisher;
        private int pongCount = 0;

        public PingHandler(Pong pongPublisher) {
            this.pongPublisher = pongPublisher;
        }

        @Override
        public void handlePing(String message) {
            pongPublisher.handlePong("Pong: " + message);
            pongCount++;
        }

        @Override
        public void handlePing2(final int count, final String message) {

        }

        public int getPongCount() {
            return pongCount;
        }
    }
}
