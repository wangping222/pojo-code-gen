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
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        return createClassBuilder(className);
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // @Data 已在 createClassBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        addAllFields(builder, classMetadata.getFields());
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // DTO 不需要额外方法
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "数据传输对象(DTO)");
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
