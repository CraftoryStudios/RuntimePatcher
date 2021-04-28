package studio.craftory.runtimepatcher;

import java.io.File;
import java.io.InputStream;

public class RemoteAttacher {

  public static final boolean isWindows() {
    return File.separatorChar == '\\';
  }

  public void attachAgent(String agentFilePath, String pid) throws Exception {
    JarFinder finder = new JarFinder();
    try (FindResult result = finder.find(RuntimePatcher.class.getName(), RuntimePatcher.class.getClassLoader())) {
      File classPath;
      if (result != null) {
        classPath = result.getFile();
      } else {
        return;
      }

      StringBuilder cmd = new StringBuilder();
      cmd.append(System.getProperty("java.home")).append(File.separatorChar).append("bin").append(File.separatorChar);
      if (isWindows()) {
        cmd.append("java.exe");
      } else {
        cmd.append("java");
      }

      ProcessBuilder procBuilder = new ProcessBuilder(cmd.toString(), // path/to/java
          "-cp", classPath.getAbsolutePath(), // -cp <this-jar>
          DirectAttacher.class.getName(), // <class-with-main>
          agentFilePath, pid); // >params for main>

      procBuilder.redirectErrorStream(true);

      Process proc = procBuilder.start();

      try (InputStream is = proc.getInputStream()) {
        if (proc.waitFor() == 42) {
          System.out.println("External attach was successful");
        } else {
          System.out.println("failed");
        }
      }
    }
  }
}
