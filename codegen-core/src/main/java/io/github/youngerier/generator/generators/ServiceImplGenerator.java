package io.github.youngerier.generator.generators;

import io.github.youngerier.support.Pagination;
import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.util.StringCaseUtils;
import com.squareup.javapoet.*;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service实现类生成器
 */
@Slf4j
public class ServiceImplGenerator extends AbstractClassGenerator {

    public ServiceImplGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        ClassName serviceType = getServiceType();
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(createServiceAnnotation())
                .addSuperinterface(serviceType);
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // @Service 已在 createTypeBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String camelEntityName = classMetadata.getCamelClassName();
        ClassName repositoryType = getRepositoryType();
        ClassName convertorType = getConvertorType();

        builder.addField(FieldSpec.builder(repositoryType, camelEntityName + "Repository")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build());

        builder.addField(FieldSpec.builder(convertorType, camelEntityName + "Convertor")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$T.INSTANCE", convertorType).build());
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        addConstructor(builder, classMetadata);
        addCrudMethods(builder, classMetadata);
    }

    private void addConstructor(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String repositoryFieldName = getRepositoryFieldName(classMetadata);
        ClassName repositoryType = getRepositoryType();

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryType, repositoryFieldName)
                .addStatement("this.$N = $N", repositoryFieldName, repositoryFieldName)
                .build());
    }

    private void addCrudMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = classMetadata.getCamelClassName();
        String repositoryFieldName = getRepositoryFieldName(classMetadata);
        String convertorFieldName = getConvertorFieldName(classMetadata);

        ClassName entityType = getEntityType(classMetadata);
        ClassName dtoType = getDtoType();
        ClassName queryType = getQueryType(classMetadata);

        // createXxx
        builder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(dtoType, camelEntityName + "DTO").returns(dtoType)
                .addStatement("$T entity = $N.toEntity($N)", entityType, convertorFieldName, camelEntityName + "DTO")
                .addStatement("$N.save(entity)", repositoryFieldName)
                .addStatement("return $N.toDto(entity)", convertorFieldName)
                .build());

        // getXxxById
        builder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id").returns(dtoType)
                .addStatement("$T entity = $N.getById(id)", entityType, repositoryFieldName)
                .addStatement("return $N.toDto(entity)", convertorFieldName)
                .build());

        // queryXxxs
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        builder.addMethod(MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(queryType, "query").returns(listOfDto)
                .addStatement("return $N.selectListByQuery(query).stream().map($N::toDto).collect($T.toList())",
                        repositoryFieldName, convertorFieldName, Collectors.class)
                .build());

        // pageQueryXxxs
        ClassName pageType = ClassName.get("com.mybatisflex.core.paginate", "Page");
        builder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addStatement("$T<$T> page = $N.page(query).map($N::toDto)",
                        pageType, dtoType, repositoryFieldName, convertorFieldName)
                .addStatement("return $T.of(page.getRecords(), query, page.getTotalRow())", Pagination.class)
                .build());

        // updateXxx
        builder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, camelEntityName + "DTO").returns(dtoType)
                .addStatement("$T existingEntity = $N.getById(id)", entityType, repositoryFieldName)
                .beginControlFlow("if (existingEntity == null)")
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T updatedEntity = $N.toEntity($N)", entityType, convertorFieldName, camelEntityName + "DTO")
                .addStatement("updatedEntity.setId(id)")
                .addStatement("$N.updateById(updatedEntity)", repositoryFieldName)
                .addStatement("return $N.toDto(updatedEntity)", convertorFieldName)
                .build());

        // deleteXxx
        builder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .addParameter(long.class, "id").returns(TypeName.BOOLEAN)
                .addStatement("return $N.removeById(id)", repositoryFieldName)
                .build());
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "服务实现类");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getServiceImplPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getServiceImplClassName();
    }
}
