package com.relationaljunction.jdbc;

import java.nio.file.Path;
import java.nio.file.Paths;

public class filepath {

    public static String pathgetter(String fileName) {
        Path path = Paths.get(fileName).toAbsolutePath();
        return String.valueOf(path);
    }

}
