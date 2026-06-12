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
 * 代码生成器抽象基类（模板方法模式）
 * <p>
 * 定义固定的生成流程骨架，子类只需实现差异部分：
 * 1. createTypeBuilder  - 创建类或接口构建器
 * 2. addAnnotations     - 添加注解
 * 3. addFields          - 添加字段
 * 4. addMethods         - 添加方法
 */
@Slf4j
public abstract class AbstractClassGenerator implements CodeGenerator {

    // ==================== 常量 ====================
    protected static final String LOMBOK_PACKAGE = "lombok";
    protected static final String SPRING_STEREOTOPE_PACKAGE = "org.springframework.stereotype";
    protected static final String SPRING_WEB_PACKAGE = "org.springframework.web.bind.annotation";

    protected static final Set<String> TIME_FIELDS = Set.of(
            "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    );

    protected static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "gmtCreate", "gmtModified", "createTime", "updateTime", "createdAt", "updatedAt"
    );

    protected final PackageStructure packageStructure;

    protected AbstractClassGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    // ==================== 模板方法 ====================

    /**
     * 模板方法：定义生成流程骨架
     */
    public final TypeSpec generate(ClassMetadata classMetadata) {
        String className = getClassName(classMetadata);
        TypeSpec.Builder builder = createTypeBuilder(className, classMetadata);
        addAnnotations(builder, classMetadata);
        addFields(builder, classMetadata);
        addMethods(builder, classMetadata);
        addClassJavadoc(builder, classMetadata);
        return builder.build();
    }

    // ==================== 抽象方法（子类必须实现） ====================

    /**
     * 创建类型构建器（类或接口）
     */
    protected abstract TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata);

    /**
     * 添加注解
     */
    protected abstract void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata);

    /**
     * 添加字段
     */
    protected abstract void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata);

    /**
     * 添加方法
     */
    protected abstract void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata);

    // ==================== 钩子方法（子类可选重写） ====================

    /**
     * 添加类注释（钩子方法，子类可重写）
     */
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String classComment = classMetadata.getClassComment();
        if (classComment != null && !classComment.isEmpty()) {
            builder.addJavadoc(classComment + "\n");
        }
    }

    /**
     * 添加注释后缀（钩子方法，子类可重写）
     * 默认实现：如果有类注释则添加后缀
     */
    protected void addClassJavadocSuffix(TypeSpec.Builder builder, ClassMetadata classMetadata, String suffix) {
        if (classMetadata.getClassComment() != null && !classMetadata.getClassComment().isEmpty()) {
            builder.addJavadoc(suffix + "\n");
        }
    }

    // ==================== 通用构建方法 ====================

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

    protected AnnotationSpec createServiceAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_STEREOTOPE_PACKAGE, "Service")).build();
    }

    protected AnnotationSpec createRestControllerAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RestController")).build();
    }

    protected AnnotationSpec createRequestMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RequestMapping"))
                .addMember("value", "$S", path).build();
    }

    protected AnnotationSpec createPostMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PostMapping"))
                .addMember("value", "$S", path).build();
    }

    protected AnnotationSpec createGetMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "GetMapping"))
                .addMember("value", "$S", path).build();
    }

    protected AnnotationSpec createPutMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PutMapping"))
                .addMember("value", "$S", path).build();
    }

    protected AnnotationSpec createDeleteMappingAnnotation(String path) {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "DeleteMapping"))
                .addMember("value", "$S", path).build();
    }

    protected AnnotationSpec createRequestBodyAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "RequestBody")).build();
    }

    protected AnnotationSpec createPathVariableAnnotation() {
        return AnnotationSpec.builder(ClassName.get(SPRING_WEB_PACKAGE, "PathVariable")).build();
    }

    // ==================== 参数构建 ====================

    protected ParameterSpec createRequestBodyParam(ClassName type, String name) {
        return ParameterSpec.builder(type, name).addAnnotation(createRequestBodyAnnotation()).build();
    }

    protected ParameterSpec createPathVariableParam(TypeName type, String name) {
        return ParameterSpec.builder(type, name).addAnnotation(createPathVariableAnnotation()).build();
    }

    // ==================== 字段操作 ====================

    protected void addAllFields(TypeSpec.Builder builder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsWithFilter(builder, fields, f -> true, false);
    }

    protected void addFieldsWithFilter(TypeSpec.Builder builder,
                                        List<ClassMetadata.FieldInfo> fields,
                                        Predicate<ClassMetadata.FieldInfo> filter,
                                        boolean addPrimaryKeyComment) {
        for (ClassMetadata.FieldInfo field : fields) {
            if (filter.test(field)) {
                builder.addField(buildFieldSpec(field, addPrimaryKeyComment));
            }
        }
    }

    protected void addFieldsExcluding(TypeSpec.Builder builder,
                                       List<ClassMetadata.FieldInfo> fields,
                                       Set<String> excludedFields) {
        addFieldsWithFilter(builder, fields, f -> !excludedFields.contains(f.getName()), false);
    }

    protected void addFieldsExcludingSystem(TypeSpec.Builder builder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsExcluding(builder, fields, SYSTEM_FIELDS);
    }

    protected void addFieldsByType(TypeSpec.Builder builder,
                                    List<ClassMetadata.FieldInfo> fields,
                                    Class<?>... types) {
        Set<String> typeNames = java.util.Arrays.stream(types)
                .map(Class::getSimpleName).collect(Collectors.toSet());
        addFieldsWithFilter(builder, fields,
                f -> f.getType() != null && typeNames.contains(f.getType().toString()), false);
    }

    protected void addPrimaryKeyField(TypeSpec.Builder builder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsWithFilter(builder, fields, ClassMetadata.FieldInfo::isPrimaryKey, true);
    }

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

    // ==================== 类型获取 ====================

    protected ClassName getEntityType(ClassMetadata classMetadata) {
        return ClassName.get(classMetadata.getPackageName(), classMetadata.getClassName());
    }

    protected ClassName getDtoType() {
        return ClassName.get(packageStructure.getDtoPackage(), packageStructure.getDtoClassName());
    }

    protected ClassName getRequestType() {
        return ClassName.get(packageStructure.getRequestPackage(), packageStructure.getRequestClassName());
    }

    protected ClassName getResponseType() {
        return ClassName.get(packageStructure.getResponsePackage(), packageStructure.getResponseClassName());
    }

    protected ClassName getQueryType(ClassMetadata classMetadata) {
        return ClassName.get(packageStructure.getRequestPackage(), classMetadata.getClassName() + "Query");
    }

    protected ClassName getMapperType() {
        return ClassName.get(packageStructure.getMapperPackage(), packageStructure.getMapperClassName());
    }

    protected ClassName getRepositoryType() {
        return ClassName.get(packageStructure.getRepositoryPackage(), packageStructure.getRepositoryClassName());
    }

    protected ClassName getServiceType() {
        return ClassName.get(packageStructure.getServicePackage(), packageStructure.getServiceClassName());
    }

    protected ClassName getServiceImplType() {
        return ClassName.get(packageStructure.getServiceImplPackage(), packageStructure.getServiceImplClassName());
    }

    protected ClassName getConvertorType() {
        return ClassName.get(packageStructure.getConvertorPackage(), packageStructure.getConvertorClassName());
    }

    // ==================== 字段名辅助 ====================

    protected String getRepositoryFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Repository";
    }

    protected String getConvertorFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Convertor";
    }

    protected String getServiceFieldName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "Service";
    }

    protected String getDtoParamName(ClassMetadata classMetadata) {
        return classMetadata.getCamelClassName() + "DTO";
    }
}
