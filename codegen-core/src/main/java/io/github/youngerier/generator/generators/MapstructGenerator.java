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
    public TypeSpec generate(ClassMetadata classMetadata) {
        String entityName = classMetadata.getClassName();
        String camelEntityName = StringCaseUtils.lowerFirstChar(entityName);

        ClassName entityType = getEntityType(classMetadata);
        ClassName dtoType = ClassName.get(packageStructure.getDtoPackage(), packageStructure.getDtoClassName());
        ClassName requestType = ClassName.get(packageStructure.getRequestPackage(), packageStructure.getRequestClassName());
        ClassName responseType = ClassName.get(packageStructure.getResponsePackage(), packageStructure.getResponseClassName());

        ParameterizedTypeName listOfEntity = ParameterizedTypeName.get(ClassName.get(List.class), entityType);
        ParameterizedTypeName listOfDto = ParameterizedTypeName.get(ClassName.get(List.class), dtoType);
        ParameterizedTypeName listOfResponse = ParameterizedTypeName.get(ClassName.get(List.class), responseType);

        TypeSpec.Builder interfaceBuilder = createInterfaceBuilder(getClassName(classMetadata))
                .addAnnotation(ClassName.get("org.mapstruct", "Mapper"));

        // 添加INSTANCE常量
        ClassName convertorType = ClassName.get(packageStructure.getConvertorPackage(), getClassName(classMetadata));
        interfaceBuilder.addField(FieldSpec.builder(convertorType, "INSTANCE")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getMapper($T.class)",
                        ClassName.get("org.mapstruct.factory", "Mappers"), convertorType)
                .build());

        // 添加转换方法
        interfaceBuilder.addMethod(createToDtoMethod(entityType, dtoType, camelEntityName));
        interfaceBuilder.addMethod(createToDtoFromRequestMethod(requestType, dtoType, camelEntityName));
        interfaceBuilder.addMethod(createToEntityMethod(dtoType, entityType, camelEntityName));
        interfaceBuilder.addMethod(createToEntityFromRequestMethod(requestType, entityType, camelEntityName));
        interfaceBuilder.addMethod(createToResponseMethod(entityType, responseType, camelEntityName));
        interfaceBuilder.addMethod(createToDtoListMethod(listOfEntity, listOfDto, camelEntityName));
        interfaceBuilder.addMethod(createToResponseListMethod(listOfEntity, listOfResponse, camelEntityName));

        addClassJavadoc(interfaceBuilder, classMetadata, "对象转换器");

        return interfaceBuilder.build();
    }

    private MethodSpec createToDtoMethod(ClassName entityType, ClassName dtoType, String camelEntityName) {
        return MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, camelEntityName)
                .returns(dtoType)
                .build();
    }

    private MethodSpec createToDtoFromRequestMethod(ClassName requestType, ClassName dtoType, String camelEntityName) {
        return MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(requestType, camelEntityName + "Request")
                .returns(dtoType)
                .build();
    }

    private MethodSpec createToEntityMethod(ClassName dtoType, ClassName entityType, String camelEntityName) {
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoType, camelEntityName + "DTO")
                .returns(entityType)
                .build();
    }

    private MethodSpec createToEntityFromRequestMethod(ClassName requestType, ClassName entityType, String camelEntityName) {
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(requestType, camelEntityName + "Request")
                .returns(entityType)
                .build();
    }

    private MethodSpec createToResponseMethod(ClassName entityType, ClassName responseType, String camelEntityName) {
        return MethodSpec.methodBuilder("toResponse")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, camelEntityName)
                .returns(responseType)
                .build();
    }

    private MethodSpec createToDtoListMethod(ParameterizedTypeName listOfEntity, ParameterizedTypeName listOfDto, String camelEntityName) {
        return MethodSpec.methodBuilder("toDtoList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, camelEntityName + "List")
                .returns(listOfDto)
                .build();
    }

    private MethodSpec createToResponseListMethod(ParameterizedTypeName listOfEntity, ParameterizedTypeName listOfResponse, String camelEntityName) {
        return MethodSpec.methodBuilder("toResponseList")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(listOfEntity, camelEntityName + "List")
                .returns(listOfResponse)
                .build();
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
