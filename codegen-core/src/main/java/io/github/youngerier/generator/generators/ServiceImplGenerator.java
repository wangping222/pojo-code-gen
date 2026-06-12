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
    public TypeSpec generate(ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = classMetadata.getCamelClassName();

        ClassName entityType = getEntityType(classMetadata);
        ClassName dtoType = ClassName.get(packageStructure.getDtoPackage(), packageStructure.getDtoClassName());
        ClassName serviceType = ClassName.get(packageStructure.getServicePackage(), packageStructure.getServiceClassName());
        ClassName repositoryType = ClassName.get(packageStructure.getRepositoryPackage(), packageStructure.getRepositoryClassName());
        ClassName mapperType = ClassName.get(packageStructure.getConvertorPackage(), packageStructure.getConvertorClassName());

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getClassName(classMetadata))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addSuperinterface(serviceType);

        addClassJavadoc(classBuilder, classMetadata, "服务实现类");
        addRepositoryAndMapperFields(classBuilder, camelEntityName, repositoryType, mapperType);
        addConstructor(classBuilder, camelEntityName, repositoryType);
        addServiceMethods(classBuilder, entityName, camelEntityName, entityType, dtoType, repositoryType, mapperType);

        return classBuilder.build();
    }

    private void addRepositoryAndMapperFields(TypeSpec.Builder classBuilder, String camelEntityName,
                                               ClassName repositoryType, ClassName mapperType) {
        classBuilder.addField(FieldSpec.builder(repositoryType, camelEntityName + "Repository")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());

        classBuilder.addField(FieldSpec.builder(mapperType, camelEntityName + "Convertor")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$T.INSTANCE", mapperType)
                .build());
    }

    private void addConstructor(TypeSpec.Builder classBuilder, String camelEntityName, ClassName repositoryType) {
        String repositoryFieldName = camelEntityName + "Repository";
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryType, repositoryFieldName)
                .addStatement("this.$N = $N", repositoryFieldName, repositoryFieldName)
                .build());
    }

    private void addServiceMethods(TypeSpec.Builder classBuilder, String entityName, String camelEntityName,
                                    ClassName entityType, ClassName dtoType,
                                    ClassName repositoryType, ClassName mapperType) {
        String repositoryFieldName = camelEntityName + "Repository";
        String mapperFieldName = camelEntityName + "Convertor";

        // createXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoType, camelEntityName + "DTO")
                .returns(dtoType)
                .addStatement("$T entity = $N.toEntity($N)", entityType, mapperFieldName, camelEntityName + "DTO")
                .addStatement("$N.save(entity)", repositoryFieldName)
                .addStatement("return $N.toDto(entity)", mapperFieldName)
                .build());

        // getXxxById
        classBuilder.addMethod(MethodSpec.methodBuilder("get" + entityName + "ById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .returns(dtoType)
                .addStatement("$T entity = $N.getById(id)", entityType, repositoryFieldName)
                .addStatement("return $N.toDto(entity)", mapperFieldName)
                .build());

        // queryXxxs
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        ClassName queryType = ClassName.get(packageStructure.getRequestPackage(), entityName + "Query");
        classBuilder.addMethod(MethodSpec.methodBuilder("query" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(queryType, "query")
                .returns(listOfDto)
                .addStatement("return $N.selectListByQuery(query).stream().map($N::toDto).collect($T.toList())",
                        repositoryFieldName, mapperFieldName, Collectors.class)
                .build());

        // pageQueryXxxs
        ClassName pageType = ClassName.get("com.mybatisflex.core.paginate", "Page");
        classBuilder.addMethod(MethodSpec.methodBuilder("pageQuery" + entityName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Pagination.class), dtoType))
                .addStatement("$T<$T> page = $N.page(query).map($N::toDto)", pageType, dtoType, repositoryFieldName, mapperFieldName)
                .addStatement("return $T.of(page.getRecords(), query, page.getTotalRow())", Pagination.class)
                .build());

        // updateXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.LONG, "id")
                .addParameter(dtoType, camelEntityName + "DTO")
                .returns(dtoType)
                .addStatement("$T existingEntity = $N.getById(id)", entityType, repositoryFieldName)
                .beginControlFlow("if (existingEntity == null)")
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T updatedEntity = $N.toEntity($N)", entityType, mapperFieldName, camelEntityName + "DTO")
                .addStatement("updatedEntity.setId(id)")
                .addStatement("$N.updateById(updatedEntity)", repositoryFieldName)
                .addStatement("return $N.toDto(updatedEntity)", mapperFieldName)
                .build());

        // deleteXxx
        classBuilder.addMethod(MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(long.class, "id")
                .returns(TypeName.BOOLEAN)
                .addStatement("return $N.removeById(id)", repositoryFieldName)
                .build());
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
