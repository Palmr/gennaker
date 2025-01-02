package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.annotations.Topic;


@Topic
public interface Ping {
    void handlePing(String message);

    void handlePing2(int count, String message);
}
