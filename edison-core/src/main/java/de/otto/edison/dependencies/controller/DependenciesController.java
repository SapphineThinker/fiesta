package de.otto.edison.dependencies.controller;

import de.otto.edison.annotations.Beta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * @since 1.1.0
 */
@Beta
@RestController
public class DependenciesController {

    private final ExternalDependencies externalDependencies;

    @Autowired
    public DependenciesController(ExternalDependencies externalDependencies) {
        this.externalDependencies = externalDependencies;
    }

    @GetMapping(
            value = "/internal/dependencies",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<?>>> listDependencies () {
        Map<String, List<?>> dependencies = singletonMap("dependencies", externalDependencies.getDependencies());
        return ok(dependencies);
    }

}
