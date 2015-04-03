package com.sandy.android.expensetracker.db;

import java.util.List ;

/**
 * This interface defines the contract for the management of Category and 
 * SubCategory items. 
 * 
 * This interface has been designed so stringently to ensure that the users of
 * this interface are motivated to call upon the methods in this interface
 * for rendering the names at the last moment and not cache the values. The
 * names and sequence of categories and sub categories are liable to be 
 * changed by the user and caching would make the data stale.
 * 
 * The implementors of this class are encouraged to build in-memory optimization
 * algorithms to ensure that all calls don't result in database access.
 *  
 * @author Sandeep Deb
 */
public interface CategoryDAO {
    
    /** 
     * Returns a list of identifiers of the categories in ascending order
     * of their sequence number. 
     */
    public List<Integer> getCategoryIds() ;
    
    /** 
     * Returns a list of identifiers of the sub categories in ascending order
     * of their sequence number. 
     */
    public List<Integer> getSubCategoryIds( int catId ) ;
    
    /** Returns the name of the category given the category identifier. */
    public String getCategoryName( int catId ) ;
    
    /** Returns the name of the sub category given the sub category identifier. */
    public String getSubCategoryName( int subCatId ) ;
    
    /** Returns true if the given category name already exists. */
    public boolean doesCategoryNameExist( String catName ) ;
    
    /** Returns true if the given sub category name already exists for the given category. */
    public boolean doesSubCategoryNameExist( int catId, String subCatName ) ;
    
    /** Returns the number of sub categories for the given category. */
    public int getNumSubCategories( int catId ) ;
    
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
     * @param catName The name of the category to add. 
     * 
     * @return A non negative integer denoting the database identifier of the 
     *         newly added category. -1 is returned in case the addition failed.
     */
    public int addCategory( String catName ) ;

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
    public int addSubCategory( int catId, String subCatName ) ;

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
    public void removeCategory( int catId ) ;
    
    /**
     * Removes the provided sub category from the database and updates the
     * internal cache accordingly.
     * 
     * It is assumed that no expense items are using the sub category provided -
     * it is important for the caller to verify this before invoking this
     * method. Depending upon database constraints, it might happen that any
     * expense items referring to this sub category might get cascade deleted.
     * 
     * @param subCatId The category to remove from the database.
     */
    public void removeSubCategory( int catId, int subCatId ) ;
    
    /**
     * This method changes the sequence order of the categories. Specifically
     * the category at 'from' index is moved in place of the category at 'to'
     * index.
     * 
     * Case a)
     * changeSequence( 1, 4)
     * 
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i1  i2  i3  i4  i5  i6  i7   <- category id
     *      |          ^
     *      +----------+
     *      
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i2  i3  i4  i1  i5  i6  i7   <- category id
     *      
     *      
     * Case b)
     * changeSequence( 4, 1)
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i1  i2  i3  i4  i5  i6  i7   <- category id
     *      ^          |
     *      +----------+
     *      
     * 0   2   6    8   9  10  11  12   <- sequence_no
     * i0  i4  i1  i2  i3  i5  i6  i7   <- category id
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
     * d) category ids are random and unique and have no correlation with 
     *    sequence number.
     *    
     * e) If direction is forward, it is guaranteed that the sequence number of
     *    toCatId is greater than the sequence number of fromCatId and 
     *    vice versa.
     *      
     * @param fromCatId The category id which has to be moved
     * @param toCatId   The category whose place will be taken by the fromCatId
     * @param dir       true if the sequence of toCatId is greater than the 
     *                  sequence of fromCatId, false otherwise.
     */
    public void changeCategorySequence( Integer fromCatId, Integer toCatId, boolean fwd ) ;

    /**
     * This method changes the sequence order of the sub categories. Specifically
     * the sub category at 'from' index is moved in place of the category at 'to'
     * index.
     * 
     * Please see the documentation of {@link #changeCategorySequence(Integer, Integer, boolean)}
     * for more details.
     * 
     * @param fromSubCatId The sub category id which has to be moved
     * @param toSubCatId   The sub category whose place will be taken by the fromSubCatId
     * @param dir          true if the sequence of toSubCatId is greater than the 
     *                     sequence of fromSubCatId, false otherwise.
     */
    public void changeSubCategorySequence( Integer catId, Integer fromSubCatId, Integer toSubCatId, boolean fwd ) ;

    /** Updates the name of the category to the new name.. */
    public void updateCatName( int catId, String newName ) ;
    
    /** Updates the name of the sub category to the new name.. */
    public void updateSubCatName( int subCatId, String newName ) ;
}
