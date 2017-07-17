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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import static com.facebook.presto.cli.CsvPrinter.formatValue;
import static java.util.Objects.requireNonNull;

public class SqpugPrinter
        implements OutputPrinter
{
    private final List<String> fieldNames;
    private final Writer writer;
    private static final char RECORD_SEPARATOR = 0x1e;

    public SqpugPrinter(List<String> fieldNames, Writer writer)
    {
        this.fieldNames = ImmutableList.copyOf(requireNonNull(fieldNames, "fieldNames is null"));
        this.writer = requireNonNull(writer, "writer is null");
    }

    @Override
    public void printRows(List<List<?>> rows, boolean complete)
            throws IOException
    {
        for (List<?> row : rows) {
            writer.write(formatRow(fieldNames, row));
        }
    }

    @Override
    public void finish()
            throws IOException
    {
        printRows(ImmutableList.of(), true);
        writer.flush();
    }

    private static String formatRow(List<String> fieldNames, List<?> row)
    {
        StringBuilder sb = new StringBuilder();
        Iterator<String> fieldIter = fieldNames.iterator();
        Iterator<?> valueIter = row.iterator();
        if (fieldIter.hasNext() && valueIter.hasNext()) {
            String target = formatValue(valueIter.next());
            fieldIter.next();
            sb.append(target);
            sb.append('|');
        }

        while (fieldIter.hasNext() && valueIter.hasNext()) {
            String field = fieldIter.next().toString();
            String value = formatValue(valueIter.next());
            if (field.startsWith("_")) {
                field = field.substring(1);
            }
            sb.append(field);
            sb.append('&');
            sb.append(value);
            if (fieldIter.hasNext()) {
                sb.append(RECORD_SEPARATOR);
            }
        }
        sb.append('\n');
        return sb.toString();
    }
}
