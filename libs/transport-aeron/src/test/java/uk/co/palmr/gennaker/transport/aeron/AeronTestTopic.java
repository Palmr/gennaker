package uk.co.palmr.gennaker.transport.aeron;

import uk.co.palmr.gennaker.annotations.Topic;

@Topic
public interface AeronTestTopic {
    void nop();

    void consume(int x);

    void repeat(String str, int n);
}
