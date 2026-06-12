package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.util.StringCaseUtils;
import io.github.youngerier.support.Response;
import io.github.youngerier.support.Pagination;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
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
    public TypeSpec generate(ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = classMetadata.getCamelClassName();

        ClassName dtoType = ClassName.get(packageStructure.getDtoPackage(), packageStructure.getDtoClassName());
        ClassName serviceType = ClassName.get(packageStructure.getServicePackage(), packageStructure.getServiceClassName());
        ClassName queryType = ClassName.get(packageStructure.getRequestPackage(), packageStructure.getQueryClassName());
        ClassName responseType = ClassName.get(Response.class);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RestController")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/" + camelEntityName + "s")
                        .build())
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j"));

        addClassJavadoc(classBuilder, classMetadata, "控制器");
        addServiceField(classBuilder, camelEntityName, serviceType);
        addControllerMethods(classBuilder, entityName, camelEntityName, dtoType, serviceType, queryType, responseType);

        return classBuilder.build();
    }

    private void addServiceField(TypeSpec.Builder classBuilder, String camelEntityName, ClassName serviceType) {
        classBuilder.addField(FieldSpec.builder(serviceType, camelEntityName + "Service")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());
    }

    private void addControllerMethods(TypeSpec.Builder classBuilder, String entityName, String camelEntityName,
                                       ClassName dtoType, ClassName serviceType, ClassName queryType, ClassName responseType) {
        String serviceName = camelEntityName + "Service";
        String dtoParamName = camelEntityName + "DTO";

        // createXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping")).build())
                .addParameter(createRequestBodyParam(dtoType, dtoParamName))
                .addJavadoc("创建$L\n", entityName)
                .addJavadoc("@param $L $L数据传输对象\n", dtoParamName, entityName)
                .addJavadoc("@return 创建的$L对象\n", entityName)
                .addStatement("log.info(\"创建$L: {}\", $L)", entityName, dtoParamName)
                .addStatement("$T result = $L.create$L($L)", dtoType, serviceName, entityName, dtoParamName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // getXxxById
        classBuilder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addJavadoc("根据ID查询$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", entityName)
                .addStatement("log.info(\"根据ID查询$L: {}\", id)", entityName)
                .addStatement("$T result = $L.get$LById(id)", dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // queryXxxList
        classBuilder.addMethod(MethodSpec.methodBuilder("query" + entityName + "List")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ParameterizedTypeName.get(ClassName.get(List.class), dtoType)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addMember("value", "$S", "/query")
                        .build())
                .addParameter(createRequestBodyParam(queryType, "query"))
                .addJavadoc("查询$L列表\n", entityName)
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L对象列表\n", entityName)
                .addStatement("log.info(\"查询$L列表: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.query$Ls(query)",
                        ClassName.get(List.class), dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // pageQueryXxxs
        classBuilder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addMember("value", "$S", "/page")
                        .build())
                .addParameter(createRequestBodyParam(queryType, "query"))
                .addJavadoc("分页查询$L\n", entityName)
                .addJavadoc("@param query 查询条件\n")
                .addJavadoc("@return $L分页对象\n", entityName)
                .addStatement("log.info(\"分页查询$L: {}\", query)", entityName)
                .addStatement("$T<$T> result = $L.pageQuery$Ls(query)",
                        ClassName.get(Pagination.class), dtoType, serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // updateXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, dtoType))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PutMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addParameter(createRequestBodyParam(dtoType, dtoParamName))
                .addJavadoc("更新$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@param $L $L数据传输对象\n", dtoParamName, entityName)
                .addJavadoc("@return 更新后的$L对象\n", entityName)
                .addStatement("log.info(\"更新$L: id={}, data={}\", id, $L)", entityName, dtoParamName)
                .addStatement("$T result = $L.update$L(id, $L)", dtoType, serviceName, entityName, dtoParamName)
                .addStatement("return $T.ok(result)", responseType)
                .build());

        // deleteXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(responseType, ClassName.get(Boolean.class)))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .addParameter(createPathVariableParam(TypeName.LONG, "id"))
                .addJavadoc("删除$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .addStatement("log.info(\"删除$L: id={}\", id)", entityName)
                .addStatement("boolean result = $L.delete$L(id)", serviceName, entityName)
                .addStatement("return $T.ok(result)", responseType)
                .build());
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
