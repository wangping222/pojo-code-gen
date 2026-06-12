package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper接口生成器 - 基于MyBatis Flex
 */
public class MapperGenerator extends AbstractClassGenerator {

    public MapperGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        return createInterfaceBuilder(className)
                .addAnnotation(Mapper.class)
                .addSuperinterface(createBaseMapperType(classMetadata));
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // @Mapper 已在 createTypeBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // Mapper 接口不需要字段
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // Mapper 接口不需要额外方法
    }

    private ParameterizedTypeName createBaseMapperType(ClassMetadata classMetadata) {
        ClassName entityType = getEntityType(classMetadata);
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        return ParameterizedTypeName.get(baseMapperType, entityType);
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "数据访问层Mapper接口");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getMapperPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getMapperClassName();
    }
}
