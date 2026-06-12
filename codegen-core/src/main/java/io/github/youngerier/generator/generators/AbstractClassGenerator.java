package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.CodeGenerator;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.model.PackageStructure;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 代码生成器抽象基类，提供通用的类和字段生成逻辑
 */
public abstract class AbstractClassGenerator implements CodeGenerator {

    protected final PackageStructure packageStructure;

    protected AbstractClassGenerator(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
    }

    /**
     * 创建类构建器（添加 @Data 注解）
     *
     * @param className 类名
     * @return TypeSpec.Builder
     */
    protected TypeSpec.Builder createClassBuilder(String className) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("lombok", "Data"));
    }

    /**
     * 创建接口构建器
     *
     * @param interfaceName 接口名
     * @return TypeSpec.Builder
     */
    protected TypeSpec.Builder createInterfaceBuilder(String interfaceName) {
        return TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC);
    }

    /**
     * 添加类注释
     *
     * @param classBuilder 类构建器
     * @param classMetadata 类元数据
     * @param suffix 后缀描述（如 "数据传输对象(DTO)"）
     */
    protected void addClassJavadoc(TypeSpec.Builder classBuilder, ClassMetadata classMetadata, String suffix) {
        String classComment = classMetadata.getClassComment();
        if (classComment != null && !classComment.isEmpty()) {
            classBuilder.addJavadoc(classComment + "\n");
            classBuilder.addJavadoc(suffix + "\n");
        }
    }

    /**
     * 添加所有字段（带注释）
     *
     * @param classBuilder 类构建器
     * @param fields 字段列表
     */
    protected void addAllFields(TypeSpec.Builder classBuilder, List<ClassMetadata.FieldInfo> fields) {
        addFieldsWithFilter(classBuilder, fields, field -> true, false);
    }

    /**
     * 添加字段（带过滤条件）
     *
     * @param classBuilder 类构建器
     * @param fields 字段列表
     * @param filter 过滤条件
     * @param addPrimaryKeyComment 是否为主键添加注释
     */
    protected void addFieldsWithFilter(TypeSpec.Builder classBuilder,
                                        List<ClassMetadata.FieldInfo> fields,
                                        Predicate<ClassMetadata.FieldInfo> filter,
                                        boolean addPrimaryKeyComment) {
        for (ClassMetadata.FieldInfo field : fields) {
            if (!filter.test(field)) {
                continue;
            }

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                    field.getType(),
                    field.getName(),
                    Modifier.PRIVATE
            );

            // 添加字段注释
            String comment = field.getComment();
            if (comment != null && !comment.isEmpty()) {
                fieldBuilder.addJavadoc(comment + "\n");
            }

            // 如果是主键，添加注释说明
            if (addPrimaryKeyComment && field.isPrimaryKey()) {
                fieldBuilder.addJavadoc("主键ID\n");
            }

            classBuilder.addField(fieldBuilder.build());
        }
    }

    /**
     * 添加字段（带排除列表）
     *
     * @param classBuilder 类构建器
     * @param fields 字段列表
     * @param excludedFields 需要排除的字段名集合
     */
    protected void addFieldsExcluding(TypeSpec.Builder classBuilder,
                                       List<ClassMetadata.FieldInfo> fields,
                                       Set<String> excludedFields) {
        addFieldsWithFilter(classBuilder, fields, field -> !excludedFields.contains(field.getName()), false);
    }

    /**
     * 获取实体类型
     *
     * @param classMetadata 类元数据
     * @return 实体类型 ClassName
     */
    protected ClassName getEntityType(ClassMetadata classMetadata) {
        return ClassName.get(classMetadata.getPackageName(), classMetadata.getClassName());
    }
}
