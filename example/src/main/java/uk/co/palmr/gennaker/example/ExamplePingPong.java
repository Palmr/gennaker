package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.AeronTransport;
import uk.co.palmr.gennaker.Gennaker;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static java.time.ZoneOffset.UTC;

public class ExamplePingPong {
    public static void main(final String[] args) {
        final var gennaker = new Gennaker(new AeronTransport());

        final var pingPublisher = gennaker.publisher(Ping.class);
        final var pongPublisher = gennaker.publisher(Pong.class);

        gennaker.subscribe(Pong.class, new PongHandler());
        gennaker.subscribe(Ping.class, new PingHandler(pongPublisher));

        System.out.println("Sending a doPing, doRepeat & doRich every 3 seconds...");
        var seq = 0;
        while (!Thread.interrupted()) {
            final var now = Instant.now().atZone(UTC);
            pingPublisher.doPing("Ping @ " + now);
            pingPublisher.doRepeat(now.getSecond(), "Repeat Me! ");
            pingPublisher.doRich(new PingPayload(seq++, "rich @ " + now));
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        }

        System.out.println("Shutting down...");
        gennaker.shutdown();
    }

    private static final class PingHandler implements Ping {
        private final Pong pongPublisher;

        PingHandler(final Pong pongPublisher) {
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

        @Override
        public void doRich(final PingPayload payload) {
            System.out.println("Received Rich[" + payload.seqNum() + "]: " + payload.text());
            pongPublisher.doPong("Pong: " + payload.text());
        }
    }

    private static final class PongHandler implements Pong {
        @Override
        public void doPong(final String message) {
            System.out.println("Received Pong: " + message);
        }
    }
}
