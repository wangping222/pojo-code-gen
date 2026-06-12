package io.github.youngerier.generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.ClassName;
import io.github.youngerier.generator.model.ClassMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static io.github.youngerier.generator.GeneratorConstants.SRC_MAIN_JAVA;
import static io.github.youngerier.generator.GeneratorConstants.SRC_TEST_JAVA;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        File sourceFile = findSourceFile(moduleName, clazz);
        JavaParser javaParser = createParser(sourceFile);

        ParseResult<CompilationUnit> parseResult = javaParser.parse(sourceFile);
        CompilationUnit cu = parseResult.getResult().orElseThrow(() ->
                new IOException("Failed to parse source file: " + sourceFile.getAbsolutePath() + ", problems: " + parseResult.getProblems()));

        ClassMetadata classMetadata = new ClassMetadata();
        classMetadata.setPackageName(clazz.getPackage().getName());

        cu.getClassByName(clazz.getSimpleName()).ifPresent(cls -> {
            classMetadata.setClassName(cls.getNameAsString());
            classMetadata.setClassComment(extractComment(cls));
            extractFields(cls, classMetadata);
        });

        return classMetadata;
    }

    private File findSourceFile(String moduleName, Class<?> clazz) throws IOException {
        // 1. 尝试从 classpath 转换路径
        File sourceFile = getSourceFileFromClasspath(clazz);
        if (sourceFile != null && sourceFile.exists()) {
            return sourceFile;
        }

        // 2. 尝试通过 ProtectionDomain 获取
        sourceFile = getSourceFileFromProtectionDomain(clazz);
        if (sourceFile != null && sourceFile.exists()) {
            return sourceFile;
        }

        // 3. 在项目目录中查找
        return findSourceInProject(moduleName, clazz);
    }

    private File getSourceFileFromClasspath(Class<?> clazz) {
        try {
            var resource = clazz.getResource(clazz.getSimpleName() + ".class");
            if (resource == null) {
                return null;
            }
            String classPath = Paths.get(resource.toURI()).toString();
            String sourcePath = classPath
                    .replaceAll("[/\\\\]target[/\\\\]classes", File.separator + SRC_MAIN_JAVA)
                    .replaceAll("[/\\\\]build[/\\\\]classes[/\\\\]java[/\\\\]main", File.separator + SRC_MAIN_JAVA)
                    .replaceAll(clazz.getSimpleName() + "\\.class$", clazz.getSimpleName() + ".java");
            return new File(sourcePath);
        } catch (Exception e) {
            return null;
        }
    }

    private File getSourceFileFromProtectionDomain(Class<?> clazz) {
        try {
            var protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null || protectionDomain.getCodeSource() == null) {
                return null;
            }
            var codeSource = protectionDomain.getCodeSource();
            if (codeSource.getLocation() == null) {
                return null;
            }
            String classLocation = codeSource.getLocation().getPath();
            if (classLocation.endsWith(".jar")) {
                return null;
            }
            String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
            String sourceFilePath = classLocation.replace("target/classes", "src/main/java")
                    + packagePath + File.separator + clazz.getSimpleName() + ".java";
            return new File(sourceFilePath);
        } catch (Exception e) {
            return null;
        }
    }

    private File findSourceInProject(String moduleName, Class<?> clazz) throws IOException {
        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        String fileName = clazz.getSimpleName() + ".java";
        File projectRoot = new File(System.getProperty("user.dir")).getAbsoluteFile();

        // 优先在指定模块中查找
        if (moduleName != null && !moduleName.isEmpty()) {
            File found = searchInModule(new File(projectRoot, moduleName), packagePath, fileName);
            if (found != null) {
                return found;
            }
        }

        // 在所有模块中查找
        File[] modules = projectRoot.listFiles(File::isDirectory);
        if (modules != null) {
            for (File module : modules) {
                if (!new File(module, "pom.xml").exists()) {
                    continue;
                }
                File found = searchInModule(module, packagePath, fileName);
                if (found != null) {
                    return found;
                }
            }
        }

        throw new IOException("Source file not found for class: " + clazz.getName());
    }

    private File searchInModule(File moduleDir, String packagePath, String fileName) {
        for (String srcDir : new String[]{SRC_MAIN_JAVA, SRC_TEST_JAVA}) {
            File sourceFile = new File(moduleDir, srcDir + File.separator + packagePath + File.separator + fileName);
            if (sourceFile.exists()) {
                return sourceFile;
            }
        }
        return null;
    }

    private JavaParser createParser(File sourceFile) {
        File srcMainJavaDir = findSrcMainJavaDir(sourceFile);
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(srcMainJavaDir));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        return new JavaParser(config);
    }

    private File findSrcMainJavaDir(File sourceFile) {
        Path current = sourceFile.toPath().toAbsolutePath().normalize().getParent();
        while (current != null) {
            Path srcMainJava = current.resolve(SRC_MAIN_JAVA);
            if (srcMainJava.toFile().exists()) {
                return srcMainJava.toFile();
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find src/main/java directory for source file: " + sourceFile.getAbsolutePath());
    }

    private void extractFields(ClassOrInterfaceDeclaration cls, ClassMetadata classMetadata) {
        for (FieldDeclaration fieldDecl : cls.getFields()) {
            if (fieldDecl.isStatic()) {
                continue;
            }
            for (VariableDeclarator var : fieldDecl.getVariables()) {
                ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
                fieldInfo.setName(var.getNameAsString());

                try {
                    ResolvedType resolvedType = var.getType().resolve();
                    fieldInfo.setFullType(resolvedType.describe());
                    fieldInfo.setType(ClassName.bestGuess(resolvedType.describe()));
                } catch (Exception e) {
                    fieldInfo.setType(ClassName.bestGuess(var.getTypeAsString()));
                    fieldInfo.setFullType(var.getTypeAsString());
                }

                fieldInfo.setComment(extractComment(fieldDecl));
                fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));
                classMetadata.getFields().add(fieldInfo);
            }
        }
    }

    private String extractComment(Node node) {
        return node.getComment()
                .map(comment -> comment.getContent().trim().replaceAll("\\*", "").trim())
                .orElse("");
    }

    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}
