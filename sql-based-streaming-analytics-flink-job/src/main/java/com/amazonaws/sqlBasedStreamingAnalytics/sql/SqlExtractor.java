package com.amazonaws.sqlBasedStreamingAnalytics.sql;


import com.amazonaws.sqlBasedStreamingAnalytics.entity.Statement;
import com.amazonaws.sqlBasedStreamingAnalytics.entity.StatementType;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;


public class SqlExtractor {

    private final S3Client s3Client = S3Client.builder().build();

    public Set<Statement> parseSql(String sqlFileName, Properties properties) throws IOException, URISyntaxException {
        Set<Statement> statements = new LinkedHashSet<>();
        try (InputStream resourceAsStream = getSqlFileInputStream(sqlFileName)) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                    resourceAsStream)))) {
                return getStatements(properties, bufferedReader, statements);
            }
        }
    }

    private Set<Statement> getStatements(Properties properties,
                                         BufferedReader bufferedReader,
                                         Set<Statement> statements) throws IOException {
        StringBuilder resultBuilder = new StringBuilder();
        String line;
        StatementType currentStatementType = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("--")) {
                currentStatementType = determineStatementType(line, currentStatementType);
                String tplLine = line;
                for (Object key : properties.keySet()) {
                    tplLine =
                            StringUtils.replace(tplLine,
                                    "##" + key.toString() + "##",
                                    properties.getProperty(key.toString()));
                }
                resultBuilder.append(tplLine);
                if (line.endsWith(";;")) {
                    if (currentStatementType == null) {
                        throw new RuntimeException("Error parsing SQL: " + resultBuilder);
                    }
                    statements.add(new Statement(resultBuilder.toString(), currentStatementType));
                    currentStatementType = null;
                    resultBuilder = new StringBuilder();
                } else {
                    resultBuilder.append(System.lineSeparator());
                }
            }
        }
        return statements;
    }

    private InputStream getSqlFileInputStream(String sqlFileName) throws URISyntaxException {
        InputStream resourceAsStream;
        if (sqlFileName.startsWith("s3://")) {
            URI uri = new URI(sqlFileName);
            resourceAsStream =
                    s3Client
                            .getObjectAsBytes(GetObjectRequest
                                    .builder()
                                    .bucket(uri.getHost())
                                    .key(uri.getPath().substring(1))
                                    .build())
                            .asInputStream();
        } else {
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(sqlFileName);
        }
        return resourceAsStream;
    }

    private StatementType determineStatementType(String line, StatementType currentStatementType) {
        if (currentStatementType == null) {
            if (line.startsWith("CREATE")) {
                currentStatementType = StatementType.CREATE;
            } else if (line.startsWith("SELECT")) {
                currentStatementType = StatementType.SELECT;
            } else if (line.startsWith("INSERT")) {
                currentStatementType = StatementType.INSERT;
            }
        }
        return currentStatementType;
    }

}
