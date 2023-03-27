package DBEngine;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.Set;

import Exceptions.DBAppException;
import com.opencsv.CSVWriter;

public class DBApp {

    private Vector<Table> tables;
    private File metadataFile;
    public static int intMaxRows;

    public static void main(String[] args) {
    }

    public void init() {
        // create data folder if it doesn't exist
        File dataFolder = new File("data");
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        // go to data folder and create metadata.csv if it doesn't exist
        metadataFile = new File("data/metadata.csv");
        if (!metadataFile.exists()) {
            try {
                metadataFile.createNewFile();
            } catch (Exception e) {
                System.out.println("Error creating metadata file");
            }
        }
        // get max rows from config file
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("src/main/resources/DBApp.config"));
            intMaxRows = Integer.parseInt(prop.getProperty("MaximumRowsCountinTablePage"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get max octree nodes from config file

        // do the rest of the initialization (Still need to figure out what that is)
    }

    // following method creates one table only
    // strClusteringKeyColumn is the name of the column that will be the primary
    // key and the clustering column as well. The data type of that column will
    // be passed in htblColNameType
    // htblColNameValue will have the column name as key and the data
    // type as value
    // htblColNameMin and htblColNameMax for passing minimum and maximum values
    // for data in the column. Key is the name of the column
    //
    //create min and max hashtables in main before create table
    public void createTable(String strTableName,
                            String strClusteringKeyColumn,
                            Hashtable<String,String> htblColNameType,
                            Hashtable<String,String> htblColNameMin,
                            Hashtable<String,String> htblColNameMax ) throws DBAppException, IOException {

        //min/max values based on what?
        //add constraint to config file?

        // verify that none of the inputs are null
        if (strTableName == null) {
            throw new DBAppException("Table name is null");
        }
        if (htblColNameType.get(strClusteringKeyColumn) == null) {
            throw new DBAppException("Clustering key column not found");
        }
        if (htblColNameMin.get(strClusteringKeyColumn) == null) {
            throw new DBAppException("Clustering key column min value not found");
        }
        if (htblColNameMax.get(strClusteringKeyColumn) == null) {
            throw new DBAppException("Clustering key column max value not found");
        }

        // verify datatype of all hashtables
        Set<Entry<String, String>> entrySet = htblColNameType.entrySet();
        for(Entry<String, String> entry : entrySet){
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            if(!(columnType.equals("java.lang.Integer") || columnType.equals("java.lang.Double") || columnType.equals("java.lang.String") || columnType.equals("java.util.Date"))) {
                throw new DBAppException("Invalid data type");
            } else{
                if(htblColNameMin.get(columnName) == null){
                    throw new DBAppException("Column min value not found");
                }
                if(htblColNameMax.get(columnName) == null){
                    throw new DBAppException("Column max value not found");
                }
            }
        }

        // check if table already exists
        BufferedReader br = new BufferedReader(new FileReader(metadataFile)); // read csv file
        String line = br.readLine();
        while (line != null) { // loop over all lines
            String[] values = line.split(",");
            if (values[0].equals(strTableName)) { // check if table name already exists
                throw new DBAppException("Table already exists"); // if it does, throw exception
            }
            line = br.readLine();
        }
        br.close();

        // write to metadata file
        CSVWriter writer = new CSVWriter(new FileWriter(metadataFile, true));
        //String csvEntry = strTableName;
        entrySet = htblColNameType.entrySet(); // what is ht???
        for (Entry<String, String> entry : entrySet) {
            //csvEntry = strTableName;
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            boolean clusteringKey = columnName.equals(strClusteringKeyColumn);
            //csvEntry += ", " + columnName + "," + columnType + "," + clusteringKey + ",null,null,";
            String min = htblColNameMin.get(columnName);
            String max = htblColNameMax.get(columnName);
            //csvEntry += min + "," + max;
            String[] csvEntry = {strTableName, columnName, columnType, Boolean.toString(clusteringKey), "null", "null", min, max}; // create csv entry
        }

        Table table = new Table(strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin,
                                htblColNameMax, "data/"); // not sure about the path

        tables.add(table); // add table to tables vector
    }

    // following method creates an octree
    // depending on the count of column names passed.
    // If three column names are passed, create an octree.
    // If only one or two column names is passed, throw an Exception.
    public void createIndex(String strTableName, String strarrColName) throws DBAppException {
        // Check if the table exists
        // If it doesn't, throw an exception
        // If it does, create a new index
        // Create a new index object
        // Add the index to the table
        // Save the table
    }

    // following method inserts one row only.
    // htblColNameValue must include a value for the primary key
    public void insertIntoTable(String strTableName,
                                Hashtable<String,Object> htblColNameValue) throws DBAppException {
        // Rows should be sorted by primary key

        // Check if the table exists
        // If it doesn't, throw an exception
        // If it does, insert the record
        // Create a new record object
        // Add the record to the table
        // Save the table

        //exceptions when inserting
        /*
        1-data type mismatch when comparing to csv
        2-primary key duplicated
         */
    }
    
    // following method updates one row only
    // htblColNameValue holds the key and new value
    // htblColNameValue will not include clustering key as column name
    // strClusteringKeyValue is the value to look for to find the row to update.
    public void updateTable(String strTableName,
                            String strClusteringKeyValue,
                            Hashtable<String,Object> htblColNameValue) throws DBAppException {
        // Check if the table exists
        // If it doesn't, throw an exception
        // If it does, update the record
        // Create a new record object
        // Add the record to the table
        // Save the table
    }

    // following method could be used to delete one or more rows.
    // htblColNameValue holds the key and value. This will be used in search
    // to identify which rows/tuples to delete.
    // htblColNameValue enteries are ANDED together
    public void deleteFromTable(String strTableName,
                                Hashtable<String,Object> htblColNameValue) throws DBAppException {
        // Check if the table exists
        // If it doesn't, throw an exception
        // If it does, delete the record
        // Create a new record object
        // Add the record to the table
        // Save the table
    }

    public Iterator selectFromTable(SQLTerm[] arrSQLTerms,
                                    String[] strarrOperators) throws DBAppException {
        // Check if the table exists
        // If it doesn't, throw an exception
        // If it does, select the records
        // Create a new record object
        // Add the record to the table
        // Save the table
        // Return the iterator
        return null;
    }
}
