package net.benbenmc;

import com.google.gson.JsonObject;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jar文件统计信息（增强版：包含指令分类统计）
 */
public class JarStatistics {
    // 指令类别枚举
    public enum InsnCategory {
        CONST_LOAD("常量加载"),
        LOCAL_LOAD("局部变量加载"),
        LOCAL_STORE("局部变量存储"),
        ARRAY_LOAD("数组加载"),
        ARRAY_STORE("数组存储"),
        STACK("栈操作"),
        ARITHMETIC("算术运算"),
        CONVERSION("类型转换"),
        COMPARISON("比较"),
        CONTROL("控制转移"),
        METHOD_CALL("方法调用"),
        METHOD_RETURN("方法返回"),
        OBJECT("对象操作"),
        ARRAY_OP("数组操作"),
        EXCEPTION("异常处理"),
        SYNCHRONIZATION("同步"),
        NOP("空操作"),
        OTHER("其他");

        private final String displayName;

        InsnCategory(String name) {
            this.displayName = name;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 指令类别映射表（256大小，操作码值0~201）
    private static final InsnCategory[] OPCODE_CATEGORY = new InsnCategory[256];

    static {
        // 常量加载指令
        setCategory(Opcodes.ACONST_NULL, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_M1, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_0, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_1, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_2, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_3, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_4, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.ICONST_5, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.LCONST_0, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.LCONST_1, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.FCONST_0, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.FCONST_1, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.FCONST_2, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.DCONST_0, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.DCONST_1, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.BIPUSH, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.SIPUSH, InsnCategory.CONST_LOAD);
        setCategory(Opcodes.LDC, InsnCategory.CONST_LOAD);

        // 局部变量加载
        setCategory(Opcodes.ILOAD, InsnCategory.LOCAL_LOAD);
        setCategory(Opcodes.LLOAD, InsnCategory.LOCAL_LOAD);
        setCategory(Opcodes.FLOAD, InsnCategory.LOCAL_LOAD);
        setCategory(Opcodes.DLOAD, InsnCategory.LOCAL_LOAD);
        setCategory(Opcodes.ALOAD, InsnCategory.LOCAL_LOAD);

        // 局部变量存储
        setCategory(Opcodes.ISTORE, InsnCategory.LOCAL_STORE);
        setCategory(Opcodes.LSTORE, InsnCategory.LOCAL_STORE);
        setCategory(Opcodes.FSTORE, InsnCategory.LOCAL_STORE);
        setCategory(Opcodes.DSTORE, InsnCategory.LOCAL_STORE);
        setCategory(Opcodes.ASTORE, InsnCategory.LOCAL_STORE);

        // 数组加载
        setCategory(Opcodes.IALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.LALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.FALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.DALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.AALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.BALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.CALOAD, InsnCategory.ARRAY_LOAD);
        setCategory(Opcodes.SALOAD, InsnCategory.ARRAY_LOAD);

        // 数组存储
        setCategory(Opcodes.IASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.LASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.FASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.DASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.AASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.BASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.CASTORE, InsnCategory.ARRAY_STORE);
        setCategory(Opcodes.SASTORE, InsnCategory.ARRAY_STORE);

        // 栈操作
        setCategory(Opcodes.POP, InsnCategory.STACK);
        setCategory(Opcodes.POP2, InsnCategory.STACK);
        setCategory(Opcodes.DUP, InsnCategory.STACK);
        setCategory(Opcodes.DUP_X1, InsnCategory.STACK);
        setCategory(Opcodes.DUP_X2, InsnCategory.STACK);
        setCategory(Opcodes.DUP2, InsnCategory.STACK);
        setCategory(Opcodes.DUP2_X1, InsnCategory.STACK);
        setCategory(Opcodes.DUP2_X2, InsnCategory.STACK);
        setCategory(Opcodes.SWAP, InsnCategory.STACK);

        // 算术运算
        setCategory(Opcodes.IADD, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LADD, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FADD, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DADD, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.ISUB, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LSUB, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FSUB, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DSUB, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IMUL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LMUL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FMUL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DMUL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IDIV, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LDIV, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FDIV, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DDIV, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IREM, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LREM, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FREM, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DREM, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.INEG, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LNEG, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.FNEG, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.DNEG, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.ISHL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LSHL, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.ISHR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LSHR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IUSHR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LUSHR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IAND, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LAND, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IOR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LOR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IXOR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.LXOR, InsnCategory.ARITHMETIC);
        setCategory(Opcodes.IINC, InsnCategory.ARITHMETIC);

        // 类型转换
        setCategory(Opcodes.I2L, InsnCategory.CONVERSION);
        setCategory(Opcodes.I2F, InsnCategory.CONVERSION);
        setCategory(Opcodes.I2D, InsnCategory.CONVERSION);
        setCategory(Opcodes.L2I, InsnCategory.CONVERSION);
        setCategory(Opcodes.L2F, InsnCategory.CONVERSION);
        setCategory(Opcodes.L2D, InsnCategory.CONVERSION);
        setCategory(Opcodes.F2I, InsnCategory.CONVERSION);
        setCategory(Opcodes.F2L, InsnCategory.CONVERSION);
        setCategory(Opcodes.F2D, InsnCategory.CONVERSION);
        setCategory(Opcodes.D2I, InsnCategory.CONVERSION);
        setCategory(Opcodes.D2L, InsnCategory.CONVERSION);
        setCategory(Opcodes.D2F, InsnCategory.CONVERSION);
        setCategory(Opcodes.I2B, InsnCategory.CONVERSION);
        setCategory(Opcodes.I2C, InsnCategory.CONVERSION);
        setCategory(Opcodes.I2S, InsnCategory.CONVERSION);

        // 比较
        setCategory(Opcodes.LCMP, InsnCategory.COMPARISON);
        setCategory(Opcodes.FCMPL, InsnCategory.COMPARISON);
        setCategory(Opcodes.FCMPG, InsnCategory.COMPARISON);
        setCategory(Opcodes.DCMPL, InsnCategory.COMPARISON);
        setCategory(Opcodes.DCMPG, InsnCategory.COMPARISON);

        // 控制转移
        setCategory(Opcodes.IFEQ, InsnCategory.CONTROL);
        setCategory(Opcodes.IFNE, InsnCategory.CONTROL);
        setCategory(Opcodes.IFLT, InsnCategory.CONTROL);
        setCategory(Opcodes.IFGE, InsnCategory.CONTROL);
        setCategory(Opcodes.IFGT, InsnCategory.CONTROL);
        setCategory(Opcodes.IFLE, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPEQ, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPNE, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPLT, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPGE, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPGT, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ICMPLE, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ACMPEQ, InsnCategory.CONTROL);
        setCategory(Opcodes.IF_ACMPNE, InsnCategory.CONTROL);
        setCategory(Opcodes.GOTO, InsnCategory.CONTROL);
        setCategory(Opcodes.JSR, InsnCategory.CONTROL);
        setCategory(Opcodes.RET, InsnCategory.CONTROL);
        setCategory(Opcodes.TABLESWITCH, InsnCategory.CONTROL);
        setCategory(Opcodes.LOOKUPSWITCH, InsnCategory.CONTROL);
        setCategory(Opcodes.IFNULL, InsnCategory.CONTROL);
        setCategory(Opcodes.IFNONNULL, InsnCategory.CONTROL);

        // 方法调用
        setCategory(Opcodes.INVOKEVIRTUAL, InsnCategory.METHOD_CALL);
        setCategory(Opcodes.INVOKESPECIAL, InsnCategory.METHOD_CALL);
        setCategory(Opcodes.INVOKESTATIC, InsnCategory.METHOD_CALL);
        setCategory(Opcodes.INVOKEINTERFACE, InsnCategory.METHOD_CALL);
        setCategory(Opcodes.INVOKEDYNAMIC, InsnCategory.METHOD_CALL);

        // 方法返回
        setCategory(Opcodes.IRETURN, InsnCategory.METHOD_RETURN);
        setCategory(Opcodes.LRETURN, InsnCategory.METHOD_RETURN);
        setCategory(Opcodes.FRETURN, InsnCategory.METHOD_RETURN);
        setCategory(Opcodes.DRETURN, InsnCategory.METHOD_RETURN);
        setCategory(Opcodes.ARETURN, InsnCategory.METHOD_RETURN);
        setCategory(Opcodes.RETURN, InsnCategory.METHOD_RETURN);

        // 对象操作
        setCategory(Opcodes.NEW, InsnCategory.OBJECT);
        setCategory(Opcodes.CHECKCAST, InsnCategory.OBJECT);
        setCategory(Opcodes.INSTANCEOF, InsnCategory.OBJECT);
        setCategory(Opcodes.GETFIELD, InsnCategory.OBJECT);
        setCategory(Opcodes.PUTFIELD, InsnCategory.OBJECT);
        setCategory(Opcodes.GETSTATIC, InsnCategory.OBJECT);
        setCategory(Opcodes.PUTSTATIC, InsnCategory.OBJECT);

        // 数组操作
        setCategory(Opcodes.NEWARRAY, InsnCategory.ARRAY_OP);
        setCategory(Opcodes.ANEWARRAY, InsnCategory.ARRAY_OP);
        setCategory(Opcodes.ARRAYLENGTH, InsnCategory.ARRAY_OP);
        setCategory(Opcodes.MULTIANEWARRAY, InsnCategory.ARRAY_OP);

        // 异常处理
        setCategory(Opcodes.ATHROW, InsnCategory.EXCEPTION);

        // 同步
        setCategory(Opcodes.MONITORENTER, InsnCategory.SYNCHRONIZATION);
        setCategory(Opcodes.MONITOREXIT, InsnCategory.SYNCHRONIZATION);

        // 空操作
        setCategory(Opcodes.NOP, InsnCategory.NOP);

        // 其他（如 wide, breakpoint, impdep1, impdep2 等）
        setCategory(196, InsnCategory.OTHER); // WIDE
        setCategory(202, InsnCategory.OTHER); // BREAKPOINT
        setCategory(254, InsnCategory.OTHER); // IMPDEP1
        setCategory(255, InsnCategory.OTHER); // IMPDEP2
    }

    private static void setCategory(int opcode, InsnCategory category) {
        OPCODE_CATEGORY[opcode] = category;
    }

    // 基本大小统计
    // Jar文件本身大小（压缩后）
    private long jarFileSize;
    private long totalSize;
    private long classFilesSize;
    private long nonClassFilesSize;
    private int classFileCount;
    private int nonClassFileCount;

    // 类分类统计
    private int regularClassCount;
    private int interfaceCount;
    private int enumCount;
    private int recordCount;
    private int annotationCount;

    // 字节码详细统计
    private int totalMethods;
    private int lambdaMethods;
    private int totalFields;
    private int staticFinalFields;
    private int totalInstructions;
    private int maxLineNumber;
    private Map<InsnCategory, Integer> instructionsByCategory = new EnumMap<>(InsnCategory.class);

    // 其他信息
    private int totalEntries;
    private int failedClasses;

    // 用于汇总的临时计数器（每个class内部统计）
    private static class PerClassCounter {
        int methods = 0;
        int lambdaMethods = 0;
        int fields = 0;
        int staticFinalFields = 0;
        int instructions = 0;
        int maxLineNumber = 0;
        Map<InsnCategory, Integer> insnCategories = new EnumMap<>(InsnCategory.class);
        int classType = 0; // 0=普通类, 1=接口, 2=枚举, 3=record, 4=注解
    }

    /**
     * 分析指定jar文件
     */
    public static JarStatistics analyze(Path jarPath) throws IOException {
        JarStatistics stats = new JarStatistics();
        stats.jarFileSize = Files.size(jarPath); // 获取文件系统上的实际大小
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                stats.totalEntries++;

                if (entry.isDirectory()) continue;

                long size = entry.getSize();
                stats.totalSize += size;

                String name = entry.getName();
                if (name.endsWith(".class")) {
                    stats.classFilesSize += size;
                    stats.classFileCount++;

                    try (InputStream is = jar.getInputStream(entry)) {
                        PerClassCounter counter = stats.analyzeClass(is);
                        // 汇总到全局
                        stats.totalMethods += counter.methods;
                        stats.lambdaMethods += counter.lambdaMethods;
                        stats.totalFields += counter.fields;
                        stats.staticFinalFields += counter.staticFinalFields;
                        stats.totalInstructions += counter.instructions;
                        if (counter.maxLineNumber > stats.maxLineNumber) {
                            stats.maxLineNumber = counter.maxLineNumber;
                        }
                        // 合并指令分类计数
                        for (Map.Entry<InsnCategory, Integer> e : counter.insnCategories.entrySet()) {
                            stats.instructionsByCategory.merge(e.getKey(), e.getValue(), Integer::sum);
                        }

                        switch (counter.classType) {
                            case 1:
                                stats.interfaceCount++;
                                break;
                            case 2:
                                stats.enumCount++;
                                break;
                            case 3:
                                stats.recordCount++;
                                break;
                            case 4:
                                stats.annotationCount++;
                                break;
                            default:
                                stats.regularClassCount++;
                        }
                    } catch (Exception e) {
                        stats.failedClasses++;
                        System.err.println("解析class失败: " + name + " - " + e.getMessage());
                    }
                } else {
                    stats.nonClassFilesSize += size;
                    stats.nonClassFileCount++;
                }
            }
        }
        return stats;
    }

    /**
     * 使用ASM分析单个class文件的字节码
     */
    private PerClassCounter analyzeClass(InputStream in) throws IOException {
        PerClassCounter counter = new PerClassCounter();
        ClassReader reader = new ClassReader(in);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                // 确定类类型
                if ((access & Opcodes.ACC_ANNOTATION) != 0) {
                    counter.classType = 4; // 注解
                } else if ((access & Opcodes.ACC_ENUM) != 0) {
                    counter.classType = 2; // 枚举
                } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
                    counter.classType = 1; // 接口
                } else if (version >= Opcodes.V14 && (access & 0x10000) != 0) { // ACC_RECORD
                    counter.classType = 3; // record
                } else {
                    counter.classType = 0; // 普通类
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                counter.fields++;
                if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0) {
                    counter.staticFinalFields++;
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                counter.methods++;
                if (name.contains("lambda$") && ((access & Opcodes.ACC_SYNTHETIC) != 0)) {
                    counter.lambdaMethods++;
                }

                return new MethodVisitor(Opcodes.ASM9) {
                    private int currentMethodMaxLine = 0;

                    // 辅助方法：累加指令分类
                    private void countInstruction(int opcode) {
                        counter.instructions++;
                        InsnCategory cat = OPCODE_CATEGORY[opcode];
                        if (cat == null) cat = InsnCategory.OTHER;
                        counter.insnCategories.merge(cat, 1, Integer::sum);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                                boolean isInterface) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethod,
                                                       Object... bootstrapArguments) {
                        // INVOKEDYNAMIC 的操作码为 186，已在分类表中设置
                        countInstruction(Opcodes.INVOKEDYNAMIC);
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        countInstruction(opcode);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        countInstruction(Opcodes.LDC);
                    }

                    @Override
                    public void visitIincInsn(int var, int increment) {
                        countInstruction(Opcodes.IINC);
                    }

                    @Override
                    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                        countInstruction(Opcodes.TABLESWITCH);
                    }

                    @Override
                    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                        countInstruction(Opcodes.LOOKUPSWITCH);
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        countInstruction(Opcodes.MULTIANEWARRAY);
                    }

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        if (line > currentMethodMaxLine) currentMethodMaxLine = line;
                    }

                    @Override
                    public void visitEnd() {
                        if (currentMethodMaxLine > counter.maxLineNumber) {
                            counter.maxLineNumber = currentMethodMaxLine;
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return counter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== Jar 统计信息 ==========\n");
        sb.append(String.format("总大小: %d bytes\n", totalSize));
        sb.append(String.format("├─ .class文件大小: %d bytes (%d 个)\n", classFilesSize, classFileCount));
        sb.append(String.format("└─ 非class文件大小: %d bytes (%d 个)\n\n", nonClassFilesSize, nonClassFileCount));

        sb.append("类分类:\n");
        sb.append(String.format("├─ 普通类: %d\n", regularClassCount));
        sb.append(String.format("├─ 接口: %d\n", interfaceCount));
        sb.append(String.format("├─ 枚举: %d\n", enumCount));
        sb.append(String.format("├─ Record: %d\n", recordCount));
        sb.append(String.format("└─ 注解: %d\n\n", annotationCount));

        sb.append("字节码统计:\n");
        sb.append(String.format("├─ 方法总数: %d\n", totalMethods));
        sb.append(String.format("├─ 估计Lambda方法数: %d\n", lambdaMethods));
        sb.append(String.format("├─ 字段总数: %d\n", totalFields));
        sb.append(String.format("├─ 静态常量字段数: %d\n", staticFinalFields));
        sb.append(String.format("├─ 指令总数: %d\n", totalInstructions));
        sb.append(String.format("└─ 最大行号: %d\n\n", maxLineNumber));

        sb.append("指令分类详情:\n");
        for (InsnCategory cat : InsnCategory.values()) {
            int count = instructionsByCategory.getOrDefault(cat, 0);
            sb.append(String.format("  %s: %d\n", cat.getDisplayName(), count));
        }

        sb.append("\n其他:\n");
        sb.append(String.format("├─ Jar总条目数: %d\n", totalEntries));
        sb.append(String.format("└─ 解析失败的class: %d\n", failedClasses));

        return sb.toString();
    }

    /**
     * 将统计结果转换为 JsonElement（树模型）
     */
    public JsonObject toJsonObject() {
        JsonObject root = new JsonObject();

        // 基本大小统计
        root.addProperty("jarFileSize", jarFileSize);
        root.addProperty("totalSize", totalSize);
        root.addProperty("classFilesSize", classFilesSize);
        root.addProperty("nonClassFilesSize", nonClassFilesSize);
        root.addProperty("classFileCount", classFileCount);
        root.addProperty("nonClassFileCount", nonClassFileCount);

        // 类分类统计
        root.addProperty("regularClassCount", regularClassCount);
        root.addProperty("interfaceCount", interfaceCount);
        root.addProperty("enumCount", enumCount);
        root.addProperty("recordCount", recordCount);
        root.addProperty("annotationCount", annotationCount);

        // 字节码统计
        root.addProperty("totalMethods", totalMethods);
        root.addProperty("lambdaMethods", lambdaMethods);
        root.addProperty("totalFields", totalFields);
        root.addProperty("staticFinalFields", staticFinalFields);
        root.addProperty("totalInstructions", totalInstructions);
        root.addProperty("maxLineNumber", maxLineNumber);

        // 指令分类详情（枚举名 -> 数量）
        JsonObject catObj = new JsonObject();
        for (Map.Entry<InsnCategory, Integer> entry : instructionsByCategory.entrySet()) {
            catObj.addProperty(entry.getKey().name(), entry.getValue());
        }
        root.add("instructionsByCategory", catObj);

        // 其他
        root.addProperty("totalEntries", totalEntries);
        root.addProperty("failedClasses", failedClasses);

        return root;
    }
}
