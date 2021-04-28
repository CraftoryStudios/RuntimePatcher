package studio.craftory.runtimepatcher.agent;

import studio.craftory.runtimepatcher.transform.ClassPatcher;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

public class Agent {

    private static Agent instance;

    private Instrumentation instrumentation;

    private Agent(Instrumentation inst){
        this.instrumentation = inst;
    }

    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        instance = new Agent(instrumentation);
    }

    public static Agent getInstance() {
        return instance;
    }

    public void process(String version, Class<?>... patcherClasses) {
        List<AgentJob> agentJobs = new ArrayList<>();

        for (Class<?> clazz : patcherClasses)
            agentJobs.add(new AgentJob(clazz, version));

        ClassPatcher classPatcher = new ClassPatcher(agentJobs);
        instrumentation.addTransformer(classPatcher, true);

        try {
            instrumentation.retransformClasses(classPatcher.getClassesToTransform());
        } catch (UnmodifiableClassException e) {
            e.printStackTrace();
        }
    }

}
