package studio.craftory.runtimepatcher.agent;

import java.util.regex.Pattern;
import studio.craftory.runtimepatcher.annotation.*;
import studio.craftory.runtimepatcher.util.MethodUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;


public class AgentJob {

    private final List<MethodJob> methodJobs;
    private Class<?> patcher;
    private Class<?> toTransform;
    private Class<?>[] interfaces;
    private static String VERSION;

    private Map<String, SpecialInvocation> specialInvocations = new HashMap<>();


    public AgentJob(Class<?> patcher, String version) {
        this.patcher = patcher;
        this.interfaces = patcher.getInterfaces();
        this.VERSION = version;

        this.readTransformationTarget(patcher);

        ClassNode patcherNode = new ClassNode(Opcodes.ASM9);
        ClassReader patcherReader;

        try {
            patcherReader = new ClassReader(patcher.getResource(patcher.getSimpleName() + ".class").openStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load class file of " + this.toTransform.getSimpleName(), e);
        }

        patcherReader.accept(patcherNode, 0);
        Map<String, ClassNode> innerClasses = readInnerClasses(patcherNode);

        Method[] methods = patcher.getDeclaredMethods();

        this.findSpecialInvocations(methods);

        this.methodJobs = new ArrayList<>(methods.length);

        Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Inject.class))
                .forEach(method -> {

                    InjectionType type = method.getAnnotation(Inject.class).value();

                    Optional<MethodNode> patcherMethodNode = ((List<MethodNode>) patcherNode.methods).stream()
                            .filter(node -> node != null && method.getName().equals(node.name) && MethodUtils.getSignature(method).equals(node.desc))
                            .findAny();

                    if (!patcherMethodNode.isPresent())
                        throw new RuntimeException("Patcher method node not found! (WTF?)");

                    this.methodJobs.add(new MethodJob(type, this.toTransform.getName().replace('.', '/'),
                            this.toTransform,
                            patcher.getName().replace('.', '/'),
                            this.toTransform.getSuperclass().getName().replace('.', '/'),
                            patcherMethodNode.get(), this.specialInvocations, innerClasses));

                });
    }

    private Map<String, ClassNode> readInnerClasses(ClassNode classNode) {
        Map<String, ClassNode> ret = new HashMap<>();
        ((List<InnerClassNode>) classNode.innerClasses).stream()
                .filter(node -> node.name.matches(".*\\$[0-9]+"))
                .filter(node -> node.innerName == null && node.outerName == null)
                .map(node -> {
                    ClassNode innerClassNode = new ClassNode(Opcodes.ASM9);

                    try (InputStream inputStream = this.patcher.getResourceAsStream(node.name.substring(node.name.lastIndexOf('/') + 1) + ".class")) {
                        ClassReader reader = new ClassReader(inputStream);

                        reader.accept(innerClassNode, 0);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return innerClassNode;
                }).forEach(node -> ret.put(node.name, node));
        return ret;
    }

    private void findSpecialInvocations(Method[] methods) {
        Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(CallParameters.class))
                .forEach(method -> this.specialInvocations.put(method.getName().replace('.', '/'),
                        new SpecialInvocation(method)));
    }

    private void readTransformationTarget(Class<?> patcher) {
        String targetClassName;
        if (patcher.isAnnotationPresent(Patch.class)) {
            targetClassName = patcher.getAnnotation(Patch.class).value().getName();
        } else if (patcher.isAnnotationPresent(PatchByName.class)) {
            targetClassName = patcher.getAnnotation(PatchByName.class).value();
        } else {
            throw new RuntimeException("No transformation annotation present on patcher: " + patcher.getName());
        }

        targetClassName = convertVersion(targetClassName);

        try {
            this.toTransform = Class.forName(targetClassName, true, patcher.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to transform class: ", e);
        }

    }

    private String convertVersion(String targetClassName) {
        targetClassName = targetClassName.replace("{version}", VERSION);

        String[] packages = targetClassName.split("\\.");
        return targetClassName.replace(packages[3], VERSION);
    }

    public List<MethodJob> getMethodJobs() {
        return methodJobs;
    }

    public Class<?> getToTransform() {
        return toTransform;
    }

    public Class<?>[] getInterfaces() {
        return interfaces;
    }

    public Class<?> getPatcher() {
        return patcher;
    }

    public void apply(ClassNode node) {
        for (MethodJob methodJob : methodJobs)
            methodJob.apply(node);
    }
}
