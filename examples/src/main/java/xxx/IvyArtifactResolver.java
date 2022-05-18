package xxx;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.URLResolver;

import java.io.File;

public class IvyArtifactResolver {
  public File resolveArtifact(String groupId, String artifactId, String version) throws Exception {
    //creates clear ivy settings
    IvySettings ivySettings = new IvySettings();

//    URLResolver resolver0 = new URLResolver();
//    resolver0.setM2compatible(true);
//    resolver0.setName("local");
//    resolver0.addArtifactPattern(
//        "file://shared/.ivy2/local/"
//            + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");

    URLResolver resolver1 = new URLResolver();
    resolver1.setM2compatible(true);
    resolver1.setName("jitpack");
    resolver1.addArtifactPattern(
        "https://jitpack.io/"
            + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");

    URLResolver resolver2 = new URLResolver();
    resolver2.setM2compatible(true);
    resolver2.setName("central");
    resolver2.addArtifactPattern(
        "https://repo1.maven.org/maven2/"
            + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");

    ChainResolver resolver = new ChainResolver();
    resolver.setName("chain");
//    resolver.add(resolver0);
    resolver.add(resolver1);
    resolver.add(resolver2);
    ivySettings.addResolver(resolver);


    //set to the default resolver
    ivySettings.setDefaultResolver(resolver.getName());
    //creates an Ivy instance with settings
    Ivy ivy = Ivy.newInstance(ivySettings);

    File ivyfile = File.createTempFile("ivy", ".xml");
    ivyfile.deleteOnExit();

    String[] dep = null;
    dep = new String[]{groupId, artifactId, version};

    DefaultModuleDescriptor md =
        DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0],
            dep[1] + "-caller", "working"));

    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
        ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
    md.addDependency(dd);

    //creates an ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyfile);

    String[] confs = new String[]{"default"};
    ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);

    //init resolve report
    ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);

    if (report.hasError()) {
      report.getAllProblemMessages().forEach(s -> System.out.println("XXX Report error: " + s));
    }

    //so you can get the jar library
    return report.getAllArtifactsReports()[0].getLocalFile();
  }

  public static void main(String args[]) {
    try {
      File jar1 = new IvyArtifactResolver().resolveArtifact(
          "com.github.tmtsoftware.esw",
          "esw-ocs-dsl-kt_2.13",
          "b964c761e4f998d64306dd70298da577558a42f2");
      System.out.println("XXX Passed: Got jar " + jar1);
      File jar2 = new IvyArtifactResolver().resolveArtifact(
          "com.github.tmtsoftware.esw",
          "esw-ocs-script-kt_2.13",
          "b964c761e4f998d64306dd70298da577558a42f2");
      System.out.println("XXX Passed: Got jar " + jar2);
    } catch (Exception ex) {
      System.out.println("XXX Failed: ");
      ex.printStackTrace();
    }
  }
}

