package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.support.Response;
import io.github.youngerier.support.Pagination;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Controller控制器生成器
 */
public class ControllerGenerator extends AbstractClassGenerator {

    public ControllerGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        String camelEntityName = classMetadata.getCamelClassName();
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(createRestControllerAnnotation())
                .addAnnotation(createRequestMappingAnnotation("/" + camelEntityName + "s"))
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j"));
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // 注解已在 createTypeBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String camelEntityName = classMetadata.getCamelClassName();
        ClassName serviceType = getServiceType();

        builder.addField(FieldSpec.builder(serviceType, camelEntityName + "Service")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build());
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = classMetadata.getCamelClassName();
        String serviceName = camelEntityName + "Service";
        String dtoParamName = camelEntityName + "DTO";

        ClassName dtoType = getDtoType();
        ClassName responseType = getResponseType();
        ClassName queryType = getQueryType(classMetadata);

        // createXxx
        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(createPostMappingAnnotation(""))
                .addParameter(createRequestBodyParam(dtoType, dtoParamName))
                .addJavadoc("创建$L\n", entityName)
                .addStatement("log.info(\"创建$L: {}\", $L)", entityName, dtoParamName)
                .addStatement("$T result = $L.create$L($L)", dtoType, serviceName, entityName, dtoParamName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // getXxxById
        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(createGetMappingAnnotation("/{id}"))
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addJavadoc("根据ID查询$L\n", entityName)
                .addStatement("log.info(\"根据ID查询$L: {}\", id)", entityName)
                .addStatement("$T result = $L.get$LById(id)", dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // queryXxxList
        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "List")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType,
                        ParameterizedTypeName.get(ClassName.get(List.class), dtoType)))
                .addAnnotation(createPostMappingAnnotation("/query"))
                .addParameter(createRequestBodyParam(queryType, "query"))
                .addJavadoc("查询$L列表\n", entityName)
                .addStatement("log.info(\"查询$L列表: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.query$Ls(query)",
                        ClassName.get(List.class), dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // pageQueryXxxs
        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType,
                        ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType)))
                .addAnnotation(createPostMappingAnnotation("/page"))
                .addParameter(createRequestBodyParam(queryType, "query"))
                .addJavadoc("分页查询$L\n", entityName)
                .addStatement("log.info(\"分页查询$L: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.pageQuery$Ls(query)",
                        ClassName.get(Pagination.class), dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // updateXxx
        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(createPutMappingAnnotation("/{id}"))
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addParameter(createRequestBodyParam(dtoType, dtoParamName))
                .addJavadoc("更新$L\n", entityName)
                .addStatement("log.info(\"更新$L: id={}, data={}\", id, $L)", entityName, dtoParamName)
                .addStatement("$T result = $L.update$L(id, $L)", dtoType, serviceName, entityName, dtoParamName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // deleteXxx
        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ClassName.get(Boolean.class)))
                .addAnnotation(createDeleteMappingAnnotation("/{id}"))
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addJavadoc("删除$L\n", entityName)
                .addStatement("log.info(\"删除$L: id={}\", id)", entityName)
                .addStatement("boolean result = $L.delete$L(id)", serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "控制器");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getControllerPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getControllerClassName();
    }
}
