package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.AeronTransport;
import uk.co.palmr.gennaker.Gennaker;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.time.ZoneOffset.UTC;

public class ExamplePingPong {
    public static void main(String[] args) {
        final var gennaker = new Gennaker(new AeronTransport());

        final var pingPublisher = gennaker.publisher(Ping.class);
        final var pongPublisher = gennaker.publisher(Pong.class);

        gennaker.subscribe(Pong.class, new PongHandler());
        gennaker.subscribe(Ping.class, new PingHandler(pongPublisher));

        System.out.println("Sending a doPing & doRepeat every 3 seconds...");
        while (!Thread.interrupted()) {
            final var now = Instant.now().atZone(UTC);
            pingPublisher.doPing("Ping @ " + now);
            pingPublisher.doRepeat(now.getSecond(), "Repeat Me! ");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        }

        System.out.println("Shutting down...");
        gennaker.shutdown();
    }

    private static class PingHandler implements Ping {
        private final Pong pongPublisher;

        public PingHandler(final Pong pongPublisher) {
            this.pongPublisher = pongPublisher;
        }

        @Override
        public void doPing(final String message) {
            System.out.println("Received Ping: " + message);

            pongPublisher.doPong("Pong: " + message);
        }

        @Override
        public void doRepeat(final int count, final String message) {
            System.out.println("Doing Repeat: " + message.repeat(count));
        }
    }

    private static class PongHandler implements Pong {
        @Override
        public void doPong(String message) {
            System.out.println("Received Pong: " + message);
        }
    }
}
