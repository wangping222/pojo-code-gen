package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.TypeSpec;

/**
 * Request模型类生成器
 */
public class RequestGenerator extends AbstractClassGenerator {

    public RequestGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder classBuilder = createClassBuilder(getClassName(classMetadata));

        addFieldsExcludingSystem(classBuilder, classMetadata.getFields());
        addClassJavadoc(classBuilder, classMetadata, "请求参数对象");

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getRequestPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getRequestClassName();
    }
}
