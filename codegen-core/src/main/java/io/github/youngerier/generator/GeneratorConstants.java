package io.github.youngerier.generator;

import java.io.File;

/**
 * 代码生成器常量定义
 */
public final class GeneratorConstants {

    private GeneratorConstants() {
    }

    /**
     * 标准 Maven 源码目录路径
     */
    public static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

    /**
     * 标准 Maven 测试源码目录路径
     */
    public static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";

    /**
     * 默认缩进（4个空格）
     */
    public static final String DEFAULT_INDENT = "    ";
}
