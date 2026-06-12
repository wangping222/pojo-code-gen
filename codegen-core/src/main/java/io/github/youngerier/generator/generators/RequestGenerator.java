package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.TypeSpec;

import java.util.Set;

/**
 * Request模型类生成器
 */
public class RequestGenerator extends AbstractClassGenerator {

    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "id", "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    );

    public RequestGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder classBuilder = createClassBuilder(getClassName(classMetadata));

        addFieldsExcluding(classBuilder, classMetadata.getFields(), EXCLUDED_FIELDS);
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
