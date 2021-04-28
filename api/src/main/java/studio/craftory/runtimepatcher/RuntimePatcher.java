package studio.craftory.runtimePatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.bukkit.Bukkit;
import studio.craftory.runtimepatcher.agent.Agent;

public class RuntimePatcher {

    private static final String ATTACH_MOD_PATH = "jmods/jdk.attach.jmod";
    private static String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    private static String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "").replace(".", "");

    private static boolean canSelfAttach() {
        String version = System.getProperty("java.version");
        return version.startsWith("1.");
    }

    private static final String getPid() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        return vmName.substring(0, vmName.indexOf('@'));
    }

    public RuntimePatcher(Class<?>... patchers) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        File javaHome = new File(System.getProperty("java.home"));
        if (systemClassLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

            try {
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);

                File toolsJar = new File(javaHome, "lib/tools.jar");
                if (!toolsJar.exists())
                    throw new RuntimeException("Not running with JDK!");

                method.invoke(urlClassLoader, toolsJar.toURI().toURL());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else {
            Path attachMod = javaHome.toPath().resolve(ATTACH_MOD_PATH);
            if (Files.notExists(attachMod)) {
                throw new RuntimeException("Not running with JDK!");
            }
        }

        String agentFilePath = getAgentJar();
        if (canSelfAttach()) {
            try {
                new DirectAttacher().attachAgent(agentFilePath, getPid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                new RemoteAttacher().attachAgent(agentFilePath, getPid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Agent.getInstance().process(VERSION, patchers);
        System.out.println("Loaded");
    }

    private static String getAgentJar() {
        try (InputStream is = RuntimePatcher.class.getResourceAsStream("/agent.jar")) {
            File agentFile = File.createTempFile("agent", ".jar");
            agentFile.deleteOnExit();

            Files.copy(is, agentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return agentFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
