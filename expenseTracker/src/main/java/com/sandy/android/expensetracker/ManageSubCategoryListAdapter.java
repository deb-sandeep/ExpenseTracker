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
 * Specialized list adapter for displaying the sub categories in the manage
 * sub category activity list view.
 * 
 * @author Sandeep Deb
 */
public class ManageSubCategoryListAdapter extends ArrayAdapter<Integer> {

    private CategoryDAO catDAO   = null ;
    private Activity    activity = null ;
    private Integer     catId    = null ;

    public ManageSubCategoryListAdapter( Activity context, int resource )
            throws Exception {

        super( context, resource ) ;
        this.activity = context ;
        this.catDAO = DAOManager.getInstance().getCategoryDAO() ;
    }
    
    /**
     * This function primes this adapters with the sub categories belonging to
     * the specified category. All the existing data is removed and is refreshed
     * with new data.
     * 
     * @param catId The category whose sub categories needs to be served by 
     *        this adapter.
     */
    public void refreshSubCategories( Integer catId ) {
        
        this.catId = catId ;
        super.clear() ;
        super.addAll( this.catDAO.getSubCategoryIds( catId ) ) ;
    }

    /**
     * Returns the view required to display a particular list item as referred
     * to by the position. This method translates the internally maintained
     * identifiers into their displayable format.
     */
    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {

        View    rv = convertView ;
        Integer id = super.getItem( position ) ;
        
        // Inflate a row view if and only if we can't reuse an existing one.
        // Remember, inflating a row view and finding views in the layout is
        // a very costly operation which slows down the UI if the list grows
        // big.
        if (rv == null) {
            
            LayoutInflater inflater = activity.getLayoutInflater() ;
            rv = inflater.inflate( R.layout.dslv_item_layout, null ) ;
        }

        // Translate the category identifier into its display name
        String itemName = catDAO.getSubCategoryName( id ) ;
        if( itemName == null ) itemName = "<Unknown Sub-Category>" ;
        
        // Set the display name into the row view
        TextView tv = ( TextView )rv.findViewById( R.id.dslv_item_text ) ;
        tv.setText( itemName ) ;
        
        // Depending upon whether the category is in use or not, display
        // the remove handle accordingly.
        ImageView iv = ( ImageView )rv.findViewById( R.id.dslv_remove_handle ) ;
        ExpenseItemDAO dao = DAOManager.getInstance().getExpenseItemDAO() ;
        
        // Set appropriate remove icon for each row. If the sub category 
        // has associated expense items or if it is the only sub category, 
        // we lighten the remove icon, indicating that these items are not 
        // for removal.
        if( dao.isSubCategoryUsed( id ) || catDAO.getNumSubCategories( catId ) <= 1 ) {
            iv.setBackgroundResource( R.drawable.ic_action_discard_negative ) ;
        }
        else {
            iv.setBackgroundResource( R.drawable.ic_action_discard ) ;
        }
        
        return rv ;
    }
    
    /**
     * This method is called by the activity when the user clicks the add
     * sub category button.
     *  
     * First we check if a sub category of the given name already exists for
     * the category we are serving, if not, we ask the category DAO to add the 
     * new sub category, get its database id and add it to the base adapter. 
     * This will trigger a notification and in turn will be visible in the view.
     * 
     * @param newSubCategoryName The name of the new sub category to be added. 
     *        It is assumed that the new sub category name is not empty or null.
     * 
     * @return A string indicating the reason why the new sub category was not 
     *         added. A null return value implies that the addition was 
     *         successful.
     */
    public String add( String newSubCategoryName ) {
        
        String msg = null ;

        if( !catDAO.doesSubCategoryNameExist( catId, newSubCategoryName ) ) {
            
            // Ask the DAO to add a new category by the given name and return
            // the identifier
            int id = catDAO.addSubCategory( catId, newSubCategoryName ) ;
            
            // Add the identifier to the base adapter.
            if( id != -1 ) {
                super.add( id ) ;
            }
            else {
                msg = "Exception while adding sub category to database" ;
            }
        }
        else {
            msg = "Sub-Category '" + newSubCategoryName + "' already exists." ;
        }
        
        return msg ;
    }
    
    /**
     * Overridden implementation of remove method. Here we ask the DAO to 
     * remove the sub category before removing the item from the adapter.
     */
    public void remove( Integer subCatId ) {
        
        catDAO.removeSubCategory( catId, subCatId ) ;
        super.remove( subCatId ) ;
    }
    
    /**
     * This method changes the sequence order of the sub categories. Specifically
     * the category at 'from' index is moved in place of the category at 'to'
     * index.
     * 
     * It converts the indexes to their sub category id and asks the adapter to 
     * change the sequence in the database. Following which, it updates the 
     * internal cache sequence which is just a remove, insert operation of 
     * the sub category list associated with the category we are serving.
     */
    public void changeSequence( int fromIndex, int toIndex ) {
        
        // Pick up the category id and sequence number tupules of only the 
        // items affected.
        Integer fromSubCatId = super.getItem( fromIndex ) ;
        Integer toSubCatId   = super.getItem( toIndex ) ;
        boolean fwd = ( toIndex > fromIndex ) ? true : false ;
        
        // Change the sequence in the database
        catDAO.changeSubCategorySequence( catId, fromSubCatId, toSubCatId, fwd ) ;
        
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
        super.remove( fromSubCatId ) ;
        super.insert( fromSubCatId, toIndex ) ;
    }
    
    /**
     * Modify the name of the sub category to the new name and update any internal
     * caches appropriately.
     */
    public void changeSubCatName( int subCatId, String newName ) {
        
        catDAO.updateSubCatName( subCatId, newName ) ;
        
        // Notify the list view listeners for change in data. So that they can
        // render the changed category name.
        notifyDataSetChanged() ;
    }
}
