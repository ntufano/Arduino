package processing.app.packages;

import java.io.File;

public interface Packager {

  /**
   * Starts the packager engine
   * 
   * @param folder
   *          the folder used by the packager as internal storage
   * @throws PackagerException
   */
  public void initialize(File folder) throws PackagerException;

  /**
   * Retrieve the specified library artifacts with all dependencies.
   * 
   * @param author
   * @param library
   * @param version
   * @throws PackagerException
   */
  public void retrieve(String author, String library, String version)
      throws PackagerException;

  public void publish(Library library, File artifactFile)
      throws PackagerException;
}
