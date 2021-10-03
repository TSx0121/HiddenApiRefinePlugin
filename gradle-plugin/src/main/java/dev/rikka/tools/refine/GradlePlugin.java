package dev.rikka.tools.refine;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@Nonnull Project target) {
        target.afterEvaluate((project) -> {
            project.getTasks().withType(JavaCompile.class, (task) -> {
                project.getDependencies().add(
                        "compileOnly",
                        "dev.rikka.tools.refine:compiler-plugin:" + GradlePlugin.class.getPackage().getImplementationVersion()
                );

                final CompileOptions options = task.getOptions();

                options.setFork(true);

                List<String> args = options.getForkOptions().getJvmArgs();
                if (args == null) {
                    args = new ArrayList<>();
                }
                args.addAll(List.of(
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
                ));
                options.getForkOptions().setJvmArgs(args);

                final String generatedClassPath = Paths.get(project.getBuildDir().getAbsolutePath(), "generated", "hidden-api-refine")
                        .toFile().getAbsolutePath();

                List<String> compilerArgs = options.getCompilerArgs();
                if (compilerArgs == null) {
                    compilerArgs = new ArrayList<>();
                }
                compilerArgs.addAll(List.of(
                        "-Xplugin:HiddenApiRefine " + URLEncoder.encode(generatedClassPath, Charset.defaultCharset())
                ));
            });
        });
    }
}
