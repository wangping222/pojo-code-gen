package io.github.youngerier.generator.generators;

import io.github.youngerier.support.QueryWrapperHelper;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.util.StringCaseUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Repository实现类生成器 - 基于MyBatis Flex ServiceImpl
 */
public class RepositoryGenerator extends AbstractClassGenerator {

    public RepositoryGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        ClassName entityType = getEntityType(classMetadata);
        ClassName mapperType = getMapperType();
        ClassName serviceImplType = ClassName.get("com.mybatisflex.spring.service.impl", "ServiceImpl");
        ClassName serviceType = ClassName.get("com.mybatisflex.core.service", "IService");

        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(serviceImplType, mapperType, entityType))
                .addSuperinterface(ParameterizedTypeName.get(serviceType, entityType));
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // 无注解
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // 无字段
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        builder.addMethod(buildQueryWrapperMethod(classMetadata));
        builder.addMethod(buildSelectListByQueryMethod(classMetadata));
        builder.addMethod(buildPageMethod(classMetadata));
    }

    private MethodSpec buildSelectListByQueryMethod(ClassMetadata classMetadata) {
        ClassName entityType = getEntityType(classMetadata);
        ClassName queryType = getQueryType(classMetadata);

        return MethodSpec.methodBuilder("selectListByQuery")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityType))
                .addStatement("return getMapper().selectListByQuery(buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildPageMethod(ClassMetadata classMetadata) {
        ClassName entityType = getEntityType(classMetadata);
        ClassName queryType = getQueryType(classMetadata);

        return MethodSpec.methodBuilder("page")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), entityType))
                .addStatement("$T<$T> page = new $T<>(query.getQueryPage(), query.getQuerySize())",
                        ClassName.get(Page.class), entityType, ClassName.get(Page.class))
                .addStatement("return getMapper().paginate(page, buildQueryWrapper(query))")
                .build();
    }

    private MethodSpec buildQueryWrapperMethod(ClassMetadata classMetadata) {
        ClassName queryType = getQueryType(classMetadata);
        ClassName tableRefs = ClassName.get(classMetadata.getPackageName() + ".table",
                classMetadata.getClassName() + "TableRefs");
        String tableVarName = StringCaseUtils.lowerFirstChar(classMetadata.getClassName()) + "TableRefs";

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("buildQueryWrapper")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(queryType, "query")
                .returns(QueryWrapper.class);

        methodBuilder.addStatement("$T $L = $T.$L", tableRefs, tableVarName, tableRefs,
                StringCaseUtils.lowerFirstChar(classMetadata.getClassName()));

        CodeBlock.Builder queryWrapperBuilder = CodeBlock.builder();
        queryWrapperBuilder.add("return $T.withOrder(query)\n", QueryWrapperHelper.class);
        queryWrapperBuilder.indent();
        queryWrapperBuilder.add(".from($L)\n", tableVarName);

        for (ClassMetadata.FieldInfo field : classMetadata.getFields()) {
            String fieldName = field.getName();
            String getterName = "get" + StringCaseUtils.upperFirstChar(fieldName);
            queryWrapperBuilder.add(".where($L.$L.eq(query.$L()))\n",
                    tableVarName, fieldName, getterName);
        }

        // 只有实体包含时间字段时才生成时间范围查询条件
        boolean hasGmtCreate = classMetadata.getFields().stream()
                .anyMatch(f -> "gmtCreate".equals(f.getName()));
        boolean hasGmtModified = classMetadata.getFields().stream()
                .anyMatch(f -> "gmtModified".equals(f.getName()));

        if (hasGmtCreate) {
            queryWrapperBuilder.add(".and($L.gmtCreate.ge(query.getMinGmtCreate()))\n", tableVarName);
            queryWrapperBuilder.add(".and($L.gmtCreate.le(query.getMaxGmtCreate()))\n", tableVarName);
        }
        if (hasGmtModified) {
            queryWrapperBuilder.add(".and($L.gmtModified.ge(query.getMinGmtModified()))\n", tableVarName);
            queryWrapperBuilder.add(".and($L.gmtModified.le(query.getMaxGmtModified()))\n", tableVarName);
        }

        queryWrapperBuilder.unindent();
        methodBuilder.addCode(queryWrapperBuilder.build());
        return methodBuilder.build();
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "数据访问层实现类");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getRepositoryPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getRepositoryClassName();
    }
}
