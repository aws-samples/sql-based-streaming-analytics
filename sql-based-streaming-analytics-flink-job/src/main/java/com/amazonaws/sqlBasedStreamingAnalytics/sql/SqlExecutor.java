// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlBasedStreamingAnalytics.sql;

import com.amazonaws.sqlBasedStreamingAnalytics.entity.Statement;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Set;

public class SqlExecutor {

    private final SqlExtractor sqlExtractor = new SqlExtractor();
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutor.class);

    public void extractAndExecuteSql(StreamTableEnvironment tableEnv, String sqlFileName, Properties properties)
            throws IOException, URISyntaxException {
        StreamStatementSet statementSet = tableEnv.createStatementSet();
        Set<Statement> statements = sqlExtractor.parseSql(sqlFileName, properties);
        for (Statement statement : statements) {
            String statementSql = statement.getStatementSql();
            switch (statement.getStatementType()) {
                case CREATE:
                case SELECT:
                    try {
                        tableEnv.executeSql(statementSql.replaceAll(";", ""));
                    } catch (RuntimeException e) {
                        LOGGER.error("Error creating select statement " + statementSql, e);
                        throw e;
                    }
                    break;
                case INSERT:
                    statementSet.addInsertSql(statementSql.replaceAll(";;", ""));
                    break;
            }
        }
        statementSet.execute();
    }

}
