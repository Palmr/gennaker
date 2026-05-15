package uk.co.palmr.gennaker.codec.json;

import com.google.auto.service.AutoService;
import uk.co.palmr.gennaker.annotations.Codecs;
import uk.co.palmr.gennaker.codec.CodecBodyEmitter;
import uk.co.palmr.gennaker.codec.MessageCodecGenerator;
import uk.co.palmr.gennaker.codec.TopicContext;

@AutoService(MessageCodecGenerator.class)
public class JsonMessageCodecGenerator implements MessageCodecGenerator {

    @Override
    public String name() {
        return Codecs.JSON;
    }

    @Override
    public CodecBodyEmitter generate(final TopicContext context) {
        return new JsonCodecBodyEmitter(context.interfaceName(), context.maxMessageSize(), context.methods());
    }
}
