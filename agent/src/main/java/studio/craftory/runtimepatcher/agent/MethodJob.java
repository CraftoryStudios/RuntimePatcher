package studio.craftory.runtimepatcher.agent;

import studio.craftory.runtimepatcher.annotation.CallParameters;
import studio.craftory.runtimepatcher.annotation.InjectionType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.Map;

public class MethodJob {

    private InjectionType type;
    private MethodNode patcherNode;
    private MethodNode resultNode;

    private String transformationTarget;
    private Class<?> transformationTargetClass;
    private String patcher;
    private String superClass;

    private Map<String, SpecialInvocation> specialInvocations;
    private Map<String, ClassNode> innerClasses;

    public MethodJob(InjectionType type, String transformationTarget, Class<?> transformationTargetClass, String patcher, String superClass, MethodNode patcherNode,
                     Map<String, SpecialInvocation> specialInvocations, Map<String, ClassNode> innerClasses) {
        this.type = type;
        this.patcherNode = patcherNode;
        this.transformationTarget = transformationTarget;
        this.transformationTargetClass = transformationTargetClass;
        this.patcher = patcher;
        this.superClass = superClass;
        this.specialInvocations = specialInvocations;
        this.innerClasses = innerClasses;

        patcherNode.name = patcherNode.name.endsWith("_INJECTED")
                ? patcherNode.name.substring(0, patcherNode.name.length() - 9)
                : patcherNode.name;

        patcherNode.name = patcherNode.name.equals("_init_") ? "<init>" : patcherNode.name;
    }

    public void process() {
        switch (type) {
            case OVERRIDE:
                override();
                break;
            case INSERT:
                insert();
                break;
            case APPEND:
                append();
                break;
        }

        transformInvocations();
    }

    private void transformInvocations() {
        boolean waitingForInit = false;
        String replacementClassName = null;
        for (Iterator<AbstractInsnNode> it = (Iterator<AbstractInsnNode>) resultNode.instructions.iterator(); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();

            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;

                if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL && waitingForInit && methodInsn.owner.startsWith(patcher)) {
                    methodInsn.owner = replacementClassName;
                    waitingForInit = false;
                }

                SpecialInvocation invocation = this.specialInvocations.get(methodInsn.name);

                if (invocation != null) {
                    CallParameters callParameters = invocation.getCallParameters();

                    methodInsn.setOpcode(callParameters.type().getOpcode());

                    methodInsn.name = callParameters.name().isEmpty() ? methodInsn.name : callParameters.name();
                    methodInsn.owner = callParameters.owner().isEmpty() ? methodInsn.owner : callParameters.owner();
                    methodInsn.desc = callParameters.desc().isEmpty() ? methodInsn.desc : callParameters.desc();
                    methodInsn.itf = callParameters.itf();
                }

                if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL && methodInsn.owner.equals(this.transformationTarget))
                    methodInsn.owner = this.superClass;
                else if (methodInsn.owner.equals(patcher))
                    methodInsn.owner = this.transformationTarget;

                if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL)
                    methodInsn.desc = methodInsn.desc.replace("L" + this.patcher + ";", "L" + this.transformationTarget + ";");

                if (methodInsn.name.endsWith("_INJECTED"))
                    methodInsn.name = methodInsn.name.substring(0, methodInsn.name.length() - 9);

            }

            if (insn.getOpcode() == Opcodes.NEW) {
                TypeInsnNode newInsn = (TypeInsnNode) insn;
                ClassNode innerClassNode = this.innerClasses.get(newInsn.desc);
                if (innerClassNode != null) {
                    Class<?> newClass = ClassFactory.generateAnonymousClassSubstitute(this.transformationTarget, innerClassNode, transformationTargetClass.getClassLoader());
                    newInsn.desc = replacementClassName = newClass.getName().replace('.', '/');
                    waitingForInit = true;
                }
            }

            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;

                if (fieldInsn.owner.equals(patcher))
                    fieldInsn.owner = this.transformationTarget;
            }

        }
    }

    private void append() {
        if (!this.resultNode.desc.endsWith("V"))
            throw new RuntimeException("Can't append to non-void method!");

        InsnList list = resultNode.instructions;

        AbstractInsnNode node = list.getLast();

        if (node instanceof LabelNode) {
            node = node.getPrevious();
        }

        if (!(node.getOpcode() == Opcodes.RETURN))
            throw new RuntimeException("Method " + this.resultNode.name + " in " + this.transformationTarget + " doesn't end with return opcode?!");

        list.remove(node);

        list.add(patcherNode.instructions);

        resultNode.instructions.add(patcherNode.instructions);
    }

    private void insert() {
        InsnList trInsns = patcherNode.instructions;

        AbstractInsnNode node = trInsns.getLast();

        while (true) {
            if (node == null)
                break;

            if (node instanceof LabelNode) {
                node = node.getPrevious();
                continue;
            } else if (node.getOpcode() == Opcodes.RETURN) {
                trInsns.remove(node);
            } else if (node.getOpcode() == Opcodes.ATHROW && node.getPrevious().getOpcode() == Opcodes.ACONST_NULL) {
                AbstractInsnNode prev = node.getPrevious();
                trInsns.remove(node);
                trInsns.remove(prev);
            }

            break;
        }

        resultNode.instructions.insert(trInsns);
    }

    private void override() {
        resultNode = patcherNode;
    }

    public MethodNode getResultNode() {
        return resultNode;
    }

    public void apply(ClassNode node) {
        for (int i = 0; i < node.methods.size(); i++) {
            if (!(patcherNode.name.equals(((MethodNode) node.methods.get(i)).name)
                    && patcherNode.desc.equals(((MethodNode) node.methods.get(i)).desc)))
                continue;

            resultNode = ((MethodNode) node.methods.get(i));

            process();

            node.methods.set(i, getResultNode());
            return;
        }

        throw new RuntimeException("Target method node not found! Patcher: " + patcher);
    }
}
