package com.relationaljunction.jdbc.common.h2.engine;

import org.h2.api.TableEngine;
import org.h2.command.ddl.CreateTableData;
import org.h2.table.TableBase;

/**
 * custom external table implementation
 */
abstract public class H2TableEngine implements TableEngine {

   abstract public TableBase createTable(CreateTableData data);
}


