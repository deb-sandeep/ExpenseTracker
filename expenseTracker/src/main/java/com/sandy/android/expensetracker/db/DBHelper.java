package com.sandy.android.expensetracker.db;

import java.util.HashMap ;

import android.content.ContentValues ;
import android.content.Context ;
import android.database.sqlite.SQLiteDatabase ;
import android.database.sqlite.SQLiteOpenHelper ;
import android.util.Log ;

import com.sandy.android.expensetracker.BuildConfig ;
import com.sandy.android.expensetracker.R ;
import com.sandy.android.expensetracker.util.LogTag ;

/**
 * This is an extension of the {@link SQLiteOpenHelper} class and helps to create
 * or update the database used by the ExpenseTracker application.
 * 
 * @author Sandeep Deb
 */
public class DBHelper extends SQLiteOpenHelper {
    
    public  final static String DB_NAME    = "ExpenseTrackerDB" ;
    private final static int    DB_VERSION = 5 ;
    
    private final static String CAT_TAB_NAME            = "category" ;
    private final static String SUBCAT_TAB_NAME         = "sub_category" ;
    
    private final static String CAT_TAB_NAME_COL_NM     = "name" ;
    private final static String CAT_TAB_SEQNO_COL_NM    = "sequence_no" ;
    private final static String SUBCAT_TAB_NAME_COL_NM  = "name" ;
    private final static String SUBCAT_TAB_SEQNO_COL_NM = "sequence_no" ;
    private final static String SUBCAT_TAB_CATID_COL_NM = "cat_id" ;
    
    // A map which stores the initial reference data for categories and associated
    // sub categories for initial database population
    private HashMap<String, Integer> catSubCatIdMap = new HashMap<String, Integer>() ;
    
    // A reference to the context under which this database is operating.
    private Context context = null ;

    /** A simplified constructor to help create the DB helper. */
    public DBHelper( Context context ) {
        super( context, DB_NAME, null, DB_VERSION ) ;
        this.context = context ;
        populateCatSubCatIDMap() ;
    }

    private void populateCatSubCatIDMap() {
        catSubCatIdMap.put( context.getString( R.string.cat_clothes),             R.array.subcat_clothes ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_contingency_expense), R.array.subcat_contingency ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_donation),            R.array.subcat_donation_gift ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_fancy_entertainment), R.array.subcat_fancy_entertainment ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_food),                R.array.subcat_food ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_fuel_parking),        R.array.subcat_fuel_parking ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_grocery_household),   R.array.subcat_grocery_and_household ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_house_maintenance),   R.array.subcat_house_maintenance ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_income),              R.array.subcat_income ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_investment),          R.array.subcat_investment ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_medicines),           R.array.subcat_medicines ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_monthly_bill),        R.array.subcat_monthly_bill ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_school),              R.array.subcat_school ) ;
        catSubCatIdMap.put( context.getString( R.string.cat_vehicle_maintenance), R.array.subcat_vehicle_maintenance ) ;
    }
    
    /**
     * This method will be called if the expense tracker database does not
     * exist. We create the database in this method.
     */
    @Override
    public void onCreate( SQLiteDatabase db ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.DB_HELPER, "Creating a new database" ) ;
        }
        
        String[] createStmts = context.getResources().getStringArray( R.array.create_tables ) ;
        
        try {
            for( String createStmt : createStmts ) {
                if( BuildConfig.DEBUG ) {
                    Log.d( LogTag.DB_HELPER, "Create table query = " + createStmt ) ;
                }
                db.execSQL( createStmt ) ;
            }
            
            populateReferenceData( db ) ;
        }
        catch( Exception e ) {
            Log.e( LogTag.DB_HELPER, "Error creating database", e ) ;
        }
    }

    /**
     * If the database version changes (upward), this method will be called to
     * give us an opportunity to gracefully upgrade the database with any
     * structural and/or data migration needs.
     */
    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.DB_HELPER, "Upgrading the database from version " +
                                     oldVersion + " to " + newVersion ) ;
        }
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.DB_HELPER, "Creating a new database" ) ;
        }
        
        String[] dropTableStmts = context.getResources().getStringArray( R.array.drop_tables ) ;
        
        try {
            for( String dropTableStmt : dropTableStmts ) {
                if( BuildConfig.DEBUG ) {
                    Log.d( LogTag.DB_HELPER, "Dropping table query = " + dropTableStmt ) ;
                }
                db.execSQL( dropTableStmt ) ;
            }
            onCreate( db ) ;
        }
        catch( Exception e ) {
            Log.e( LogTag.DB_HELPER, "Error dropping table", e ) ;
        }
    }
    
    /**
     * This function populates the reference data for the categories and associated
     * sub categories during table creation.
     */
    private void populateReferenceData( SQLiteDatabase db ) {

        int catSeq = -1 ;
        ContentValues cv = new ContentValues() ;
        
        // Get the categories in the preferential order as they are defined
        // in the configuration.  
        String[] categories = context.getResources().getStringArray( 
                                                  R.array.expense_categories ) ;
        
        // Iterate through the categories and start inserting in the category
        // and the sub category table
        for( String catName : categories ) {

            // If we have a category in the resource file but not present 
            // in the static map, we ignore that category.
            if( !catSubCatIdMap.containsKey( catName ) ) {
                continue ;
            }
            
            String[] subCats = context.getResources().getStringArray( 
                                               catSubCatIdMap.get( catName ) ) ;
            
            // If this category has associated sub categories, we consider
            // it for insertion, else we just ignore.
            catSeq++ ;
            
            cv.clear() ;
            cv.put( CAT_TAB_NAME_COL_NM, catName ) ;
            cv.put( CAT_TAB_SEQNO_COL_NM, catSeq ) ;
            
            if( BuildConfig.DEBUG ) {
                Log.d( LogTag.DB_HELPER, "Inserting cateogry = " + catName ) ;
            }
            long catId = db.insert( CAT_TAB_NAME, null, cv ) ;
            
            int subCatSeq = -1 ;
            
            for( String subCat : subCats ) {
                
                subCatSeq++ ;
                
                cv.clear() ;
                cv.put( SUBCAT_TAB_CATID_COL_NM, catId ) ;
                cv.put( SUBCAT_TAB_NAME_COL_NM,  subCat ) ;
                cv.put( SUBCAT_TAB_SEQNO_COL_NM, subCatSeq ) ;
                
                if( BuildConfig.DEBUG ) {
                    Log.d( LogTag.DB_HELPER, 
                           "Inserting sub cateogry = " + subCat ) ;
                }
                db.insert( SUBCAT_TAB_NAME, null, cv ) ;
            }
        }
    }
}
