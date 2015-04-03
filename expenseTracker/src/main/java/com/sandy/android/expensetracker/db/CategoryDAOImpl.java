package com.sandy.android.expensetracker.db;

import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import android.content.Context ;
import android.database.Cursor ;
import android.database.SQLException ;
import android.database.sqlite.SQLiteDatabase ;
import android.database.sqlite.SQLiteStatement ;
import android.util.Log ;

import com.sandy.android.expensetracker.BuildConfig ;
import com.sandy.android.expensetracker.R ;
import com.sandy.android.expensetracker.util.LogTag ;

/**
 * A concrete implementation of CategoryDAO interface backed up the SQLite 
 * database.
 * 
 * @author Sandeep Deb
 */
class CategoryDAOImpl implements CategoryDAO {
    
    public static final String TABLE_CATEGORY_NAME    = "category" ;
    public static final String TABLE_SUBCATEGORY_NAME = "sub_category" ;
    public static final String COL_ID_NAME            = "_id" ;
    public static final String COL_SEQNO_NAME         = "sequence_no" ;
    public static final String COL_NAME_NAME          = "name" ;
    public static final String COL_CATID_NAME         = "cat_id" ;
    
    private SQLiteDatabase db = null ;
    private Context context = null ;
    
    private List<Integer>               categoryIdList       = new ArrayList<Integer>() ;
    private Map<Integer, String>        categoryIdNameMap    = new HashMap<Integer, String>() ;
    private Map<Integer, List<Integer>> catSubCatIdListMap   = new HashMap<Integer, List<Integer>>() ;
    private Map<Integer, String>        subCategoryIdNameMap = new HashMap<Integer, String>() ;
    
    /** Constructor. */
    public CategoryDAOImpl( Context context, SQLiteDatabase db ) {
        this.db = db ;
        this.context = context ;
        
        refreshDataCache() ;
    }
    
    /**
     * Loads the reference data for category and sub categories into memory
     * and stores them. This is the only in-memory cache that exists within
     * the application and is not accessible to the rest of the application in
     * the raw form.
     */
    private void refreshDataCache() {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.CATEGORY_DAO, "Refreshing data cache" ) ;
        }
        
        categoryIdList.clear() ;
        categoryIdNameMap.clear() ;
        catSubCatIdListMap.clear() ;
        subCategoryIdNameMap.clear() ;
        
        String sql = context.getString( R.string.query_select_all_categories ) ;
        
        Cursor c = db.rawQuery( sql, null ) ;
        c.moveToFirst() ;
        
        while( !c.isAfterLast() ) {
            
            int    catId = c.getInt( 0 ) ;
            String name  = c.getString( 1 ) ;
            
            categoryIdList.add( catId ) ;
            categoryIdNameMap.put( catId, name ) ;
            
            refreshSubCatCache( catId ) ;
            
            c.moveToNext() ;
        }
        
        c.close() ;
    }
    
    /** 
     * Given a category id, this method refreshes the complete sub category
     * information into the data cache.
     */
    private void refreshSubCatCache( int catId ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.CATEGORY_DAO, "Refreshing sub category for " +
                   " category " + categoryIdNameMap.get( catId ) ) ;
        }
        
        ArrayList<Integer> subCatIdList = new ArrayList<Integer>() ;
        
        String sql = context.getString( R.string.query_select_all_sub_categories ) ;
        sql = sql.replace( "?", Integer.toString( catId ) ) ;
        
        Cursor c = db.rawQuery( sql, null ) ;
        
        c.moveToFirst() ;
        
        while( !c.isAfterLast() ) {
            
            int    subCatId = c.getInt( 0 ) ;
            String name     = c.getString( 1 ) ;
            
            subCategoryIdNameMap.put( subCatId, name ) ;
            subCatIdList.add( subCatId ) ;
            
            c.moveToNext() ;
        }
        
        catSubCatIdListMap.put( catId, subCatIdList ) ;
        
        c.close() ;
    }

    /** 
     * Returns a list of identifiers of the categories in ascending order
     * of their sequence number. 
     */
    @Override
    public List<Integer> getCategoryIds() {
        return categoryIdList ;
    }

    /** Returns the name of the category given the category identifier. */
    @Override
    public String getCategoryName( int catId ) {
        return categoryIdNameMap.get( catId ) ;
    }

    /** 
     * Returns a list of identifiers of the sub categories in ascending order
     * of their sequence number. 
     */
    @Override
    public List<Integer> getSubCategoryIds( int catId ) {
        return catSubCatIdListMap.get( catId ) ;
    }

    /** Returns the name of the sub category given the sub category identifier. */
    @Override
    public String getSubCategoryName( int subCatId ) {
        return subCategoryIdNameMap.get( subCatId ) ;
    }
    
    /** Returns true if the given category name already exists. */
    @Override
    public boolean doesCategoryNameExist( String catName ) {
        return categoryIdNameMap.values().contains( catName ) ;
    }
    
    /** Returns the number of sub categories for the given category. */
    public int getNumSubCategories( int catId ) {
        
        int retVal = 0 ;
        List<Integer> subCatIdList = catSubCatIdListMap.get( catId ) ;
        if( subCatIdList != null ) {
            retVal = subCatIdList.size() ;
        }
        return retVal ;
    }
    
    /** Returns true if the given category name already exists. */
    @Override
    public boolean doesSubCategoryNameExist( int catId, String subCatName ) {
        
        boolean retVal = false ;
        List<Integer> subCatIds = catSubCatIdListMap.get( catId ) ;
        
        if( subCatIds != null && subCatIds.size() != 0 ) {
            
            for( Integer id : subCatIds ) {
                String existingSubCatName = subCategoryIdNameMap.get( id ) ;
                if( existingSubCatName.equals( subCatName ) ) {
                    retVal = true ;
                    break ;
                }
            }
        }
        
        return retVal ;
    }
    
    /**
     * Adds the given category name as a new category and returns the database
     * identifier for the new category. This method will return a -1 under
     * the following circumstances. Appropriate log messages will give further
     * insight into what went wrong.
     * 
     * a) The category name is either empty or null
     * b) The category name is already present in the database
     * c) Any database exception while adding
     * d) Any internal logic exceptions
     * 
     * In addition to adding a new category, this method will create a default
     * sub category for the newly created category (which can later be modified
     * by the user of course). This is to ensure that the category is created
     * with at least one sub category so that while creating an expense with
     * the newly created category, the sub category values don't show up as null.
     * The name of the default sub category would be "<catName>" - note the
     * angle brackets. 
     * 
     * This method also does appropriate internal data caching to ensure that
     * the cache is in sync with the database. 
     * 
     * @param catName The name of the category to add. 
     * 
     * @return A non negative integer denoting the database identifier of the 
     *         newly added category. -1 is returned in case the addition failed.
     */
    @Override
    public int addCategory( String catName ) {
        
        int id = -1 ;
        
        if( !doesCategoryNameExist( catName ) ) {
            
            try {
                String sql = context.getString( R.string.query_add_category ) ;
                
                SQLiteStatement stmt = db.compileStatement( sql ) ;
                stmt.bindString( 1, catName ) ;
                
                id = ( int )stmt.executeInsert() ;
                
                stmt.close() ;
            } 
            catch ( SQLException e ) {
                Log.e( LogTag.CATEGORY_DAO, "Exception while inserting category", e ) ;
            }
        }
        else {
            Log.e( LogTag.CATEGORY_DAO, "Could not add category. The name already exists" ) ;
        }
        
        // If we have successfully inserted the new category, refresh the 
        // internal cache.
        if( id != -1 ) {
            
            // Note that we are adding the category id to the end of the 
            // category id list. The category id list is sorted based on 
            // ascending order of the category sequence numbers. Since the 
            // insertion creates the category with 1 more than the max sequence
            // number, this logic is valid.
            categoryIdList.add( id ) ;
            categoryIdNameMap.put( id, catName ) ;
            catSubCatIdListMap.put( id, new ArrayList<Integer>() ) ;
            
            // Create the default sub category for the newly created category.
            addSubCategory( id, "<" + catName + ">" ) ;
        }
        
        return id ;
    }
    
    /**
     * Adds the given sub category name for the given category id, with the 
     * sequence number of the sub category the last in the line. 
     * 
     * This method will return a -1 under the following circumstances. 
     * Appropriate log messages will give further insight into what went wrong.
     * 
     * a) The sub-category name is either empty or null
     * b) The sub-category name is already present in the database
     * c) Any database exception while adding
     * d) Any internal logic exceptions
     * 
     * This method also does appropriate internal data caching to ensure that
     * the cache is in sync with the database. 
     * 
     * @param catId The identifier of the category to which the new sub 
     *        category is to be added. 
     * @param subCatName The name of the sub category to add. 
     * 
     * @return A non negative integer denoting the database identifier of the 
     *         newly added sub category. -1 is returned in case the addition failed.
     */
    @Override
    public int addSubCategory( int catId, String subCatName ) {
        
        int id = -1 ;
        
        if( !doesSubCategoryNameExist( catId, subCatName ) ) {
            
            try {
                String sql = context.getString( R.string.query_add_sub_category ) ;
                
                SQLiteStatement stmt = db.compileStatement( sql ) ;
                
                stmt.bindLong  ( 1, catId      ) ;
                stmt.bindString( 2, subCatName ) ;
                stmt.bindLong  ( 3, catId      ) ;
                stmt.bindLong  ( 4, catId      ) ;
                
                id = ( int )stmt.executeInsert() ;
                
                stmt.close() ;
            } 
            catch ( SQLException e ) {
                Log.e( LogTag.CATEGORY_DAO, "Exception while inserting sub category", e ) ;
            }
        }
        else {
            Log.e( LogTag.CATEGORY_DAO, "Could not add sub category. The name already exists" ) ;
        }
        
        // If we have successfully inserted the new category, refresh the 
        // internal cache.
        if( id != -1 ) {
            
            // Note that we are adding the sub category id to the end of the 
            // sub category id list. The sub category id list is sorted based on 
            // ascending order of the sub category sequence numbers. Since the 
            // insertion creates the sub category with 1 more than the max sequence
            // number, this logic is valid.
            List<Integer> subCatIdList = catSubCatIdListMap.get( catId ) ; 
            subCatIdList.add( id ) ;
            subCategoryIdNameMap.put( id, subCatName ) ;
        }
        
        return id ;
    }

    /**
     * Removes the provided category and associated sub categories from the
     * database and updates the internal cache accordingly.
     * 
     * It is assumed that no expense items are using the category provided - it
     * is important for the caller to verify this before invoking this method.
     * Depending upon database constraints, it might happen that any expense
     * items referring to this category might get cascade deleted.
     * 
     * @param catId The category to remove from the database.
     */
    @Override
    public void removeCategory( int catId ) {
        
        try {
            // First remove all sub categories for the given category
            removeSubCategoriesForCategory( catId ) ;
            
            // Now delete the category
            String sql = context.getString( R.string.query_delete_cat ) ;
            
            SQLiteStatement stmt = db.compileStatement( sql ) ;
            stmt.bindLong  ( 1, catId      ) ;
            stmt.executeUpdateDelete() ;
            stmt.close() ;
            
            // Update the category cache
            categoryIdList.remove( Integer.valueOf( catId ) ) ;
            categoryIdNameMap.remove( catId ) ;
        } 
        catch ( SQLException e ) {
            Log.e( LogTag.CATEGORY_DAO, "Exception while deleting category " + catId, e ) ;
        }
    }
    
    /**
     * Removes the provided sub category from the database and updates the
     * internal cache accordingly.
     * 
     * It is assumed that no expense items are using the sub category provided -
     * it is important for the caller to verify this before invoking this
     * method. Depending upon database constraints, it might happen that any
     * expense items referring to this sub category might get cascade deleted.
     * 
     * @param catId The category to which the sub category belongs. Although
     *        this is not required for the database operation, this makes 
     *        managing the internal cache easier.
     *        
     * @param subCatId The category to remove from the database.
     */
    @Override
    public void removeSubCategory( int catId, int subCatId ) {
        
        try {
            // Now delete the category
            String sql = context.getString( R.string.query_delete_subcat ) ;
            
            SQLiteStatement stmt = db.compileStatement( sql ) ;
            stmt.bindLong  ( 1, subCatId ) ;
            stmt.executeUpdateDelete() ;
            stmt.close() ;
            
            // Update the category cache
            subCategoryIdNameMap.remove( subCatId ) ;
            
            List<Integer> subCatList = catSubCatIdListMap.get( catId ) ;
            subCatList.remove( Integer.valueOf( subCatId ) ) ;
        } 
        catch ( SQLException e ) {
            Log.e( LogTag.CATEGORY_DAO, "Exception while deleting category " + subCatId, e ) ;
        }
    }

    /**
     * Deletes all the sub categories associated with a category.
     * 
     * @param catId The category id for which all sub categories need to be 
     *        removed.
     */
    private void removeSubCategoriesForCategory( int catId ) {
        
        try {
            String sql = context.getString( R.string.query_delete_all_subcats_for_cat ) ;
            
            SQLiteStatement stmt = db.compileStatement( sql ) ;
            stmt.bindLong  ( 1, catId      ) ;
            stmt.executeUpdateDelete() ;
            stmt.close() ;
            
            // Update the sub category cache
            List<Integer> subCatIdList = catSubCatIdListMap.get( catId ) ;
            catSubCatIdListMap.remove( catId ) ;
            
            if( subCatIdList != null ) {
                for( Integer subCatId : subCatIdList ) {
                    subCategoryIdNameMap.remove( subCatId ) ;
                }
            }
        } 
        catch ( SQLException e ) {
            Log.e( LogTag.CATEGORY_DAO, "Exception while deleting sub " +
            		                   "categories for category " + catId, e ) ;
        }
    }
    

    /** Change category sequence numbers. */
    @Override
    public void changeCategorySequence( Integer fromCatId,
                                        Integer toCatId, boolean fwd ) {
        
        changeItemSequence( fromCatId, toCatId, fwd, 
                            R.string.query_get_cat_seq_change_tupules, 
                            R.string.query_update_cat_sequence_no,
                            this.categoryIdList ) ;
    }

    /** Change sub category sequence numbers. */
    @Override
    public void changeSubCategorySequence( Integer catId, Integer fromSubCatId,
                                           Integer toSubCatId, boolean fwd ) {
        
        changeItemSequence( fromSubCatId, toSubCatId, fwd, 
                R.string.query_get_subcat_seq_change_tupules, 
                R.string.query_update_subcat_sequence_no,
                this.catSubCatIdListMap.get( catId ) ) ;
    }

    /**
     * This method changes the sequence order of the items (either category or
     * sub category). Specifically the item at 'from' index is moved in place of 
     * the item at 'to' index.
     * 
     * Case a)
     * changeSequence( 1, 4)
     * 
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i1  i2  i3  i4  i5  i6  i7   <- _id
     *      |          ^
     *      +----------+
     *      
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i2  i3  i4  i1  i5  i6  i7   <- _id
     *      
     *      
     * Case b)
     * changeSequence( 4, 1)
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i1  i2  i3  i4  i5  i6  i7   <- _id
     *      ^          |
     *      +----------+
     *      
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i4  i1  i2  i3  i5  i6  i7   <- _id
     *      
     * Important points to note:
     * a) Only the sequence numbers of items between the from and to index 
     *    get affected
     *    
     * b) from can be either greater than or less than to - either an item
     *    can be pulled up or pushed down
     *    
     * c) The sequence numbers need not be consecutive. Holes in sequence numbers
     *    can arise from remove operations.
     *    
     * d) _ids are random and unique and have no correlation with sequence number.
     *    
     * e) If direction is forward, it is guaranteed that the sequence number of
     *    toId is greater than the sequence number of fromId and 
     *    vice versa.
     */
    private void changeItemSequence( Integer fromId, Integer toId, 
                                     boolean fwd, int getSeqChgTupuleQueryId,
                                     int seqChgQueryId, 
                                     List<Integer> cacheList ) {

        List<int[]> chgTupules = null ;
        
        if( fwd ) {
            chgTupules = getSeqChangeTupules( fromId, toId, getSeqChgTupuleQueryId ) ;
        }
        else {
            chgTupules = getSeqChangeTupules( toId, fromId, getSeqChgTupuleQueryId ) ;
        }
        
        //  id   seq_no  (Change tupules)
        //  i1   2
        //  i2   6
        //  i3   8
        //  i4   9
        int firstSeqNo = chgTupules.get( 0 )[1] ;
        int lastSeqNo  = chgTupules.get( chgTupules.size()-1 )[1] ;
        
        if( fwd ) {
            //  id   seq_no  (Forward)
            //  i1   9
            //  i2   2
            //  i3   6
            //  i4   8
            for( int i=0; i<chgTupules.size(); i++ ) {
                if( i == 0 ) {
                    updateItemSequenceNo( chgTupules.get(i)[0], lastSeqNo, seqChgQueryId  ) ;
                }
                else {
                    updateItemSequenceNo( chgTupules.get(i)[0], chgTupules.get(i-1)[1], seqChgQueryId ) ;
                }
            }
        }
        else {
            //  id   seq_no  (Backward)
            //  i1   6
            //  i2   8
            //  i3   9
            //  i4   2
            for( int i=0; i<chgTupules.size(); i++ ) {
                if( i == (chgTupules.size()-1) ) {
                    updateItemSequenceNo( chgTupules.get(i)[0], firstSeqNo, seqChgQueryId ) ;
                }
                else {
                    updateItemSequenceNo( chgTupules.get(i)[0], chgTupules.get(i+1)[1], seqChgQueryId ) ;
                }
            }
        }
        
        // Now we change the internal cache sequence
        int fromIdPos = cacheList.indexOf( fromId ) ;
        int toIdPos   = cacheList.indexOf( toId ) ;
        
        cacheList.remove( fromIdPos ) ;
        cacheList.add( toIdPos, fromId ) ;
    }
    
    /** 
     * Returns a list of tupules (id, sequence_no) of categories which will 
     * be affected by the change sequence operation. It is guaranteed that the
     * list will have two or more tupules and the sequence number of those 
     * tupules will be in ascending order.
     * 
     * @param idA The category id whose sequence is smaller
     * @param idB The category id whose sequence is greater
     * @param queryId The resource id of the query which will return the tupule
     *        as mentioned in the documentation. The query should take two 
     *        parameters idA and idB and should return a result set of tupules
     *        ( id, sequence_no )
     * 
     */
    private List<int[]> getSeqChangeTupules( int idA, int idB, int queryId ) {

        List<int[]> retVal = new ArrayList<int[]>() ;
        
        String sql = context.getString( queryId ) ;
        sql = sql.replace( "#param1#", Integer.toString( idA ) ) ;
        sql = sql.replace( "#param2#", Integer.toString( idB ) ) ;
        
        Cursor c = db.rawQuery( sql, null ) ;
        
        c.moveToFirst() ;
        while( !c.isAfterLast() ) {
            
            int[] tupule = new int[2] ;
            tupule[0] = c.getInt( 0 ) ;
            tupule[1] = c.getInt( 1 ) ;
            
            retVal.add( tupule ) ;
            
            c.moveToNext() ;
        }
        c.close() ;
        
        return retVal ;
    }
    
    /**
     * Updates the sequence number of the given category to the specified 
     * sequence number.
     */
    private void updateItemSequenceNo( int catId, int seqNo, int seqChgQueryId ) {
        
        String sql = context.getString( seqChgQueryId ) ;
        SQLiteStatement stmt = db.compileStatement( sql ) ;
        stmt.bindLong( 1, seqNo ) ;
        stmt.bindLong( 2, catId ) ;
        stmt.executeUpdateDelete() ;
        stmt.close() ;
    }

    /** Updates the name of the category to the new name.. */
    @Override
    public void updateCatName( int catId, String newName ) {
        updateItemName( catId, newName, R.string.query_update_cat_name, categoryIdNameMap ) ;
    }

    /** Updates the name of the sub category to the new name.. */
    @Override
    public void updateSubCatName( int subCatId, String newName ) {
        updateItemName( subCatId, newName, R.string.query_update_subcat_name, subCategoryIdNameMap ) ;
    }
    
    /**
     * Private refactored method to update the name of either category or 
     * sub category.
     */
    private void updateItemName( int id, String newName, int updateNameQueryId, 
                                 Map<Integer, String> nameMap ) {
        
        // Update the database
        String sql = context.getString( updateNameQueryId ) ;
        SQLiteStatement stmt = db.compileStatement( sql ) ;
        stmt.bindString( 1, newName ) ;
        stmt.bindLong( 2, id ) ;
        stmt.executeUpdateDelete() ;
        stmt.close() ;
        
        // Update the internal cache.
        nameMap.put( id, newName ) ;
    }
}
