/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.cli;

import com.google.common.collect.ImmutableList;

import com.tableausoftware.TableauException;
import com.tableausoftware.common.Collation;
import com.tableausoftware.common.Result;
import com.tableausoftware.common.Type;
import com.tableausoftware.extract.Extract;
import com.tableausoftware.extract.Row;
import com.tableausoftware.extract.Table;
import com.tableausoftware.extract.TableDefinition;
import com.tableausoftware.server.ServerAPI;
import com.tableausoftware.server.ServerConnection;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static io.airlift.units.Duration.nanosSince;
import static java.util.Objects.requireNonNull;

public class TdePrinter
        implements OutputPrinter
{
    private final List<String> fieldNames;
    private final List<String> fieldTypes;
    private long tdeStart;
    private Extract extract;
    private int totalRows;
    private boolean publishToServer;
    private TableauConfig tableauConfig;
    private String tdefile;

    public TdePrinter(List<String> fieldNames, List<String> fieldTypes, TableauConfig tableauConfig, boolean publish)
    {
        requireNonNull(fieldNames, "fieldNames is null");
        requireNonNull(fieldTypes, "fieldTypes is null");
        this.fieldNames = ImmutableList.copyOf(fieldNames);
        this.fieldTypes = ImmutableList.copyOf(fieldTypes);
        this.publishToServer = publish;
        this.tableauConfig = tableauConfig;
        if (publish) {
            this.tdefile = "tempExtract.tde";
        }
        else {
            this.tdefile = tableauConfig.getExtractName();
        }

        File tdeFilepath = new File(tdefile);
        if (tdeFilepath.exists()) {
            System.out.println("Overwriting existing extract");
            tdeFilepath.delete();
        }

        this.tdeStart = System.nanoTime();
        this.extract = null;
        this.totalRows = 0;
        try {
            extract = new Extract(tdefile);
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public TableDefinition makeTableDefinition() throws TableauException
    {
        System.out.println("Table Schema:");
        TableDefinition tableDef = new TableDefinition();
        tableDef.setDefaultCollation(Collation.EN_GB);
        for (int i = 0; i < fieldNames.size(); i++) {
            switch (fieldTypes.get(i)){
                case "varchar":
                    tableDef.addColumn(fieldNames.get(i), Type.CHAR_STRING);
                    System.out.println("[String] " + fieldNames.get(i));
                    break;
                case "double":
                    tableDef.addColumn(fieldNames.get(i), Type.DOUBLE);
                    System.out.println("[Double] " + fieldNames.get(i));
                    break;
                case "bigint":
                    tableDef.addColumn(fieldNames.get(i), Type.INTEGER);
                    System.out.println("[Integer] " + fieldNames.get(i));
                    break;
                case "integer":
                    tableDef.addColumn(fieldNames.get(i), Type.INTEGER);
                    System.out.println("[Integer] " + fieldNames.get(i));
                    break;
                case "boolean":
                    tableDef.addColumn(fieldNames.get(i), Type.BOOLEAN);
                    System.out.println("[Boolean] " + fieldNames.get(i));
                    break;
                case "date":
                    tableDef.addColumn(fieldNames.get(i), Type.DATE);
                    System.out.println("[Date] " + fieldNames.get(i));
                    break;
                case "timestamp":
                    tableDef.addColumn(fieldNames.get(i), Type.DATETIME);
                    System.out.println("[Datetime] " + fieldNames.get(i));
                    break;
                default:
                    tableDef.addColumn(fieldNames.get(i), Type.CHAR_STRING);
                    System.out.println("[" + fieldTypes.get(i) + " -> String] " + fieldNames.get(i));
                    break;
            }
        }
        return tableDef;
    }

    public void publishExtract()
    {
        try {
            System.out.println("Publishing TDE to Server");
            ServerAPI.initialize();
            ServerConnection serverConnection = new ServerConnection();
            System.out.println("[Host] " + tableauConfig.getHost());
            System.out.println("[Username] " + tableauConfig.getUsername());
            System.out.println("[Password] " + tableauConfig.getPassword());
            System.out.println("[Site ID] " + tableauConfig.getSiteID());
            System.out.println("[Project] " + tableauConfig.getProject());
            System.out.println("[Datasource] " + tableauConfig.getDatasource());
            System.out.println("[Overwrite] " + tableauConfig.getOverwrite());

            serverConnection.connect(tableauConfig.getHost(), tableauConfig.getUsername(), tableauConfig.getPassword(), tableauConfig.getSiteID());
            serverConnection.publishExtract(tdefile, tableauConfig.getProject(), tableauConfig.getDatasource(), tableauConfig.getOverwrite());
            serverConnection.disconnect();
            ServerAPI.cleanup();
        }
        catch (TableauException e) {
            switch(Result.enumForValue(e.getErrorCode())) {
                case INTERNAL_ERROR:
                    System.err.println("INTERNAL_ERROR - Could not parse the response from the server.");
                    break;
                case INVALID_ARGUMENT:
                    System.err.println("INVALID_ARGUMENT - " + e.getMessage());
                    break;
                case CURL_ERROR:
                    System.err.println("CURL_ERROR - " + e.getMessage());
                    break;
                case SERVER_ERROR:
                    System.err.println("SERVER_ERROR - " + e.getMessage());
                    break;
                case NOT_AUTHENTICATED:
                    System.err.println("NOT_AUTHENTICATED - " + e.getMessage());
                    break;
                case BAD_PAYLOAD:
                    System.err.println("BAD_PAYLOAD - Unknown response from the server. Make sure this version of Tableau API is compatible with your server.");
                    break;
                case INIT_ERROR:
                    System.err.println("INIT_ERROR - " + e.getMessage());
                    break;
                case UNKNOWN_ERROR:
                default:
                    System.err.println("An unknown error occured.");
                    break;
            }
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void printRows(List<List<?>> rows, boolean complete)
            throws IOException
    {
        try {
            Table table;
            if (!extract.hasTable("Extract")) {
                TableDefinition tableDef = makeTableDefinition();
                table = extract.addTable("Extract", tableDef);
            }
            else {
                table = extract.openTable("Extract");
            }
            TableDefinition tableDef = table.getTableDefinition();
            for (List<?> row : rows) {
                Row tderow = new Row(tableDef);
                for (int i = 0; i < row.size(); i++) {
                    Object curVal = row.get(i);
                    if (curVal == null) {
                        tderow.setNull(i);
                    }
                    else {
                        switch (fieldTypes.get(i)) {
                            case "varchar":
                                tderow.setCharString(i, curVal.toString());
                                break;
                            case "double":
                                tderow.setDouble(i, (double) curVal);
                                break;
                            case "bigint":
                                tderow.setLongInteger(i, (long) curVal);
                                break;
                            case "integer":
                                tderow.setInteger(i, (int) curVal);
                                break;
                            case "boolean":
                                tderow.setBoolean(i, (boolean) curVal);
                                break;
                            case "date":
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                Date parsedDate = dateFormat.parse((String) curVal);
                                Calendar calDate = Calendar.getInstance();
                                calDate.setTime(parsedDate);
                                tderow.setDate(i, calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH) + 1, calDate.get(Calendar.DAY_OF_MONTH));
                                break;
                            case "timestamp":
                                SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                Date parsedDatetime = datetimeFormat.parse((String) curVal);
                                Calendar calDatetime = Calendar.getInstance();
                                calDatetime.setTime(parsedDatetime);
                                tderow.setDateTime(i, calDatetime.get(Calendar.YEAR), calDatetime.get(Calendar.MONTH) + 1, calDatetime.get(Calendar.DAY_OF_MONTH), calDatetime.get(Calendar.HOUR_OF_DAY), calDatetime.get(Calendar.MINUTE), calDatetime.get(Calendar.SECOND), calDatetime.get(Calendar.MILLISECOND) * 10);
                                break;
                            default:
                                tderow.setCharString(i, curVal.toString());
                                break;
                        }
                    }
                }
                table.insert(tderow);
            }
            totalRows += rows.size();
            System.out.println(totalRows + " rows inserted in " + nanosSince(tdeStart));
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    @Override
    public void finish()
            throws IOException
    {
        try {
            long flushStart = System.nanoTime();
            extract.close();
            System.out.println("Flushed: " + totalRows + " rows in: " + nanosSince(flushStart));
            if (publishToServer) {
                long publishStart = System.nanoTime();
                publishExtract();
                System.out.println("Published extract in " + nanosSince(flushStart));
                File tdeFilepath = new File(tdefile);
                tdeFilepath.delete();
            }
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}
