package org.cbioportal.staging.services.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerUtils {
    public static ProcessBuilder dockerComposeProcessBuilder(String composeContext, String[] composeExtensions, List<String> arguments) {
        List<String> commands = new ArrayList<>();
        commands.add("docker-compose");
        List<String> extensions = new ArrayList<>();
        Arrays.stream(composeExtensions)
                .forEach(e -> {
                    commands.add("-f");
                    commands.add(e);
                });
        commands.addAll(extensions);
        commands.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File(composeContext));
        return processBuilder;
    }
}
