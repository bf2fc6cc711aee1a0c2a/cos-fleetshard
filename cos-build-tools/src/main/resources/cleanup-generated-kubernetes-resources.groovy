import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import io.fabric8.kubernetes.api.model.GenericKubernetesResource

if (!(inputFile as File).exists()) {
    return
}

log.info("Input file: ${inputFile}")
log.info("Output file: ${outputFile}")

def factory = new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
def mapper = new ObjectMapper(factory).configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
def parser = factory.createParser(inputFile as File)
var docs = mapper.readValues(parser, GenericKubernetesResource.class).readAll()

docs.each {
    it?.metadata?.annotations?.remove('app.quarkus.io/commit-id')
    it?.metadata?.annotations?.remove('app.quarkus.io/build-timestamp')

    it?.additionalProperties?.spec?.template?.metadata?.annotations?.remove('app.quarkus.io/commit-id')
    it?.additionalProperties?.spec?.template?.metadata?.annotations?.remove('app.quarkus.io/build-timestamp')
}

(outputFile as File).withWriter {
    mapper.writer().writeValues(it).writeAll(docs)
}