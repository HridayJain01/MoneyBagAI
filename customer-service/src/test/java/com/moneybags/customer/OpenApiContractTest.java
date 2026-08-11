package com.moneybags.customer;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {
    @Test
    void ideContractIsValidYamlWithUniqueOperationsAndResolvableLocalReferences() throws IOException {
        Object parsed;
        try (var reader = Files.newBufferedReader(Path.of("openapi.yml"))) {
            parsed = new Yaml().load(reader);
        }
        assertThat(parsed).isInstanceOf(Map.class);
        Map<?, ?> root = (Map<?, ?>) parsed;
        assertThat(root.get("openapi")).isEqualTo("3.0.3");

        Map<?, ?> paths = (Map<?, ?>) root.get("paths");
        assertThat(paths).hasSizeGreaterThanOrEqualTo(25);
        List<String> operationIds = new ArrayList<>();
        for (Object pathItem : paths.values()) {
            Map<?, ?> operations = (Map<?, ?>) pathItem;
            for (Map.Entry<?, ?> entry : operations.entrySet()) {
                if (List.of("get", "post", "put", "patch", "delete").contains(entry.getKey())) {
                    Map<?, ?> operation = (Map<?, ?>) entry.getValue();
                    operationIds.add((String) operation.get("operationId"));
                }
            }
        }
        assertThat(operationIds).doesNotContainNull().doesNotHaveDuplicates();

        List<String> references = new ArrayList<>();
        collectReferences(root, references);
        assertThat(references).isNotEmpty();
        references.forEach(reference -> assertThat(resolve(root, reference))
                .as("OpenAPI reference %s", reference)
                .isNotNull());
    }

    private void collectReferences(Object node, List<String> references) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if ("$ref".equals(key) && value instanceof String reference && reference.startsWith("#/")) {
                    references.add(reference);
                }
                collectReferences(value, references);
            });
        } else if (node instanceof Iterable<?> iterable) {
            iterable.forEach(value -> collectReferences(value, references));
        }
    }

    private Object resolve(Map<?, ?> root, String reference) {
        Object current = root;
        for (String segment : reference.substring(2).split("/")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(segment);
        }
        return current;
    }
}
