package io.github.youngerier.generator;

import io.github.youngerier.generator.generators.ControllerGenerator;
import io.github.youngerier.generator.generators.DtoGenerator;
import io.github.youngerier.generator.generators.MapperGenerator;
import io.github.youngerier.generator.generators.MapstructGenerator;
import io.github.youngerier.generator.generators.QueryGenerator;
import io.github.youngerier.generator.generators.RepositoryGenerator;
import io.github.youngerier.generator.generators.RequestGenerator;
import io.github.youngerier.generator.generators.ResponseGenerator;
import io.github.youngerier.generator.generators.ServiceGenerator;
import io.github.youngerier.generator.generators.ServiceImplGenerator;
import io.github.youngerier.generator.model.PackageStructure;

/**
 * Registry used to create generators from abstract generator types.
 */
public final class GeneratorRegistry {

    private GeneratorRegistry() {
    }

    public static CodeGenerator create(GeneratorType type, PackageStructure packageStructure) {
        return switch (type) {
            case DTO -> new DtoGenerator(packageStructure);
            case SERVICE -> new ServiceGenerator(packageStructure);
            case SERVICE_IMPL -> new ServiceImplGenerator(packageStructure);
            case MAPPER -> new MapperGenerator(packageStructure);
            case CONTROLLER -> new ControllerGenerator(packageStructure);
            case REQUEST -> new RequestGenerator(packageStructure);
            case QUERY -> new QueryGenerator(packageStructure);
            case RESPONSE -> new ResponseGenerator(packageStructure);
            case MAPSTRUCT -> new MapstructGenerator(packageStructure);
            case REPOSITORY -> new RepositoryGenerator(packageStructure);
        };
    }
}
