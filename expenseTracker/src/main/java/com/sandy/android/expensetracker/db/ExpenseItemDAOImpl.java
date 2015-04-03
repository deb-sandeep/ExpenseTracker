package com.sandy.android.expensetracker.db;

import java.util.ArrayList ;
import java.util.Date ;
import java.util.List ;

import android.content.ContentValues ;
import android.content.Context ;
import android.database.Cursor ;
import android.database.sqlite.SQLiteDatabase ;
import android.util.Log ;

import com.sandy.android.expensetracker.BuildConfig ;
import com.sandy.android.expensetracker.R ;
import com.sandy.android.expensetracker.util.LogTag ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * A concrete implementation of ExpenseItemDAO backed up the SQLite database.
 * 
 *           CREATE TABLE expense_item ( 
 *               _id          INTEGER PRIMARY KEY AUTOINCREMENT,
 *               date         INTEGER NOT NULL,
 *               cat_id       INTEGER REFERENCES category ( _id ),
 *               subcat_id    INTEGER REFERENCES sub_category ( _id ),
 *               paid_by      TEXT    NOT NULL,
 *               amount       INTEGER NOT NULL,
 *               description  TEXT 
 *           )
 * 
 * @author Sandeep Deb
 */
class ExpenseItemDAOImpl implements ExpenseItemDAO {
    
    public static final String TABLE_NAME_EXPENSE_ITEM = "expense_item" ;
    
    public static final String COL_NAME_ID       = "_id" ;
    public static final String COL_NAME_DATE     = "date" ;
    public static final String COL_NAME_CAT_ID   = "cat_id" ;
    public static final String COL_NAME_SUBCAT_ID= "subcat_id" ;
    public static final String COL_NAME_PAID_BY  = "paid_by" ;
    public static final String COL_NAME_AMT      = "amount" ;
    public static final String COL_NAME_DESC     = "description" ;
    
    private SQLiteDatabase db = null ;
    private Context context = null ;
    
    public ExpenseItemDAOImpl( Context context, SQLiteDatabase db ) {
        this.db = db ;
        this.context = context ;
    }
    
    @Override
    public ExpenseItem create( ExpenseItem item )
            throws IllegalArgumentException {
        
        if( item == null || item.getId() != -1 ) {
            throw new IllegalArgumentException( "Expense item is either null " +
            		                        "or is not a new item, id != -1" ) ;
        }

        ContentValues cv = new ContentValues() ;
        cv.put( COL_NAME_DATE,     item.getDate().getTime() ) ;
        cv.put( COL_NAME_CAT_ID,   item.getCatId() ) ;
        cv.put( COL_NAME_SUBCAT_ID,item.getSubCatId() ) ;
        cv.put( COL_NAME_PAID_BY,  item.getPaidBy() ) ;
        cv.put( COL_NAME_AMT,      item.getAmount() ) ;
        cv.put( COL_NAME_DESC,     item.getDescription() ) ;
        
        int id = ( int )db.insert( TABLE_NAME_EXPENSE_ITEM, null, cv ) ;
        
        if( id == -1 ) {
            Log.e( LogTag.EXPENSE_ITEM_DAO, 
                   "Could not insert expense item into database" ) ;
            item = null ;
        }
        else {
            // Populate the database identity into the object instance 
            if( BuildConfig.DEBUG ) {
                Log.d( LogTag.EXPENSE_ITEM_DAO, "Id of new expense item is " + id ) ;
            }
            item.setId( id ) ;
        }
        
        return item ;
    }

    @Override
    public boolean update( ExpenseItem item ) throws IllegalArgumentException {
        
        if( item == null || item.getId() == -1 ) {
            throw new IllegalArgumentException( "Expense item is either null " +
                                            "or is a new item, id == -1" ) ;
        }

        ContentValues cv = new ContentValues() ;
        cv.put( COL_NAME_DATE,     item.getDate().getTime() ) ;
        cv.put( COL_NAME_CAT_ID,   item.getCatId() ) ;
        cv.put( COL_NAME_SUBCAT_ID,item.getSubCatId() ) ;
        cv.put( COL_NAME_PAID_BY,  item.getPaidBy() ) ;
        cv.put( COL_NAME_AMT,      item.getAmount() ) ;
        cv.put( COL_NAME_DESC,     item.getDescription() ) ;
        
        int id = ( int )db.update( TABLE_NAME_EXPENSE_ITEM, cv, COL_NAME_ID + "=" + item.getId(), null ) ;
        
        if( id == -1 || id == 0 ) {
            Log.e( LogTag.EXPENSE_ITEM_DAO, 
                   "Could not update expense item in database" ) ;
            return false ;
        }
        else {
            if( BuildConfig.DEBUG ) {
                Log.d( LogTag.EXPENSE_ITEM_DAO, "Expense item successfully updated" ) ;
            }
        }
        
        return true ;
    }

    @Override
    public List<ExpenseItem> getAllExpenseItems() {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.EXPENSE_ITEM_DAO, "Fetching all expense items" ) ;
        }
        
        String selectAllSQL = context.getString( R.string.query_select_all_expense_items ) ;
        
        ArrayList<ExpenseItem> expenseItems = new ArrayList<ExpenseItem>() ;
        Cursor c = db.rawQuery( selectAllSQL, null ) ;
        c.moveToFirst() ;
        
        while( !c.isAfterLast() ) {
            
            ExpenseItem item = new ExpenseItem() ;
            
            item.setId          ( c.getInt    ( 0 ) ) ;
            item.setDate        ( new Date( c.getLong( 1 ) ) ) ;
            item.setCatId       ( c.getInt    ( 2 ) ) ;
            item.setSubCatId    ( c.getInt    ( 3 ) ) ;
            item.setPaidBy      ( c.getString ( 4 ) ) ;
            item.setAmount      ( c.getInt    ( 5 ) ) ;
            item.setDescription ( c.getString ( 6 ) ) ;
            
            expenseItems.add( item ) ;
            
            // Gawd! I had forgotten this and it was a vision into infinity 
            // and a hot air blowing CPU.
            c.moveToNext() ;
        }
        
        c.close() ;
        
        return expenseItems ;
    }

    @Override
    public boolean delete( ExpenseItem item ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.EXPENSE_ITEM_DAO, 
                   "Delete expense item with id = " + item.getId() ) ;
        }
        
        int flag = db.delete( TABLE_NAME_EXPENSE_ITEM, 
                              COL_NAME_ID + "=" + item.getId(), null ) ;
        
        return flag > 0 ? true : false ;
    }
    
    @Override
    public void deleteAll() {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.EXPENSE_ITEM_DAO, "Deleting all data in the database" ) ;
        }
        
        db.delete( TABLE_NAME_EXPENSE_ITEM, null, null ) ;
    }
    
    /**
     * This method returns a true if and only if there are one or more expense
     * items which refer to the supplied category id.
     */
    public boolean isCategoryUsed( int catId ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.EXPENSE_ITEM_DAO, "Checking for category association" ) ;
        }
        
        boolean retVal = false ;
        
        String sql = context.getString( R.string.query_is_cat_used ) ;
        sql = sql.replace( "?", "" + catId ) ;
        
        Cursor c = db.rawQuery( sql, null ) ;
        c.moveToFirst() ;
        if( c.getInt( 0 ) > 0 ) {
            retVal = true ;
        }
        c.close() ;
        
        return retVal ;
    }

    /**
     * This method returns a true if and only if there are one or more expense
     * items which refer to the supplied sub category id.
     */
    public boolean isSubCategoryUsed( int subCatId ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.EXPENSE_ITEM_DAO, "Checking for sub-category association" ) ;
        }
        
        boolean retVal = false ;
        
        String sql = context.getString( R.string.query_is_subcat_used ) ;
        sql = sql.replace( "?", "" + subCatId ) ;
        
        Cursor c = db.rawQuery( sql, null ) ;
        c.moveToFirst() ;
        if( c.getInt( 0 ) > 0 ) {
            retVal = true ;
        }
        c.close() ;
        
        return retVal ;
    }
}
