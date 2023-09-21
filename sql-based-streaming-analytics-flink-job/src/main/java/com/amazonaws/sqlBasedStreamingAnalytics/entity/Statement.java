package com.amazonaws.sqlBasedStreamingAnalytics.entity;

public class Statement {

    private final String statementSql;
    private final StatementType statementType;

    public Statement(String statementSql, StatementType statementType) {
        this.statementSql = statementSql;
        this.statementType = statementType;
    }

    public String getStatementSql() {
        return statementSql;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    @Override public String toString() {
        return "Statement{" + "statementSql='" + statementSql + '\'' + ", statementType=" + statementType + '}';
    }
}
