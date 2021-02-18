# 如何使用JavaAgent？
JavaAgent是借助JVM TI实现，在Java程序启动过程中，以及启动以后，获取到 Instrument API，并通过Instrument重载正在被ClassLoader加载或者已经被ClassLoader加载的字节码的能力。通过JavaAgent可以对字节码进行修改插桩。
常见的应用有利用Agent实现诊断工具(Arthas),热部署(Spring-load)，代码覆盖率工具(JaCoCo)等。

## 编写一个Agent示例：
示例程序说明：
- demo程序：实现一个小程序，每隔一秒生成一个随机数，再执行质因数分解，并打印出分解结果。（没错，就是借鉴arthas教程中的小程序）
- agent程序： 实现一个Agent，分别实现 `premain`，和 `agentmain` 方法，利用`Javassist` 插桩，打印到控制台。

### 编写Demo程序
就是一个普通的Java小程序，不再详细描述实现，可以直接看源码

### 编写agent程序
下面演示实现Agent主要步骤与关键代码，具体代码参考

1. 实现 MyAgent
```java
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
```

2. 打包 MyAgent
- 要使Agent运行起来，需要配置`MANIFEST.MF`文件
- 在`MANIFEST.MF`文件中配置 Premain-Class
- 在`MANIFEST.MF`文件中配置 Agent-Class
- 通常也需要配置 Can-Redefine-Classes 与 Can-Retransform-Classes
  为了让我们的 Agent.jar 包中含有符合要求的`MANIFEST.MF`以及依赖的jar包，我们使用maven-assembly-plugin插件

```xml
<plugin>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <finalName>agent</finalName>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <archive>
            <index>true</index>
            <manifest>
                <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
            </manifest>
            <manifestEntries>
                <Premain-Class>com.eben.agent.PreMainTraceAgent</Premain-Class>
                <Agent-Class>com.eben.agent.PreMainTraceAgent</Agent-Class>
                <Can-Redefine-Classes>true</Can-Redefine-Classes>-->
                <Can-Retransform-Classes>true</Can-Retransform-Classes>
            </manifestEntries>
        </archive>
        <outputDirectory>
            ${project.parent.basedir}/bin
        </outputDirectory>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id> <!-- this is used for inheritance merges -->
            <phase>package</phase> <!-- bind to the packaging phase -->
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 运行 agent
**在 Demo 程序启动过程中加载Agent**
编写脚本`startDemoWithAgent.sh`启动
```bash
#!/usr/bin/env sh
java -javaagent:agent-jar-with-dependencies.jar -jar demo.jar | tee output.log
```

**在 Demo 程序启动后再加载Agent**
1. 启动 Demo 程序

```bash
#!/usr/bin/env sh
java -jar demo.jar | tee output.log
```

2. 启动 Agent 程序

```bash
#!/usr/bin/env sh
java -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar  -jar agent-boot.jar agent-jar-with-dependencies.jar <pid>


```
