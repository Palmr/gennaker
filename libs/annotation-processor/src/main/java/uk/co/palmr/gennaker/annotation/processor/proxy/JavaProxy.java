package uk.co.palmr.gennaker.annotation.processor.proxy;

import java.io.Writer;

public interface JavaProxy {
    String getFileName();
    void write(Writer writer) throws Exception;
}
