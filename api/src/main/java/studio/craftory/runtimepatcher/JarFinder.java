package studio.craftory.runtimepatcher;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * All credit for this class goes to https://github.com/avaje-common/avaje-agentloader
 * Licensed under The Apache Software License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

/**
 * Searches a jar file by a class name, that is contained in the jar. (e.g. the
 * main class of an agent)
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class JarFinder {

  /**
   * Buffer size used when extracting the agent (when it is embedded).
   */
  private static final int IO_BUFFER_SIZE = 1024 * 4;

  /**
   * Tries to find the jar, that matches the query.
   */
  public FindResult find(String query, ClassLoader cl) {
    List<URL> candidates = findCandidates(query, cl);
    if (candidates.isEmpty()) {
      return null;
    } else if (candidates.size() > 1) {
    }
    try {
      URL url = candidates.get(0);
      // We have found the agent jar in the classpath
      FindResult jar = null;
      if (isJarInJar(url)) {
        // extract the agent jar into a tmp directory for use
        jar = extractJar(url, query);
      } else {
        jar = new FindResult(new File(url.toURI()), false);
      }
      return jar;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return true if the agent jar is embedded. In this case we extract it out into
   * a tmp directory.
   */
  protected boolean isJarInJar(URL url) {
    return url.getProtocol().equals("jar") && url.getPath().contains("!/");
  }

  /**
   * This method will extract agent JAR file from URL path. Due to the package
   * implementation, this method will cover two cases as below: Embedded Class
   * Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file/ Embedded Jar
   * Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file!/
   *
   * @param path      is full url entry in the classpath
   * @param agentName is the agent name that we are trying to match
   * @return null if it fails or a full path to the jar file if it succeeds
   */
  protected FindResult extractJar(URL path, String agentName) {
    File fullPath = null;

    try {
      fullPath = File.createTempFile(agentName, ".jar");
      try (InputStream is = path.openStream();
          OutputStream os = new BufferedOutputStream(new FileOutputStream(fullPath))) {
        copyBytes(is, os);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return fullPath == null ? null : new FindResult(fullPath, true);
  }

  /**
   * Copy the bytes from input to output streams (using a 4K buffer).
   */
  protected long copyBytes(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[IO_BUFFER_SIZE];

    long count = 0;
    int n;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }


  private URL stripUrl(URL url, String classFile) throws MalformedURLException {
    if (url == null) {
      return null;
    }
    String tmp = url.toString();
    if (!tmp.endsWith(classFile)) {
      return null;
    }
    tmp = tmp.substring(0, tmp.length() - classFile.length()); // remove

    if (tmp.startsWith("jar:")) {
      if (tmp.endsWith("!/")) { // cut away the last "!/"
        tmp = tmp.substring(0, tmp.length() - 2);
      }
      if (tmp.contains("!/")) {
        // path contains additional !/ -> this means jar in jar
      } else if (tmp.startsWith("jar:file:")) {
        tmp = tmp.substring(4); // else this means resource in jar
      }
    }
    return new URL(url, tmp, null);
  }

  protected List<URL> findCandidates(final String query, ClassLoader cl) {
    String classFile = query.replace('.', '/') + ".class";
    List<URL> candidates = new ArrayList<>();
    try {
      // we use only the first candidate here (className should be unique)
      URL url = cl.getResource(classFile);
      url = stripUrl(url, classFile);
      if (url != null) {
        candidates.add(url);
      } else {
        try {
          // we did not find the jar, so try to resolve via codeSource
          Class<?> clazz = cl.loadClass(query);
          ProtectionDomain pd = clazz.getProtectionDomain();
          CodeSource cs = pd == null ? null : pd.getCodeSource();
          if (cs != null) {
            candidates.add(cs.getLocation());
          }
        } catch (ClassNotFoundException cnf) {
          // NOP
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return candidates;
  }
}
