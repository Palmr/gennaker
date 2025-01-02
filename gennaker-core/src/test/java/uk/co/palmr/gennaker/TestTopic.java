package uk.co.palmr.gennaker;

import uk.co.palmr.gennaker.annotations.Topic;

@Topic
public interface TestTopic {
    void nop();

    void consume(int x);

    void repeat(String str, int n);
}
