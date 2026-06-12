package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
import com.squareup.javapoet.*;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 代码生成器抽象基类，提供通用的类和字段生成逻辑
 */
@Slf4j
public abstract class AbstractClassGenerator implements CodeGenerator {

    // 常用包名常量
    protected static final String LOMBOK_PACKAGE = "lombok";
    protected static final String SPRING_STEREOTOPE_PACKAGE = "org.springframework.stereotype";
    protected static final String SPRING_WEB_PACKAGE = "org.springframework.web.bind.annotation";

    protected final PackageStructure packageStructure;

    protected AbstractClassGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    // ==================== 类/接口构建 ====================

    /**
     * 创建类构建器（添加 @Data 注解）
     */
    protected TypeSpec.Builder createClassBuilder(String className) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(LOMBOK_PACKAGE, "Data"));
    }

    /**
     * 创建类构建器（带父类）
     */
    protected TypeSpec.Builder createClassBuilder(String className, TypeName superclass) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(superclass)
                .addAnnotation(ClassName.get(LOMBOK_PACKAGE, "Data"));
    }

    /**
     * 创建接口构建器
     */
    protected TypeSpec.Builder createInterfaceBuilder(String interfaceName) {
        return TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC);
    }

    // ==================== 注解辅助 ====================

    /**
     * 创建 Spring @Service 注解
     */
    protected AnnotationSpec createServiceAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_STEREOTOPE_PACKAGE, "Service")).build();
    }

    /**
     * 创建 Spring @RestController 注解
     */
    protected AnnotationSpec createRestControllerAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RestController")).build();
    }

    /**
     * 创建 Spring @RequestMapping 注解
     */
    protected AnnotationSpec createRequestMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RequestMapping"))
                .addMember("value", "$S", path)
                .build();
    }

    /**
     * 创建 Spring @PostMapping 注解
     */
    protected AnnotationSpec createPostMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PostMapping"))
                .addMember("value", "$S", path)
                .build();
    }

    /**
     * 创建 Spring @GetMapping 注解
     */
    protected AnnotationSpec createGetMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "GetMapping"))
                .addMember("value", "$S", path)
                .build();
    }

    /**
     * 创建 Spring @PutMapping 注解
     */
    protected AnnotationSpec createPutMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PutMapping"))
                .addMember("value", "$S", path)
                .build();
    }

    /**
     * 创建 Spring @DeleteMapping 注解
     */
    protected AnnotationSpec createDeleteMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "DeleteMapping"))
                .addMember("value", "$S", path)
                .build();
    }

    /**
     * 创建 @RequestBody 注解
     */
    protected AnnotationSpec createRequestBodyAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RequestBody")).build();
    }

    /**
     * 创建 @PathVariable 注解
     */
    protected AnnotationSpec createPathVariableAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PathVariable")).build();
    }

    // ==================== 参数构建 ====================

    /**
     * 创建 @RequestBody 参数
     */
    protected ParameterSpec createRequestBodyParam(ClassName type, String name) {
        return ParameterSpec.builder(type, name)
                .addAnnotation(createRequestBodyAnnotation())
                .build();
    }

    /**
     * 创建 @PathVariable 参数
     */
    protected ParameterSpec createPathVariableParam(TypeName type, String name) {
        return ParameterSpec.builder(type, name)
                .addAnnotation(createPathVariableAnnotation())
                .build();
    }

    // ==================== 字段过滤 ====================

    /**
     * 添加所有字段（带注释）
     */
    protected void addAllFields(TypeSpec.Builder classBuilder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsWithFilter(classBuilder, fields, field -> true, false);
    }

    /**
     * 添加字段（带过滤条件）
     */
    protected void addFieldsWithFilter(TypeSpec.Builder classBuilder,
                                        List<ClassMetadata.FieldInfo> fields,
                                        Predicate<ClassMetadata.FieldInfo> filter,
                                        boolean addPrimaryKeyComment) {
        for (ClassMetadata.FieldInfo field : fields) {
            if (!filter.test(field)) {
                continue;
            }
            classBuilder.addField(buildFieldSpec(field, addPrimaryKeyComment));
        }
    }

    /**
     * 添加字段（带排除列表）
     */
    protected void addFieldsExcluding(TypeSpec.Builder classBuilder,
                                       List<ClassMetadata.FieldInfo> fields,
                                       Set<String> excludedFields) {
        addFieldsWithFilter(classBuilder, fields, field -> !excludedFields.contains(field.getName()), false);
    }

    /**
     * 排除时间相关字段
     */
    protected static final Set<String> TIME_FIELDS = Set.of(
            "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    );

    /**
     * 排除系统字段（ID和时间）
     */
    protected static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    );

    /**
     * 排除系统字段添加字段
     */
    protected void addFieldsExcludingSystem(TypeSpec.Builder classBuilder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsExcluding(classBuilder, fields, SYSTEM_FIELDS);
    }

    /**
     * 按类型过滤添加字段
     */
    protected void addFieldsByType(TypeSpec.Builder classBuilder,
                                    List<ClassMetadata.FieldInfo> fields,
                                    Class<?>... types) {
        Set<String> typeNames = java.util.Arrays.stream(types)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        addFieldsWithFilter(classBuilder, fields,
                f -> f.getType() != null && typeNames.contains(f.getType().toString()), false);
    }

    /**
     * 只添加主键字段
     */
    protected void addPrimaryKeyField(TypeSpec.Builder classBuilder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsWithFilter(classBuilder, fields, ClassMetadata.FieldInfo::isPrimaryKey, true);
    }

    /**
     * 构建 FieldSpec
     */
    protected FieldSpec buildFieldSpec(ClassMetadata.FieldInfo field, boolean addPrimaryKeyComment) {
        FieldSpec.Builder builder = FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE);

        String comment = field.getComment();
        if (comment != null && !comment.isEmpty()) {
            builder.addJavadoc(comment + "\n");
        }
        if (addPrimaryKeyComment && field.isPrimaryKey()) {
            builder.addJavadoc("主键ID\n");
        }
        return builder.build();
    }

    // ==================== 类注释 ====================

    /**
     * 添加类注释
     */
    protected void addClassJavadoc(TypeSpec.Builder classBuilder, ClassMetadata classMetadata, String suffix) {
        String classComment = classMetadata.getClassComment();
        if (classComment != null && !classComment.isEmpty()) {
            classBuilder.addJavadoc(classComment + "\n");
            classBuilder.addJavadoc(suffix + "\n");
        }
    }

    // ==================== 实体类型 ====================

    /**
     * 获取实体类型
     */
    protected ClassName getEntityType(ClassMetadata classMetadata) {
        return ClassName.get(classMetadata.getPackageName(), classMetadata.getClassName());
    }

    /**
     * 获取 DTO 类型
     */
    protected ClassName getDtoType() {
        return ClassName.get(packageStructure.getDtoPackage(), packageStructure.getDtoClassName());
    }

    /**
     * 获取 Request 类型
     */
    protected ClassName getRequestType() {
        return ClassName.get(packageStructure.getRequestPackage(), packageStructure.getRequestClassName());
    }

    /**
     * 获取 Response 类型
     */
    protected ClassName getResponseType() {
        return ClassName.get(packageStructure.getResponsePackage(), packageStructure.getResponseClassName());
    }

    /**
     * 获取 Query 类型
     */
    protected ClassName getQueryType(ClassMetadata classMetadata) {
        return ClassName.get(packageStructure.getRequestPackage(), classMetadata.getClassName() + "Query");
    }

    /**
     * 获取 Mapper 类型
     */
    protected ClassName getMapperType() {
        return ClassName.get(packageStructure.getMapperPackage(), packageStructure.getMapperClassName());
    }

    /**
     * 获取 Repository 类型
     */
    protected ClassName getRepositoryType() {
        return ClassName.get(packageStructure.getRepositoryPackage(), packageStructure.getRepositoryClassName());
    }

    /**
     * 获取 Service 类型
     */
    protected ClassName getServiceType() {
        return ClassName.get(packageStructure.getServicePackage(), packageStructure.getServiceClassName());
    }

    /**
     * 获取 ServiceImpl 类型
     */
    protected ClassName getServiceImplType() {
        return ClassName.get(packageStructure.getServiceImplPackage(), packageStructure.getServiceImplClassName());
    }

    /**
     * 获取 Convertor 类型
     */
    protected ClassName getConvertorType() {
        return ClassName.get(packageStructure.getConvertorPackage(), packageStructure.getConvertorClassName());
    }

    // ==================== 字段名辅助 ====================

    /**
     * 获取 Repository 字段名
     */
    protected String getRepositoryFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Repository";
    }

    /**
     * 获取 Convertor 字段名
     */
    protected String getConvertorFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Convertor";
    }

    /**
     * 获取 Service 字段名
     */
    protected String getServiceFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Service";
    }

    /**
     * 获取 DTO 参数名
     */
    protected String getDtoParamName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "DTO";
    }
}
