package net.officefloor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.sonatype.nexus.maven.staging.remote.RemoteNexus;

/**
 * Updates the <code>nexus-staging-maven-plugin</code> to be more reliable (by
 * attempting retries connecting to staging repository).
 * 
 * @author Daniel Sagenschneider
 */
public class UpdateNexusStagingMavenPluginTest {

	@Test
	public void updateNexusStagingMavenPlugin() throws Exception {

		// Load the properties
		Properties properties = new Properties();
		properties.load(new FileReader(new File(".", "target/repository.properties")));
		String localRepository = properties.getProperty("repository");
		String pluginVersion = properties.getProperty("version");

		// Obtain naming
		String artifactId = "nexus-staging-maven-plugin";
		String artifactFileName = artifactId + "-" + pluginVersion + ".jar";

		// Obtain the plugin jar
		Path pluginJarPath = Paths.get(localRepository, "org", "sonatype", "plugins", artifactId, pluginVersion,
				artifactFileName);
		assertTrue(Files.exists(pluginJarPath), "Can not find plugin JAR " + pluginJarPath.toAbsolutePath());

		// Obtain the class to enhance
		Class<?> remoteNexusClass = RemoteNexus.class;
		String remoteNexusResourceName = remoteNexusClass.getName().replace('.', '/') + ".class";
		InputStream remoteNexusContent = remoteNexusClass.getClassLoader().getResourceAsStream(remoteNexusResourceName);
		assertNotNull(remoteNexusContent, "Missing on class path " + remoteNexusResourceName);

		// Determine if back up jar exists
		Path backupJarPath = pluginJarPath.getParent().resolve(artifactId + "-" + pluginVersion + "_bak.jar");
		if (Files.exists(backupJarPath)) {
			Files.delete(backupJarPath);
		}

		// Move JAR to back up
		Files.move(pluginJarPath, backupJarPath);

		// Copy jar back, but replace classes for enhancement
		boolean isIncludeEnhancment = false;
		try (ZipFile backupJar = new ZipFile(backupJarPath.toFile())) {
			try (final ZipOutputStream pluginJar = new ZipOutputStream(new FileOutputStream(pluginJarPath.toFile()))) {
				byte[] buf = new byte[1024];
				for (Enumeration<? extends ZipEntry> backupJarEntries = backupJar.entries(); backupJarEntries
						.hasMoreElements();) {
					ZipEntry backupJarEntry = backupJarEntries.nextElement();
					if (backupJarEntry.getName().equalsIgnoreCase(remoteNexusResourceName)) {

						// Replace
						pluginJar.putNextEntry(new ZipEntry(remoteNexusResourceName));
						int len;
						while ((len = remoteNexusContent.read(buf)) > 0) {
							pluginJar.write(buf, 0, len);
						}
						isIncludeEnhancment = true;

					} else {
						// Copy in previous entry
						pluginJar.putNextEntry(backupJarEntry);
						InputStream entryContent = backupJar.getInputStream(backupJarEntry);
						int len;
						while ((len = entryContent.read(buf)) > 0) {
							pluginJar.write(buf, 0, len);
						}
					}
				}
			}
		}
		assertTrue(isIncludeEnhancment, "Target file to enhance not found in artifact " + artifactFileName);

		// Indicate enhanced
		System.out.println("Enhanced " + artifactFileName + " to be more reliable");
	}
}
