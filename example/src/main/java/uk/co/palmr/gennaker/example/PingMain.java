package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.Gennaker;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.time.ZoneOffset.UTC;

public class PingMain {
    public static void main(String[] args) {
        final var gennaker = new Gennaker();

        gennaker.subscribe(Pong.class, new PongHandler());

        final var pingPublisher = gennaker.publisher(Ping.class);
        final var pongPublisher = gennaker.publisher(Pong.class);

        gennaker.subscribe(Ping.class, new Ping() {
            @Override
            public void handlePing(final String message) {
                System.out.println("Received Ping: " + message);
                pongPublisher.handlePong("Pong: " + message);
            }

            @Override
            public void handlePing2(final int count, final String message) {
                System.out.println("Received Ping2: " + message.repeat(count));
            }
        });

        while (!Thread.interrupted()) {
            final var now = Instant.now().atZone(UTC);
            pingPublisher.handlePing("Ping @ " + now);
            pingPublisher.handlePing2(now.getSecond(), "Repeat Me! ");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        }
    }

    private static class PongHandler implements Pong {
        @Override
        public void handlePong(String message) {
            System.out.println("Received Pong: " + message);
        }
    }
}
