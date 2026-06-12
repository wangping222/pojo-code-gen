package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.TypeSpec;

/**
 * DTO类生成器
 */
public class DtoGenerator extends AbstractClassGenerator {

    public DtoGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder classBuilder = createClassBuilder(getClassName(classMetadata));

        addClassJavadoc(classBuilder, classMetadata, "数据传输对象(DTO)");
        addAllFields(classBuilder, classMetadata.getFields());

        return classBuilder.build();
    }

    @Override
    public String getPackageName() {
        return packageStructure.getDtoPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getDtoClassName();
    }
}
