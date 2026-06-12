package io.github.youngerier.generator;

import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import static io.github.youngerier.generator.GeneratorConstants.SRC_MAIN_JAVA;

/**
 * 代码生成引擎，负责协调整个代码生成过程。
 */
@Slf4j
public class GeneratorEngine {

    private final GeneratorConfig config;

    public GeneratorEngine(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Execute code generation.
     */
    public void execute() {
        for (Class<?> pojoClass : config.getPojoClasses()) {
            try {
                generateSinglePojo(pojoClass, config.getModuleName());
            } catch (IOException | NoSuchAlgorithmException e) {
                log.error("Error generating code for {}: {}", pojoClass.getName(), e.getMessage(), e);
            }
        }
        log.info("所有代码生成任务完成!");
    }

    private void generateSinglePojo(Class<?> pojoClass, String moduleName) throws IOException, NoSuchAlgorithmException {
        // 1. Parse the POJO class
        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
        ClassMetadata classMetadata = analyzer.parse(pojoClass, moduleName);
        log.info("Successfully parsed POJO: {}", classMetadata.getClassName());

        // 2. 创建包配置
        String basePackage = classMetadata.getBasePackageName();
        PackageStructure packageStructure = new PackageStructure(basePackage, classMetadata.getClassName());

        // 3. 创建文件生成器
        CodeFileWriter codeFileWriter = new CodeFileWriter(config.getOutputBaseDir());

        // 4. 定义需要生成的代码类型
        List<CodeGenerator> generators = resolveGenerators(packageStructure);

        // 5. 生成所有代码
        for (CodeGenerator generator : generators) {
            codeFileWriter.generateFile(generator, classMetadata);
        }

        log.info("为 {} 生成的代码已完成!", classMetadata.getClassName());
        log.info("生成的文件位于: {}", new File(config.getOutputBaseDir(), SRC_MAIN_JAVA).getAbsolutePath());
    }

    private List<CodeGenerator> resolveGenerators(PackageStructure packageStructure) {
        Set<GeneratorType> enabledGenerators = config.getEnabledGeneratorsOrDefault();
        return enabledGenerators.stream()
                .map(type -> GeneratorRegistry.create(type, packageStructure))
                .toList();
    }
}