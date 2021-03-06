package com.test.vendor._cubrid;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import com.test.AbstractTestSuite;

public abstract class ConcreteCubridTestNopeSuite extends AbstractTestSuite {

    public ConcreteCubridTestNopeSuite () throws ClassNotFoundException, SQLException {
        
        // Explicit class name declaration
        Class.forName("cubrid.jdbc.driver.CUBRIDDriver");
        
        this.jdbcURL = "jdbc:cubrid:"+ AbstractTestSuite.HOSTNAME +":33000:demodb:::";
        
        this.jdbcUser = StringUtils.EMPTY;
        this.jdbcPass = StringUtils.EMPTY;
        this.jsqlDatabaseName = "PUBLIC";
        this.jsqlTableName = "code";
        this.jsqlColumnName = "f_name";
        
        this.jdbcColumnForDatabaseName = "owner_name";
        this.jdbcColumnForTableName = "class_name";
        this.jdbcColumnForColumnName = "attr_name";
        
        this.jdbcQueryForDatabaseNames = "select "+ this.jdbcColumnForDatabaseName +" from db_class";
        this.jdbcQueryForTableNames = "select "+ this.jdbcColumnForTableName +" from db_class where owner_name='"+ this.jsqlDatabaseName +"'";
        this.jdbcQueryForColumnNames = "select "+ this.jdbcColumnForColumnName +" from db_attribute c inner join db_class t on t.class_name = c.class_name where t.owner_name='"+ this.jsqlDatabaseName +"' and t.class_name='"+ this.jsqlTableName +"'";
        this.jdbcQueryForValues = "SELECT "+ this.jsqlColumnName +" FROM "+ this.jsqlDatabaseName +"."+ this.jsqlTableName;
        
        this.requestJdbc();
    }
}
