import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition

import java.nio.file.Files
import java.nio.file.Path

def sources = Path.of(pathCrdsIn)
def destination = Path.of(pathCrdsOut)

if (!Files.exists(sources)) {
    return
}

log.info("Input path: ${pathCrdsIn}")
log.info("Output path: ${pathCrdsOut}")

def factory = new YAMLFactory()
        .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)

def mapper = JsonMapper.builder(factory)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build()

Files.list(sources).each {
    def p = it as Path

    if (p.fileName.toString().endsWith('.org-v1.yml')) {
        log.info("Sanitizing ${p}")

        def parser = factory.createParser(p.toFile())
        def crd = mapper.readValue(parser, CustomResourceDefinition.class)

        crd.spec.versions.each {
            ver -> {
                ver.additionalPrinterColumns.sort {
                    col -> col.name
                }
                ver.additionalPrinterColumns.each { col -> {
                    if (col.jsonPath == '.status.deployment.connectorTypeId') {
                        col.jsonPath == '.spec.deployment.connectorTypeId'
                    }
                }}
            }
        }

        def out = destination.resolve(p.fileName)
        if (!Files.exists(out)) {
            Files.createDirectories(out)
        }

        log.info("Writing to ${out}")

        out.toFile().withWriter {
            w -> mapper.writer().writeValue(w, crd)
        }
    }

}