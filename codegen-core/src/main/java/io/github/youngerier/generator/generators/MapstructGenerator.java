package io.github.youngerier.generator.generators;

import io.github.youngerier.generator.model.PackageStructure;
import io.github.youngerier.generator.model.ClassMetadata;
import io.github.youngerier.generator.util.StringCaseUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * MapStruct转换器生成器
 */
public class MapstructGenerator extends AbstractClassGenerator {

    public MapstructGenerator(PackageStructure packageStructure) {
        super(packageStructure);
    }

    @Override
    protected TypeSpec.Builder createTypeBuilder(String className, ClassMetadata classMetadata) {
        return createInterfaceBuilder(className)
                .addAnnotation(ClassName.get("org.mapstruct", "Mapper"));
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        // @Mapper 已在 createTypeBuilder 中添加
    }

    @Override
    protected void addFields(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        ClassName convertorType = getConvertorType();
        builder.addField(FieldSpec.builder(convertorType, "INSTANCE")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getMapper($T.class)",
                        ClassName.get("org.mapstruct.factory", "Mappers"), convertorType)
                .build());
    }

    @Override
    protected void addMethods(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        String camelEntityName = classMetadata.getCamelClassName();
        ClassName entityType = getEntityType(classMetadata);
        ClassName dtoType = getDtoType();
        ClassName requestType = getRequestType();
        ClassName responseType = getResponseType();

        ParameterizedTypeName listOfEntity = ParameterizedTypeName.get(ClassName.get(List.class), entityType);
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        ParameterizedTypeName listOfResponse = ParameterizedTypeName.get(ClassName.get(List.class), responseType);

        builder.addMethod(createToDtoMethod(entityType, dtoType, camelEntityName));
        builder.addMethod(createToDtoFromRequestMethod(requestType, dtoType, camelEntityName));
        builder.addMethod(createToEntityMethod(dtoType, entityType, camelEntityName));
        builder.addMethod(createToEntityFromRequestMethod(requestType, entityType, camelEntityName));
        builder.addMethod(createToResponseMethod(entityType, responseType, camelEntityName));
        builder.addMethod(createToDtoListMethod(listOfEntity, listOfDto, camelEntityName));
        builder.addMethod(createToResponseListMethod(listOfEntity, listOfResponse, camelEntityName));
    }

    private MethodSpec createToDtoMethod(ClassName entityType, ClassName dtoType, String name) {
        return MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, name).returns(dtoType).build();
    }

    private MethodSpec createToDtoFromRequestMethod(ClassName requestType, ClassName dtoType, String name) {
        return MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(requestType, name + "Request").returns(dtoType).build();
    }

    private MethodSpec createToEntityMethod(ClassName dtoType, ClassName entityType, String name) {
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoType, name + "DTO").returns(entityType).build();
    }

    private MethodSpec createToEntityFromRequestMethod(ClassName requestType, ClassName entityType, String name) {
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(requestType, name + "Request").returns(entityType).build();
    }

    private MethodSpec createToResponseMethod(ClassName entityType, ClassName responseType, String name) {
        return MethodSpec.methodBuilder("toResponse")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, name).returns(responseType).build();
    }

    private MethodSpec createToDtoListMethod(ParameterizedTypeName listOfEntity, ParameterizedTypeName listOfDto, String name) {
        return MethodSpec.methodBuilder("toDtoList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, name + "List").returns(listOfDto).build();
    }

    private MethodSpec createToResponseListMethod(ParameterizedTypeName listOfEntity, ParameterizedTypeName listOfResponse, String name) {
        return MethodSpec.methodBuilder("toResponseList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, name + "List").returns(listOfResponse).build();
    }

    @Override
    protected void addClassJavadoc(TypeSpec.Builder builder, ClassMetadata classMetadata) {
        super.addClassJavadoc(builder, classMetadata);
        addClassJavadocSuffix(builder, classMetadata, "对象转换器");
    }

    @Override
    public String getPackageName() {
        return packageStructure.getConvertorPackage();
    }

    @Override
    public String getClassName(ClassMetadata classMetadata) {
        return packageStructure.getConvertorClassName();
    }
}
