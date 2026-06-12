package io.github.youngerier.generator.generators;

import io.github.youngerier.support.Pagination;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Service接口生成器
 */
public class ServiceGenerator extends AbstractClassGenerator {

    public ServiceGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        return createInterfaceBuilder(className);
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // 接口无注解
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // 接口无字段
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = classMetadata.getCamelClassName();
        ClassName dtoType = getDtoType();
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        ClassName queryType = getQueryType(classMetadata);

        // createXxx
        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(dtoType, camelEntityName + "DTO")
                .addJavadoc("创建$L\n", entityName)
                .addJavadoc("@param $L $L数据传输对象\n", camelEntityName + "DTO", entityName)
                .addJavadoc("@return 创建的$L对象\n", entityName)
                .build());

        // getXxxById
        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("根据ID查询$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 对应的$L对象\n", entityName)
                .build());

        // queryXxxs
        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listOfDto)
                .addParameter(queryType, "query")
                .addJavadoc("查询所有$L\n", entityName)
                .addJavadoc("@return $L对象列表\n", entityName)
                .build());

        // pageQueryXxxs
        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addParameter(queryType, "query")
                .addJavadoc("分页查询$L\n", entityName)
                .addJavadoc("@return $L对象列表\n", entityName)
                .build());

        // updateXxx
        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dtoType)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, camelEntityName + "DTO")
                .addJavadoc("更新$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@param $L $L数据传输对象\n", camelEntityName + "DTO", entityName)
                .addJavadoc("@return 更新后的$L对象\n", entityName)
                .build());

        // deleteXxx
        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.LONG, "id")
                .addJavadoc("删除$L\n", entityName)
                .addJavadoc("@param id 主键ID\n")
                .addJavadoc("@return 是否删除成功\n")
                .build());
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "服务接口");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getServicePackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getServiceClassName();
    }
}
