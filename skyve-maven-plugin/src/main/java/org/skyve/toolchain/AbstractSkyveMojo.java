package org.skyve.toolchain;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

abstract class AbstractSkyveMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    protected Prompter prompter;

    protected void configureClasspath() throws DependencyResolutionRequiredException, MalformedURLException {
    	configureClasspath(null);
    }
    
	protected void configureClasspath(String srcDir) throws DependencyResolutionRequiredException, MalformedURLException {
        final Collection<String> elements = project.getTestClasspathElements();
        final List<URL> listUrl = new ArrayList<>(elements.size() + 1);
        if (srcDir != null) {
        	listUrl.add(new File(srcDir).toURI().toURL());
        }
        for (String artifact : elements) {
            final URL url = new File(artifact).toURI().toURL();
            listUrl.add(url);
        }

        @SuppressWarnings("resource")
        final URLClassLoader newClassLoader = new URLClassLoader(listUrl.toArray(new URL[listUrl.size()]), Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(newClassLoader);
    }

    protected Path getSkyveDirectory(String directoryName) throws FileNotFoundException {
        for (final String sourceRoot : project.getCompileSourceRoots()) {
            final Path modules = Paths.get(sourceRoot, directoryName);
            if (modules.toFile().exists()) {
                return modules;
            }
        }

        throw new FileNotFoundException(String.format("Failed to find %s directory.", directoryName));
    }

    protected Path getModulesDirectory() throws FileNotFoundException {
        return getSkyveDirectory("modules");
    }

    protected Path getCustomersDirectory() throws FileNotFoundException {
        return getSkyveDirectory("customers");
    }

    protected static List<File> getCustomerDirectories(Path customersDirectory) {
        return Arrays.stream(customersDirectory.toFile().listFiles())
                .filter(File::isDirectory)
                .collect(Collectors.toList());
    }

    protected String getDefaultOrPromptCustomer(String defaultCustomer) throws FileNotFoundException, PrompterException {
        if (StringUtils.isNotBlank(defaultCustomer)) {
            return defaultCustomer;
        }

        final Path customersDirectory = getCustomersDirectory();
        final List<File> customerDirectories = getCustomerDirectories(customersDirectory);

        final File customerDirectory;
        if (customerDirectories.size() == 1) {
            customerDirectory = customerDirectories.get(0);
        } else if (customerDirectories.size() > 1) {
            final String selectedCustomer = prompter.prompt(String.format("Please enter a customer name (customers available: %s)",
                    customerDirectories.stream().map(File::getName).collect(Collectors.joining(", "))));
            customerDirectory = customerDirectories.stream()
                    .filter(c -> c.getName().equals(selectedCustomer))
                    .findFirst()
                    .orElse(null);

            if (customerDirectory == null) {
                throw new IllegalArgumentException(String.format("Customer %s does not exist.", selectedCustomer));
            }
        } else {
            return prompter.prompt("Please enter a customer name");
        }

        return customerDirectory.getName();
    }

    protected String getDefaultOrPrompt(String defaultValue, String promptMessage) throws PrompterException {
        if (StringUtils.isNotBlank(defaultValue)) {
            return defaultValue;
        }

        return prompter.prompt(promptMessage);
    }
}
