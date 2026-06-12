package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import javax.lang.model.element.Modifier;

/**
 * Mapper接口生成器 - 基于MyBatis Flex
 */
public class MapperGenerator extends AbstractClassGenerator {

    public MapperGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder interfaceBuilder = createInterfaceBuilder(getClassName(classMetadata))
                .addAnnotation(Mapper.class)
                .addSuperinterface(createBaseMapperType(classMetadata));

        addClassJavadoc(interfaceBuilder, classMetadata, "数据访问层Mapper接口");

        return interfaceBuilder.build();
    }

    private ParameterizedTypeName createBaseMapperType(ClassMetadata classMetadata) {
        ClassName entityType = getEntityType(classMetadata);
        ClassName baseMapperType = ClassName.get("com.mybatisflex.core", "BaseMapper");
        return ParameterizedTypeName.get(baseMapperType, entityType);
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
