package DBEngine;

import Exceptions.DBAppException;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

public class Table implements Serializable {
    private String _strTableName;
    private String _strClusteringKeyColumn;
    private String _strPath;
    private Hashtable<String, String> _htblColNameType;
    private Hashtable<String, String> _htblColNameMin;
    private Hashtable<String, String> _htblColNameMax;
    private Vector<String> _pagesID;
    private int _intNumberOfRows;

    public Table(String strTableName, String strClusteringKeyColumn, Hashtable<String, String> htblColNameType, Hashtable<String, String> htblColNameMin, Hashtable<String, String> htblColNameMax, String strPath) {
        _strTableName = strTableName;
        _strClusteringKeyColumn = strClusteringKeyColumn;
        _htblColNameType = htblColNameType;
        _htblColNameMin = htblColNameMin;
        _htblColNameMax = htblColNameMax;
        _strPath = strPath;
        _pagesID = new Vector<String>();
        _intNumberOfRows = 0;
        saveTable();
    }

    public Table(String strTableName, String strPath) {
        _strTableName = strTableName;
        _strPath = strPath;
    }

    public void insertRow(Hashtable<String, Object> htblNewRow) throws DBAppException {
        //     if the page exists, add the row to it
        Object objClusteringKeyValue = htblNewRow.get(_strClusteringKeyColumn);
        Page page = getPageFromClusteringKey(objClusteringKeyValue);

        if (page == null) { // if page not found, create a new page
            if (_pagesID.size() > 0) {
                int lastPageID = Integer.parseInt(_pagesID.get(_pagesID.size() - 1)); // get the id of the last page
                page = new Page(lastPageID + 1 + "", _strPath, _strTableName); // create a new page with the id of the last page + 1
            } else
                page = new Page(0 + "", _strPath, _strTableName); // create a new page with the id 0

            page.addRow(htblNewRow);
            _pagesID.add(page.get_strPageID());
            page.unloadPage();
        } else {
            int intRowID = page.binarySearchForInsertion(objClusteringKeyValue, _strClusteringKeyColumn); // get the row id to insert the row in
            page.addRow(htblNewRow, intRowID); // add the row to the page
            if (page.get_intNumberOfRows() > DBApp.intMaxRows) // if the page is full, split it
                splitPage(page, _pagesID.indexOf(page.get_strPageID()));

            page.unloadPage();
        }
        _intNumberOfRows++;
    }


    public void deleteRow(Page page, int intRowID) {
        page.deleteRow(intRowID); // delete the row from the page
        if (page.get_intNumberOfRows() == 0) { // delete the page if it is empty
            _pagesID.remove(page.get_strPageID());
            page.deletePage();
        }
        _intNumberOfRows--;
        page.unloadPage();
    }

    public void updateRow(Hashtable<String, Object> htblNewRow, Object objClusteringKeyValue) throws DBAppException {
        Page page = getPageFromClusteringKey(objClusteringKeyValue);
        int intRowID = getRowIDFromClusteringKey(page, objClusteringKeyValue);
        if (page == null || intRowID == -1) // if page or row not found,
            return; // don't do anything
        page.updateRow(intRowID, htblNewRow);
        page.unloadPage();
    }

    public void splitPage(Page currPage, int intCurrPageIndex) throws DBAppException { // splits page if it is full
        int lastRowIDinPage = currPage.get_rows().size() - 1; // get the id of the last row in the page
        Hashtable<String, Object> lastRow = currPage.get_rows().get(lastRowIDinPage); // get the last row in the page

        if (intCurrPageIndex == _pagesID.size() - 1) { // if the page is the last page in the table
            int lastPageID = Integer.parseInt(_pagesID.get(_pagesID.size() - 1)); // get the id of the last page
            Page newPage = new Page(lastPageID + 1 + "", _strPath, _strTableName); // create a new page with the id of the last page + 1
            newPage.addRow(lastRow);
            _pagesID.add(newPage.get_strPageID());
            newPage.unloadPage();
        } else { // if the page is not the last page in the table
            int intNextPageIndex = intCurrPageIndex + 1; // get the id of the next page
            String nextPageID = _pagesID.get(intNextPageIndex); // get the next page
            Page nextPage = Page.loadPage(_strPath, _strTableName, nextPageID); // load the next page
            nextPage.addRow(lastRow, 0); // add the last row to the next page as the first row
            if (nextPage.get_intNumberOfRows() > DBApp.intMaxRows) // if the next page is full, split it
                splitPage(nextPage, intNextPageIndex);
            nextPage.unloadPage();
        }
        currPage.deleteRow(lastRowIDinPage); // delete the last row from the current page
    }

    public Page getPageFromClusteringKey(Object objClusteringKeyValue) throws DBAppException {
        for (int i = 0; i < _pagesID.size(); i++) {
            String pageID = _pagesID.get(i); // get the id of the page
            Page page = Page.loadPage(_strPath, _strTableName, pageID); // load the page
            Object firstRowClusteringKey = page.get_rows().get(0).get(_strClusteringKeyColumn); // get the clustering key of the first row in the page
            Object lastRowClusteringKey = page.get_rows().get(page.get_rows().size() - 1).get(_strClusteringKeyColumn); // get the clustering key of the last row in the page
            if (((Comparable) firstRowClusteringKey).compareTo(objClusteringKeyValue) >= 0)  // if the clustering key of the first row is greater than or equal to the clustering key of the row to be inserted
                return page;
            if (((Comparable) lastRowClusteringKey).compareTo(objClusteringKeyValue) >= 0) // if the clustering key of the last row is greater than or equal to the clustering key of the row to be inserted
                return page; // return the page
            if (page.get_intNumberOfRows() < DBApp.intMaxRows) { // if the page is not full and in between the clustering keys of the first and last rows
                if (i == _pagesID.size() - 1)
                    return page;
                String nextPageID = _pagesID.get(i + 1); // get the next page ID
                Page nextPage = Page.loadPage(_strPath, _strTableName, nextPageID); // load the next page
                Object nextPageFirstRowClusteringKey = nextPage.get_rows().get(0).get(_strClusteringKeyColumn); // get the clustering key of the first row in the next page
                nextPage.unloadPage();
                if (((Comparable) nextPageFirstRowClusteringKey).compareTo(objClusteringKeyValue) > 0) // if the clustering key of the first row in the next page is greater than the clustering key of the row to be inserted
                    return page;
            }
            page.unloadPage(); // unload the page before moving on to the next one
        }
        return null; // if the page is not found return null
    }

    public int getRowIDFromClusteringKey(Page page, Object objClusteringKeyValue) throws DBAppException {
        if (page == null)
            return -1; // if the page is not found return null ( TODO: use this to create a new page when inserting)
        int rowId = page.getRowID(objClusteringKeyValue, _strClusteringKeyColumn);
        return rowId;
    }

    public Object getClusteringKeyFromRow(Hashtable<String, Object> htblColNameValue) {
        return htblColNameValue.get(_strClusteringKeyColumn);
    }

    // should we have save table and load table methods?
    public void saveTable() {
        File file = new File(_strPath + _strTableName + ".ser");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTable() {
        File file = new File(_strPath + _strTableName + ".ser");
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Table table = (Table) ois.readObject();
            ois.close();
            fis.close();
            _strClusteringKeyColumn = table.get_strClusteringKeyColumn();
            _htblColNameType = table.get_htblColNameType();
            _htblColNameMin = table.get_htblColNameMin();
            _htblColNameMax = table.get_htblColNameMax();
            _pagesID = table.get_pagesID();
            _intNumberOfRows = table.get_intNumberOfRows();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void unloadTable() {
        saveTable();
        _strClusteringKeyColumn = null;
        _htblColNameType = null;
        _htblColNameMin = null;
        _htblColNameMax = null;
        _pagesID = null;
        _intNumberOfRows = 0;
    }

    private void deleteTableFile() {
        File file = new File(_strPath + _strTableName + ".ser");
        file.delete();
    }

    @Override
    public String toString() {
        String pages = "";
        for (String pageID : _pagesID) {
            try {
                Page page = Page.loadPage(_strPath, _strTableName, pageID);
                pages += page + "\n";
                page.unloadPage();
            } catch (DBAppException e) {
            }
        }
        return pages;
    }


    // getters and setters


    public String get_strTableName() {
        return _strTableName;
    }

    public void set_strTableName(String _strTableName) {
        this._strTableName = _strTableName;
    }

    public String get_strClusteringKeyColumn() {
        return _strClusteringKeyColumn;
    }

    public void set_strClusteringKeyColumn(String _strClusteringKeyColumn) {
        this._strClusteringKeyColumn = _strClusteringKeyColumn;
    }

    public Hashtable<String, String> get_htblColNameType() {
        return _htblColNameType;
    }

    public void set_htblColNameType(Hashtable<String, String> _htblColNameType) {
        this._htblColNameType = _htblColNameType;
    }

    public Hashtable<String, String> get_htblColNameMin() {
        return _htblColNameMin;
    }

    public void set_htblColNameMin(Hashtable<String, String> _htblColNameMin) {
        this._htblColNameMin = _htblColNameMin;
    }

    public Hashtable<String, String> get_htblColNameMax() {
        return _htblColNameMax;
    }

    public void set_htblColNameMax(Hashtable<String, String> _htblColNameMax) {
        this._htblColNameMax = _htblColNameMax;
    }

    public String get_strPath() {
        return _strPath;
    }

    public void set_strPath(String _strPath) {
        this._strPath = _strPath;
    }

    public Vector<String> get_pagesID() {
        return _pagesID;
    }

    public int get_intNumberOfRows() {
        return _intNumberOfRows;
    }

}
