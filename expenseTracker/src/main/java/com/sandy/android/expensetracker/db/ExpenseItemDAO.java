package com.sandy.android.expensetracker.db;

import java.util.List ;

import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * This interface defines the contract for the ExpenseItem data access object.
 * To facilitate unit testing and mocking, this has been designed as an interface.
 *  
 * @author Sandeep Deb
 */
public interface ExpenseItemDAO {

    /**
     * Creates a new expense item in the database and returns the same instance
     * back with the unique identifier populated.
     * 
     * @param item The expense item to insert. Note that the expense item is
     *        inserted if and only if the identified is -1. If not, an exception
     *        is raised.
     *        
     * @return The same instance of expense item with the unique database 
     *         identifier populated. 
     */
    public ExpenseItem create( ExpenseItem item ) throws IllegalArgumentException ;
    
    /**
     * Updates all the column data for the expense item in the database.
     * 
     * @param item The expense item to update. Note that the expense item is
     *        updated if and only if the identified is not -1. If not, an 
     *        exception is raised.
     *        
     * @return true if the update was successful, false otherwise 
     */
    public boolean update( ExpenseItem item ) throws IllegalArgumentException ;
    
    /**
     * Delete from database the expense item referred to by the input.
     * 
     * @param The expense item instance to delete from the database. If the 
     *        expense item does not have a valid identifier, an 
     *        {@link IllegalArgumentException} will be raised.
     *        
     * @return true if the item was successfully deleted, false otherwise.
     */
    public boolean delete( ExpenseItem item ) ;
    
    /**
     * Deletes all the data in the database.
     */
    public void deleteAll() ;
    
    /**
     * Returns a collection of all expense items in the database which have 
     * not been exported.
     * 
     * @return A collection of {@link ExpenseItem} instances.
     */
    public List<ExpenseItem> getAllExpenseItems() ;

    /**
     * This method returns a true if and only if there are one or more expense
     * items which refer to the supplied category id.
     */
    public boolean isCategoryUsed( int catId ) ;

    /**
     * This method returns a true if and only if there are one or more expense
     * items which refer to the supplied sub category id.
     */
    public boolean isSubCategoryUsed( int subCatId ) ;
}