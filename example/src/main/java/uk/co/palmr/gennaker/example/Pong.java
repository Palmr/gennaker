package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.annotations.Topic;

@Topic
public interface Pong {
    void doPong(final String message);
}
