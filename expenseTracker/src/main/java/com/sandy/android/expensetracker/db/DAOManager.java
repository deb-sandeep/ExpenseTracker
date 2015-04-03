package com.sandy.android.expensetracker.db;

import android.app.Activity ;
import android.database.sqlite.SQLiteDatabase ;

/**
 * This is a singleton class for managing all the DAO implementations. 
 * 
 * @author Sandeep Deb
 */
public class DAOManager {

    private static DAOManager instance = null ;
    
    private DBHelper dbHelper = null ;
    private SQLiteDatabase db = null ;
    
    private ExpenseItemDAO expenseItemDAO = null ;
    private CategoryDAO    categoryDAO    = null ;
    
    private DAOManager() {
    }
    
    public static void initialize( Activity activity ) {
        
        instance = new DAOManager() ;
        instance.dbHelper = new DBHelper( activity ) ;
        instance.db = instance.dbHelper.getWritableDatabase() ;
        
        instance.expenseItemDAO = new ExpenseItemDAOImpl( activity, instance.db ) ;
        instance.categoryDAO    = new CategoryDAOImpl( activity, instance.db ) ;
    }
    
    public static DAOManager getInstance() {
        return instance ;
    }
    
    public ExpenseItemDAO getExpenseItemDAO() {
        return expenseItemDAO ;
    }
    
    public CategoryDAO getCategoryDAO() {
        return categoryDAO ;
    }
    
    public void closeDB() {
        instance.db.close() ;
    }
}
