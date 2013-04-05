package processing.app.packager;

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

public class Packager {

  private Ivy ivy;
  private File folder;
  private File libFolder;
  private PackageMantainer mantainer;

  public Packager(File _folder) throws ParseException, IOException {
    folder = _folder;
    libFolder = new File(_folder, "libraries");

    IvySettings settings = new IvySettings();
    settings.setDefaultIvyUserDir(folder);
    settings.load(new File(folder, "ivy-settings.xml"));
    ivy = Ivy.newInstance(settings);

    out.println("Base Dir     : " + settings.getBaseDir());
    out.println("Ivy User Dir : " +
        settings.getDefaultIvyUserDir().getAbsolutePath());
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

  public void retrieve(String org, String module, String rev)
      throws IOException, ParseException {
    File xml = File.createTempFile("arduino", ".xml");
    try {
      PrintStream xmlOut = new PrintStream(new FileOutputStream(xml));
      xmlOut.println("<ivy-module version=\"2.0\">");
      xmlOut.println("  <info organisation=\"me\" module=\"mod\" />");
      xmlOut.println("  <dependencies>");
      xmlOut.println("    <dependency org=\"" + org + "\" name=\"" + module +
          "\" rev=\"" + rev + "\">");
      xmlOut.println("      <artifact name=\"" + module + "\" type=\"zip\" />");
      xmlOut.println("    </dependency>");
      xmlOut.println("  </dependencies>");
      xmlOut.println("</ivy-module>");
      xmlOut.close();

      ivyRetrieve(xml);
    } finally {
      xml.delete();
    }
  }

  public void ivyRetrieve(File xml) throws IOException, ParseException {
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

  public static void main(String args[]) throws ParseException, IOException,
      PackagerException {
    PackageMantainer mant = new PackageMantainer("arduino", "abc",
        "abc@arduino.cc");
    Packager packager = new Packager(new File("app/test-ivy"));
    packager.setLogLevel(100);
    packager.setMantainer(mant);
    // out.println("Num. of modules ids: " + packager.load());

    // packager.resolve(new File("app/test-ivy/libs/Servo/ivy.xml"), "1.0");
    // packager.publish(new File("app/test-ivy/libs/Servo"), "Servo", "1.0");

    packager.retrieve("arduino", "Servo", "1.0");
  }

}
