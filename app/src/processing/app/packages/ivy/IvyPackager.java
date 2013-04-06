package processing.app.packages.ivy;

import static java.lang.System.out;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.repository.RepositoryManagementEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.MessageLoggerEngine;

import processing.app.packages.Library;
import processing.app.packages.PackageMantainer;
import processing.app.packages.Packager;
import processing.app.packages.PackagerException;

public class IvyPackager implements Packager {

  private Ivy ivy;
  private File folder;
  private File libFolder;
  private PackageMantainer mantainer;

  @Override
  public void initialize(File _folder) throws PackagerException {
    folder = _folder;
    libFolder = new File(_folder, "libraries");

    IvySettings settings = new IvySettings();
    settings.setDefaultIvyUserDir(folder);

    // settings.load(new File(folder, "ivy-settings.xml"));
    URL url = IvyPackager.class.getResource("ivy-settings.xml");
    try {
      settings.load(url);
    } catch (ParseException e) {
      throw new PackagerException(e);
    } catch (IOException e) {
      throw new PackagerException(e);
    }
    ivy = Ivy.newInstance(settings);

    out.println("Base Dir     : " + settings.getBaseDir());
    out.println("Ivy User Dir : " +
        settings.getDefaultIvyUserDir().getAbsolutePath());

    setLogLevel(10);
  }

  public void setLogLevel(int level) {
    MessageLoggerEngine loggerEngine = ivy.getLoggerEngine();
    loggerEngine.setDefaultLogger(new DefaultMessageLogger(level));
  }

  public int load() {
    RepositoryManagementEngine engine = ivy.getRepositoryEngine();
    engine.load();
    return engine.getModuleIdsNumber();
  }

  public ResolveReport resolve(File xml, String revision)
      throws ParseException, IOException {
    ResolveOptions opt = new ResolveOptions();
    opt.setRevision(revision);
    try {
      URL url = xml.toURI().toURL();
      return ivy.resolve(url, opt);
    } catch (MalformedURLException e) {
      // Should never happen, but...
      throw new IOException("Malformed URL.", e);
    }
  }

  public void publish(File baseDir, String module, String revision)
      throws IOException, PackagerException {
    if (mantainer == null)
      throw new PackagerException("Mantainer not set.");

    List<String> srcArtifactPattern = new ArrayList<String>();
    srcArtifactPattern.add(baseDir.getAbsolutePath() + "/[artifact].[ext]");

    String resolver = "public";
    // String resolver = "arduino";

    ModuleId mid = new ModuleId(mantainer.getUsername(), module);
    ModuleRevisionId mrid = new ModuleRevisionId(mid, revision);

    PublishOptions opts = new PublishOptions();
    opts.setPubrevision(revision);
    opts.setPubdate(new Date());
    opts.setOverwrite(true);
    opts.setValidate(true);
    opts.setSrcIvyPattern(baseDir.getAbsolutePath() + "/ivy.xml");
    ivy.publish(mrid, srcArtifactPattern, resolver, opts);
  }

  public PackageMantainer getMantainer() {
    return mantainer;
  }

  public void setMantainer(PackageMantainer _mantainer) {
    mantainer = _mantainer;
  }

  @Override
  public void retrieve(String author, String library, String version)
      throws PackagerException {
    File xml = null;
    try {
      xml = File.createTempFile("arduino", ".xml");
      PrintStream xmlOut = new PrintStream(new FileOutputStream(xml));
      xmlOut.println("<ivy-module version=\"2.0\">");
      xmlOut.println("  <info organisation=\"me\" module=\"mod\" />");
      xmlOut.println("  <dependencies>");
      xmlOut.println("    <dependency org=\"" + author + "\" name=\"" +
          library + "\" rev=\"" + version + "\">");
      xmlOut
          .println("      <artifact name=\"" + library + "\" type=\"zip\" />");
      xmlOut.println("    </dependency>");
      xmlOut.println("  </dependencies>");
      xmlOut.println("</ivy-module>");
      xmlOut.close();

      ivyRetrieve(xml);
    } catch (IOException e) {
      throw new PackagerException(e);
    } catch (ParseException e) {
      throw new PackagerException(e);
    } finally {
      if (xml != null)
        xml.delete();
    }
  }

  private void ivyRetrieve(File xml) throws IOException, ParseException {
    URL url;
    try {
      url = xml.toURI().toURL();
    } catch (MalformedURLException e) {
      // Should never happen, but...
      throw new IOException("Malformed URL.", e);
    }
    ResolveOptions opt = new ResolveOptions();
    ResolveReport report = ivy.resolve(url, opt);

    RetrieveOptions options = new RetrieveOptions();
    String destFilePattern = libFolder.getAbsolutePath() +
        "/[organization]-[artifact]-[revision].[ext]";
    ModuleDescriptor md = report.getModuleDescriptor();
    ModuleRevisionId mrid = md.getModuleRevisionId();
    ivy.retrieve(mrid, destFilePattern, options);
  }

  @Override
  public void publish(Library library, File artifactFile)
      throws PackagerException {
    File xml = null;
    try {
      String author = library.getAuthor();
      String name = library.getName();
      String version = library.getVersion();

      xml = File.createTempFile("arduino", ".xml");
      PrintStream xmlOut = new PrintStream(new FileOutputStream(xml));
      xmlOut.println("<ivy-module version=\"2.0\">");
      xmlOut.println("  <info");
      xmlOut.println("     organisation=\"" + author + "\"");
      xmlOut.println("     module=\"" + name + "\"");
      xmlOut.println("     revision=\"" + version + "\"");
      xmlOut.println("     status=\"release\" />");
      // xmlOut.println("  <dependencies>");
      // xmlOut.println("    <dependency org=\"" + author + "\" name=\"" +
      // library + "\" rev=\"" + version + "\">");
      // xmlOut
      // .println("      <artifact name=\"" + library + "\" type=\"zip\" />");
      // xmlOut.println("    </dependency>");
      // xmlOut.println("  </dependencies>");
      xmlOut.println("  <publications>");
      xmlOut.println("    <artifact name=\"" + name + "\" type=\"zip\" />");
      xmlOut.println("  </publications>");
      xmlOut.println("</ivy-module>");
      xmlOut.close();

      List<String> srcArtifactPattern = new ArrayList<String>();
      String artifactFolder = artifactFile.getParentFile().getAbsolutePath();
      srcArtifactPattern.add(artifactFolder + "/[artifact].[ext]");

      ModuleId mid = new ModuleId(author, name);
      ModuleRevisionId mrid = new ModuleRevisionId(mid, version);

      PublishOptions opts = new PublishOptions();
      opts.setPubrevision(version);
      opts.setPubdate(new Date());
      opts.setOverwrite(true);
      opts.setValidate(true);
      opts.setSrcIvyPattern(xml.getAbsolutePath());
      ivy.publish(mrid, srcArtifactPattern, "public", opts);
    } catch (IOException e) {
      throw new PackagerException(e);
    } finally {
      if (xml != null)
        xml.delete();
    }
  }

  public static void main(String args[]) throws ParseException, IOException,
      PackagerException {
    PackageMantainer mant = new PackageMantainer("arduino", "abc",
        "abc@arduino.cc");
    IvyPackager packager = new IvyPackager();
    packager.initialize(new File("app/test-ivy"));
    packager.setMantainer(mant);
    // out.println("Num. of modules ids: " + packager.load());

    packager.resolve(new File("app/test-ivy/libs/Servo/ivy.xml"), "1.0");
    packager.publish(new File("app/test-ivy/libs/Servo"), "Servo", "1.0");

    packager.retrieve("arduino", "Servo", "1.0");
  }

}
