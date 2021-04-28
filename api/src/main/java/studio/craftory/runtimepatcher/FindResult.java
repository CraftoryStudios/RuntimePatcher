package studio.craftory.runtimepatcher;

import java.io.File;

/**
 * All credit for this class goes to https://github.com/avaje-common/avaje-agentloader
 * Licensed under The Apache Software License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

/**
 * Holds the result of a jar-resolve. The containing file may either be a temp
 * file or a real file (mostly in .m2 directory)
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class FindResult implements AutoCloseable {

  private final File file;
  private final boolean temp;

  public FindResult(File file, boolean temp) {
    super();
    this.file = file;
    this.temp = temp;
  }

  public File getFile() {
    return file;
  }

  public boolean isTemp() {
    return temp;
  }

  @Override
  public void close() {
    if (isTemp()) {
      try {
        file.delete();
      } catch (Exception ex) {
        file.deleteOnExit();
      }
    }
  }

}
