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
import java.util.stream.Stream;

import static io.github.youngerier.generator.GeneratorConstants.SRC_MAIN_JAVA;
import static io.github.youngerier.generator.GeneratorConstants.SRC_TEST_JAVA;

/**
 * 源码分析器，使用JavaParser解析Java源码并提取类元数据信息
 */
@Slf4j
public class SourceCodeAnalyzer {

    private static final String[] SRC_DIRS = {SRC_MAIN_JAVA, SRC_TEST_JAVA};

    public ClassMetadata parse(Class<?> clazz, String moduleName) throws IOException {
        File sourceFile = findSourceFile(moduleName, clazz);
        JavaParser javaParser = createParser(sourceFile);

        ParseResult<CompilationUnit> parseResult = javaParser.parse(sourceFile);
        CompilationUnit cu = parseResult.getResult().orElseThrow(() ->
                new IOException("Failed to parse: " + sourceFile.getAbsolutePath() + ", problems: " + parseResult.getProblems()));

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
        File sourceFile = getSourceFileFromClasspath(clazz);
        if (sourceFile != null && sourceFile.exists()) {
            return sourceFile;
        }
        return findSourceInProject(moduleName, clazz);
    }

    private File getSourceFileFromClasspath(Class<?> clazz) {
        try {
            var resource = clazz.getResource(clazz.getSimpleName() + ".class");
            if (resource == null) {
                log.debug("No .class resource found for: {}", clazz.getName());
                return null;
            }
            String path = Paths.get(resource.toURI()).toString();
            log.debug("Class file path: {}", path);

            path = path
                    .replaceAll("[/\\\\]target[/\\\\]classes", File.separator + SRC_MAIN_JAVA)
                    .replaceAll("[/\\\\]build[/\\\\]classes[/\\\\]java[/\\\\]main", File.separator + SRC_MAIN_JAVA)
                    .replace(clazz.getSimpleName() + ".class", clazz.getSimpleName() + ".java");

            log.debug("Converted source path: {}", path);
            File sourceFile = new File(path);
            log.debug("Source file exists: {}", sourceFile.exists());
            return sourceFile;
        } catch (Exception e) {
            log.debug("Error resolving source file for {}: {}", clazz.getName(), e.getMessage());
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

        // 递归查找所有 Maven 模块
        File found = findInSubModules(projectRoot, packagePath, fileName);
        if (found != null) {
            return found;
        }

        throw new IOException("Source file not found for class: " + clazz.getName());
    }

    private File findInSubModules(File dir, String packagePath, String fileName) {
        // 检查当前目录是否是 Maven 模块
        if (new File(dir, "pom.xml").exists()) {
            File found = searchInModule(dir, packagePath, fileName);
            if (found != null) {
                return found;
            }
        }
        // 递归查找子目录
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                // 跳过 .git、target、.idea 等目录
                String name = subDir.getName();
                if (name.startsWith(".") || name.equals("target")) {
                    continue;
                }
                File found = findInSubModules(subDir, packagePath, fileName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private File searchInModule(File moduleDir, String packagePath, String fileName) {
        return Stream.of(SRC_DIRS)
                .map(srcDir -> new File(moduleDir, srcDir + File.separator + packagePath + File.separator + fileName))
                .filter(File::exists)
                .findFirst()
                .orElse(null);
    }

    private JavaParser createParser(File sourceFile) {
        File srcMainJavaDir = findSrcMainJavaDir(sourceFile);

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(srcMainJavaDir));

        ParserConfiguration config = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        return new JavaParser(config);
    }

    private File findSrcMainJavaDir(File sourceFile) {
        return Stream.iterate(sourceFile.toPath().toAbsolutePath().normalize().getParent(), Path::getParent)
                .filter(p -> p.resolve(SRC_MAIN_JAVA).toFile().exists())
                .map(p -> p.resolve(SRC_MAIN_JAVA).toFile())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find src/main/java for: " + sourceFile));
    }

    private void extractFields(ClassOrInterfaceDeclaration cls, ClassMetadata classMetadata) {
        cls.getFields().stream()
                .filter(f -> !f.isStatic())
                .flatMap(f -> f.getVariables().stream())
                .forEach(var -> classMetadata.getFields().add(buildFieldInfo(var)));
    }

    private ClassMetadata.FieldInfo buildFieldInfo(VariableDeclarator var) {
        ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
        fieldInfo.setName(var.getNameAsString());

        String typeStr;
        try {
            ResolvedType resolved = var.getType().resolve();
            typeStr = resolved.describe();
        } catch (Exception e) {
            typeStr = var.getTypeAsString();
        }
        fieldInfo.setType(ClassName.bestGuess(typeStr));
        fieldInfo.setFullType(typeStr);

        fieldInfo.setComment(extractComment(var));
        fieldInfo.setPrimaryKey(isPrimaryKey(fieldInfo.getName()));
        return fieldInfo;
    }

    private String extractComment(Node node) {
        return node.getComment()
                .map(c -> c.getContent().lines()
                        .map(l -> l.replaceFirst("^\\s*\\*\\s?", "").trim())
                        .filter(l -> !l.isEmpty())
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse(""))
                .orElse("");
    }

    private boolean isPrimaryKey(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}
