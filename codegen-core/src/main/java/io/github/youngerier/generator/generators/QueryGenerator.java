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

/**
 * Query模型类生成器
 */
public class QueryGenerator extends AbstractClassGenerator {

    private static final ClassName LOCAL_DATE_TIME_TYPE = ClassName.get("java.time", "LocalDateTime");

    public QueryGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    public TypeSpec generate(ClassMetadata classMetadata) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(ClassName.get(AbstractPageQuery.class), ClassName.get(DefaultOrderField.class)))
                .addAnnotation(ClassName.get("lombok", "Data"));

        addAllFields(classBuilder, classMetadata.getFields());
        addClassJavadoc(classBuilder, classMetadata, "查询参数对象");
        addTimeRangeFields(classBuilder);

        return classBuilder.build();
    }

    private void addTimeRangeFields(TypeSpec.Builder classBuilder) {
        classBuilder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "minGmtCreate", Modifier.PRIVATE)
                .addJavadoc("最小创建时间\n")
                .build());
        classBuilder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "maxGmtCreate", Modifier.PRIVATE)
                .addJavadoc("最大创建时间\n")
                .build());
        classBuilder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "minGmtModified", Modifier.PRIVATE)
                .addJavadoc("最小修改时间\n")
                .build());
        classBuilder.addField(FieldSpec.builder(LOCAL_DATE_TIME_TYPE, "maxGmtModified", Modifier.PRIVATE)
                .addJavadoc("最大修改时间\n")
                .build());
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
