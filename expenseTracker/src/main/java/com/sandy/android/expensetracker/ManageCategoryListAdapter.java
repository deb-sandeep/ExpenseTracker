package com.sandy.android.expensetracker ;

import android.app.Activity ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.ArrayAdapter ;
import android.widget.ImageView ;
import android.widget.TextView ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.db.ExpenseItemDAO ;

/**
 * Specialized list adapter for displaying the categories in the manage
 * category activity list view.
 * 
 * @author Sandeep Deb
 */
public class ManageCategoryListAdapter extends ArrayAdapter<Integer> {

    private CategoryDAO    catDAO     = null ;
    private Activity       activity   = null ;

    public ManageCategoryListAdapter( Activity context, int resource )
            throws Exception {

        super( context, resource ) ;
        this.activity = context ;
        this.catDAO = DAOManager.getInstance().getCategoryDAO() ;
        super.addAll( this.catDAO.getCategoryIds() ) ;
    }

    /**
     * Returns the view required to display a particular list item as referred
     * to by the position. This method translates the internally maintained
     * category identifiers into their displayable format.
     */
    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {

        View        rv    = convertView ;
        Integer     catId = super.getItem( position ) ;
        
        // Inflate a row view if and only if we can't reuse an existing one.
        // Remember, inflating a row view and finding views in the layout is
        // a very costly operation which slows down the UI if the list grows
        // big.
        if (rv == null) {
            
            LayoutInflater inflater = activity.getLayoutInflater() ;
            rv = inflater.inflate( R.layout.dslv_item_layout, null ) ;
        }

        // Translate the category identifier into its display name
        String catName = catDAO.getCategoryName( catId ) ;
        if( catName == null ) catName = "<Unknown Category>" ;
        
        // Set the display name into the row view
        TextView tv = ( TextView )rv.findViewById( R.id.dslv_item_text ) ;
        tv.setText( catName ) ;
        
        // Depending upon whether the category is in use or not, display
        // the remove handle accordingly.
        ImageView iv = ( ImageView )rv.findViewById( R.id.dslv_remove_handle ) ;
        ExpenseItemDAO dao = DAOManager.getInstance().getExpenseItemDAO() ;
        
        if( dao.isCategoryUsed( catId ) ) {
            iv.setBackgroundResource( R.drawable.ic_action_discard_negative ) ;
        }
        else {
            iv.setBackgroundResource( R.drawable.ic_action_discard ) ;
        }
        
        return rv ;
    }
    
    /**
     * This method is called by the activity when the user clicks the add
     * category button.
     *  
     * First we check if a category of the given name already exists,
     * if not, we ask the category DAO to add the new category, get its
     * database id and add it to the base adapter. This will trigger a 
     * notification and in turn will be visible in the view.
     * 
     * @param newCategoryName The name of the new category to be added. It is
     *        assumed that the new category name is not empty or null.
     * 
     * @return A string indicating the reason why the new category was not 
     *         added. A null return value implies that the addition was 
     *         successful.
     */
    public String add( String newCategoryName ) {
        
        String msg = null ;

        if( !catDAO.doesCategoryNameExist( newCategoryName ) ) {
            
            // Ask the DAO to add a new category by the given name and return
            // the identifier
            int id = catDAO.addCategory( newCategoryName ) ;
            
            // Add the identifier to the base adapter.
            if( id != -1 ) {
                super.add( id ) ;
            }
            else {
                msg = "Exception while adding category to database" ;
            }
        }
        else {
            msg = "Category '" + newCategoryName + "' already exists." ;
        }
        
        return msg ;
    }
    
    /**
     * Overridden implementation of remove method. Here we ask the DAO to 
     * remove the category (and associated sub categories) before removing the 
     * item from the adapter.
     */
    public void remove( Integer catId ) {
        
        catDAO.removeCategory( catId ) ;
        super.remove( catId ) ;
    }
    
    /**
     * This method changes the sequence order of the categories. Specifically
     * the category at 'from' index is moved in place of the category at 'to'
     * index.
     * 
     * It converts the indexes to their category id and asks the adapter to 
     * change the sequence in the database. Following which, it updates the 
     * internal cache sequence which is just a remove, insert operation of 
     * the category list.
     */
    public void changeSequence( int fromIndex, int toIndex ) {
        
        // Pick up the category id and sequence number tupules of only the 
        // items affected.
        Integer fromCatId = super.getItem( fromIndex ) ;
        Integer toCatId   = super.getItem( toIndex ) ;
        boolean fwd = ( toIndex > fromIndex ) ? true : false ;
        
        // Change the sequence in the database
        catDAO.changeCategorySequence( fromCatId, toCatId, fwd ) ;
        
        // Change the sequence in the cache
        /*
         * ---------------------------------------------------------------------
         * Case a)
         * changeSequence( 1, 4)
         * 
         * 0   1   2    3   4   5   6   7   <- Index value
         * i0  i1  i2  i3  i4  i5  i6  i7   <- category id
         *      |          ^
         *      +----------+
         *      
         * remove( fromCatId )      
         * 0   1    2   3   4   5   6       <- Index value
         * i0  i2  i3  i4  i5  i6  i7       <- category id
         *      
         * insert( fromCatId, toIndex )
         * 0   1   2    3   4   5   6   7   <- Index value
         * i0  i2  i3  i4  i1  i5  i6  i7   <- category id
         *      
         * ---------------------------------------------------------------------
         * Case b)
         * changeSequence( 4, 1)
         * 0   1   2    3   4   5   6   7   <- Index value
         * i0  i1  i2  i3  i4  i5  i6  i7   <- category id
         *      ^          |
         *      +----------+
         *      
         * remove( fromCatId )      
         * 0   1   2    3   4   5   6       <- Index value
         * i0  i1  i2  i3  i5  i6  i7       <- category id
         *      
         * insert( fromCatId, toIndex )
         * 0   1   2    3   4   5   6   7   <- Index value
         * i0  i4  i1  i2  i3  i5  i6  i7   <- category id
         *      
         */
        super.remove( fromCatId ) ;
        super.insert( fromCatId, toIndex ) ;
    }
    
    /**
     * Modify the name of the category to the new name and update any internal
     * caches appropriately.
     */
    public void changeCatName( int catId, String newName ) {
        
        catDAO.updateCatName( catId, newName ) ;
        
        // Notify the list view listeners for change in data. So that they can
        // render the changed category name.
        notifyDataSetChanged() ;
    }
}
