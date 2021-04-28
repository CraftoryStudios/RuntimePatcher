package studio.craftory.runtimepatcher;

import com.sun.tools.attach.VirtualMachine;

public class DirectAttacher {

  /**
   * @param args 0: agent jar path, 1: PID
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    new DirectAttacher().attachAgent(args[0], args[1]);
    System.exit(42);
  }

  public void attachAgent(String agentFilePath, String pid) throws Exception {
    VirtualMachine vm = VirtualMachine.attach(pid);
    vm.loadAgent(agentFilePath);
    vm.detach();
  }
}
