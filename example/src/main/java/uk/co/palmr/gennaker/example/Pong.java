package uk.co.palmr.gennaker.example;

import uk.co.palmr.gennaker.annotations.Codecs;
import uk.co.palmr.gennaker.annotations.Topic;

@Topic(messageCodec = Codecs.JSON)
public interface Pong {
    void doPong(String message);
}
