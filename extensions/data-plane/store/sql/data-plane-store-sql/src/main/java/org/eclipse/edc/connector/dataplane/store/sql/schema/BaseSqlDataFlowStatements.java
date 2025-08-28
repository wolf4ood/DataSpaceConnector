/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.store.sql.schema;

import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.DataFlowMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDataFlowStatements implements DataFlowStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDataFlowStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getUpsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getErrorDetailColumn())
                .column(getCallbackAddressColumn())
                .jsonColumn(getSourceColumn())
                .jsonColumn(getDestinationColumn())
                .jsonColumn(getPropertiesColumn())
                .column(getFlowTypeColumn())
                .column(getTransferTypeDestinationColumn())
                .column(getRuntimeIdColumn())
                .jsonColumn(getResourceDefinitionsColumn())
                .upsertInto(getDataPlaneTable(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getDataPlaneTable());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new DataFlowMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec) {
        var queryTemplate = "%s LEFT JOIN %s l ON %s.%s = l.%s".formatted(getSelectTemplate(), getLeaseTableName(), getDataPlaneTable(), getIdColumn(), getLeaseIdColumn());
        return new SqlQueryStatement(queryTemplate, querySpec, new DataFlowMapping(this), operatorTranslator);
    }

    @Override
    public String getInsertLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .insertInto(getLeaseTableName());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .update(getDataPlaneTable(), getIdColumn());
    }

    @Override
    public String getNotLeasedFilter() {
        return format("(l.%s IS NULL OR (? > (%s + %s)))",
                getLeaseIdColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }
}
