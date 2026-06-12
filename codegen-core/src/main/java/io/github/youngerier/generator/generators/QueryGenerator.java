package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.youngerier.support.AbstractPageQuery;
import io.github.youngerier.support.enums.DefaultOrderField;

import javax.lang.model.element.Modifier;
import java.util.Set;

/**
 * Query模型类生成器
 */
public class QueryGenerator extends AbstractClassGenerator {

    private static final ClassName LOCAL_DATE_TIME_TYPE = ClassName.get("java.time", "LocalDateTime");
    private static final Set<String> TIME_RANGE_FIELDS = Set.of("gmtCreate", "gmtModified");

    public QueryGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(AbstractPageQuery.class),
                        ClassName.get(DefaultOrderField.class)))
                .addAnnotation(ClassName.get("lombok", "Data"));
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // @Data 已在 createTypeBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        addAllFields(builder, classMetadata.getFields());

        // 只有实体包含时间字段时才添加时间范围查询字段
        if (hasTimeRangeFields(classMetadata)) {
            addTimeRangeFields(builder, classMetadata);
        }
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // Query 不需要额外方法
    }

    private boolean hasTimeRangeFields(ClassMetadata classMetadata) {
        return classMetadata.getFields().stream()
                .anyMatch(f -> TIME_RANGE_FIELDS.contains(f.getName()));
    }

    private void addTimeRangeFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        boolean hasGmtCreate = classMetadata.getFields().stream()
                .anyMatch(f -> "gmtCreate".equals(f.getName()));
        boolean hasGmtModified = classMetadata.getFields().stream()
                .anyMatch(f -> "gmtModified".equals(f.getName()));

        if (hasGmtCreate) {
            builder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "minGmtCreate", Modifier.PRIVATE)
                    .addJavadoc("最小创建时间\n").build());
            builder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "maxGmtCreate", Modifier.PRIVATE)
                    .addJavadoc("最大创建时间\n").build());
        }
        if (hasGmtModified) {
            builder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "minGmtModified", Modifier.PRIVATE)
                    .addJavadoc("最小修改时间\n").build());
            builder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "maxGmtModified", Modifier.PRIVATE)
                    .addJavadoc("最大修改时间\n").build());
        }
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "查询参数对象");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getRequestPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getQueryClassName();
    }
}
