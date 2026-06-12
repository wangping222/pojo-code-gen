package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.TypeSpec;

/**
 * Response模型类生成器
 */
public class ResponseGenerator extends AbstractClassGenerator {

    public ResponseGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder classBuilder = createClassBuilder(getClassName(classMetadata));

        addAllFields(classBuilder, classMetadata.getFields());
        addClassJavadoc(classBuilder, classMetadata, "响应对象");

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getResponsePackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getResponseClassName();
    }
}
