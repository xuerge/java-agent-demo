package com.eben.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.*;
import java.security.ProtectionDomain;

public class MyAgent {
    private static final String className = "com.eben.demo.MathGame";
    /**
     * 程序启动后，调用Agent
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        Class classNeedRedefine = null;
        for (Class cls : instrumentation.getAllLoadedClasses()) {
            if (cls.getName().equals(className)) {
                classNeedRedefine = cls;
            }
        }

        ClassDefinition def = new ClassDefinition(classNeedRedefine, MathGameRedefinition.getBytes());
        try {
            instrumentation.redefineClasses(new ClassDefinition[]{def});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 程序启动过程中，调用Agent
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new MyTransformer(), true);
    }

    static class MathGameRedefinition {
        public static byte[] getBytes() {
            try {
                final ClassPool classPool = ClassPool.getDefault();
                final CtClass clazz = classPool.get(className);
                CtMethod convertToAbbr = clazz.getDeclaredMethod("run");
                convertToAbbr.insertBefore("{ System.out.println(\"enter run\"); }");

                // 返回字节码，并且detachCtClass对象
                return clazz.toBytecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class MyTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            String mathGame = "com/eben/demo/MathGame";
            if (mathGame.equals(className)) {
                return MathGameRedefinition.getBytes();
            }
            // 如果返回null则字节码不会被修改
            return null;
        }
    }
}
