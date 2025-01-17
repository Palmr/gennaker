package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.annotations.Topic;


@Topic
public interface Ping {
    void doPing(String message);

    void doRepeat(int count, String message);
}
