package processing.app.packager;

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.repository.RepositoryManagementEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.MessageLoggerEngine;

public class Packager {

  private Ivy ivy;
  private File folder;
  private PackageMantainer mantainer;

  public Packager(File _folder) throws ParseException, IOException {
    folder = _folder;

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

  public void load() {
    RepositoryManagementEngine engine = ivy.getRepositoryEngine();
    engine.load();
    int n = engine.getModuleIdsNumber();
    out.println("Num. of modules ids: " + n);
  }

  public void resolve(File xml, String revision) throws ParseException,
      IOException {
    ResolveOptions opt = new ResolveOptions();
    opt.setRevision(revision);
    try {
      URL url = xml.toURI().toURL();
      ivy.resolve(url, opt);
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

  public void retrieve(File baseDir, String owner, String module,
                       String revision) throws IOException, ParseException {
    ModuleId mid = new ModuleId(owner, module);
    ModuleRevisionId mrid = new ModuleRevisionId(mid, revision);

    // ResolveOptions opts = new ResolveOptions();
    // ivy.resolve(mrid, opts, false);

    RetrieveOptions options = new RetrieveOptions();
    options.setDestArtifactPattern(baseDir.getAbsolutePath() +
        "/[artifact].[ext]");
    options.setDestIvyPattern(baseDir.getAbsolutePath() +
        "/ivy-[artifact].[ext]");
    ivy.retrieve(mrid, options);
  }

  public static void main(String args[]) throws ParseException, IOException,
      PackagerException {
    PackageMantainer mant = new PackageMantainer("arduino", "", "");
    Packager packager = new Packager(new File("app/test-ivy"));
    packager.setLogLevel(10);
    packager.setMantainer(mant);
    packager.load();

    // packager.resolve(new File("app/test-ivy/libs/Servo/ivy.xml"), "1.0");
    // packager.publish(new File("app/test-ivy/libs/Servo"), "Servo", "1.0");

    packager.resolve(new File("app/test-ivy/libs/ivy-xxx-1.0.xml"), "1.0");
    packager.retrieve(new File("app/test-ivy/libraries"), "arduino", "Servo",
                      "1.0");
  }

}
