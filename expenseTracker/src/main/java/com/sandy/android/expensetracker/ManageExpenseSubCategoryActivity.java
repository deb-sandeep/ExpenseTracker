package com.sandy.android.expensetracker ;

import java.util.List ;

import android.app.Activity ;
import android.app.DialogFragment ;
import android.content.Context ;
import android.os.Bundle ;
import android.util.Log ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.view.inputmethod.InputMethodManager ;
import android.widget.AdapterView ;
import android.widget.AdapterView.OnItemLongClickListener ;
import android.widget.AdapterView.OnItemSelectedListener ;
import android.widget.Button ;
import android.widget.Spinner ;
import android.widget.TextView ;
import android.widget.Toast ;

import com.mobeta.android.dslv.DragSortListView ;
import com.mobeta.android.dslv.DragSortListView.DropListener ;
import com.mobeta.android.dslv.DragSortListView.RemoveListener ;
import com.sandy.android.expensetracker.ModifyStringDialogFragment.ModifyStringDialogFragmentListener ;
import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.db.ExpenseItemDAO ;
import com.sandy.android.expensetracker.util.DialogUtils ;

/**
 * This activity is used to manage the expense sub categories. This activity 
 * consists of the following interaction elements.
 * 
 * a) A spinner to select the category for which the sub categories will be 
 *    managed
 *    
 * b) Drag sort list view which enables the user to 
 *    - delete and reorder expense sub categories
 *    - edit a name by long clicking on a row. This pops up a dialog which 
 *      enables the user to edit the name
 *      
 * c) An add sub category text view in which the user can enter a new name
 * 
 * d) A button besides the add sub category text box which when pressed adds the
 *    string in the add sub category text box as a new sub category
 * 
 * @author Sandeep Deb (deb.sandeep@gmail.com)
 */
public class ManageExpenseSubCategoryActivity extends Activity 
    implements OnClickListener, DropListener, RemoveListener, 
               OnItemLongClickListener, ModifyStringDialogFragmentListener,
               OnItemSelectedListener {

    private TextView         addSubCatTextView ;
    private Spinner          catSpinner ;
    private DragSortListView listView ;
    
    private ManageSubCategoryListAdapter adapter ;
    private CatSubCatItemListAdapter     catSpinnerAdapter ;
    
    /* ====================================================================== */
    // Call back methods
    /* ====================================================================== */
    /**
     * This function is called during the creation of this activity. We 
     * capture references of views, link listeners to views and enhance views
     * with special adapters
     */
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState ) ;
    
        CategoryDAO catDAO = DAOManager.getInstance().getCategoryDAO() ;
        
        try {
            setContentView( R.layout.activity_manage_expense_subcategory ) ;
            
            // Prepare the category spinner by creating and setting the adapter
            // and setting the selection listener
            catSpinner = ( Spinner  ) findViewById( R.id.categorySpinner    ) ;
            List<Integer> catIds = catDAO.getCategoryIds() ;
            
            catSpinnerAdapter = new CatSubCatItemListAdapter( this, catIds, 
                                       CatSubCatItemListAdapter.CAT_LIST_ADAPTER ) ;
            catSpinner.setAdapter( catSpinnerAdapter ) ;
            catSpinner.setOnItemSelectedListener( this ) ;
            
            // Get a reference to the text view
            addSubCatTextView = ( TextView )findViewById( R.id.addSubCatTextArea ) ;
            

            // Create the sub category adapter and prepare the drag sort list.
            adapter = new ManageSubCategoryListAdapter( this, R.layout.dslv_item_layout ) ;
            
            listView = ( DragSortListView )findViewById( R.id.manageSubCatListView ) ;
            listView.setLongClickable( true ) ;
            listView.setAdapter( adapter ) ;
            listView.setFocusable( true ) ;
            listView.setDropListener( this ) ;
            listView.setRemoveListener( this ) ;
            listView.setOnItemLongClickListener( this ) ;
            
            // Get a reference to the add sub category button and attach the 
            // click listener.
            Button b = ( Button )findViewById( R.id.addSubCatBtn ) ;
            b.setOnClickListener( this ) ;
            
            // Force select the first item in the category spinner. This will 
            // cause the sub categories to be populated in the DSLV for this 
            // category.
            catSpinner.setSelection( 0 ) ;
            
            listView.requestFocus() ;
        } 
        catch (Exception e) {
            Log.e( "ManageExpenseSubCategoryActivity", "Could not create activity", e ) ;
        }
    }

    /**
     * This method is called when the add button is pressed by the user. We
     * pick up the text in the add sub category text view and depending upon it's
     * validity ask the adapter to add it.
     */
    @Override
    public void onClick( View v ) {
        
        if( v.getId() == R.id.addSubCatBtn ) {
            String text = addSubCatTextView.getText().toString() ;
            if( text != null && !text.trim().equals( "" ) ) {
                
                // Note that the adapter might do further checks to determine
                // if the request is valid for addition of a new category. If
                // not, the adapter will return an appropriate message as to
                // why the category was not added.
                
                // The adapter will notify the view for refresh, so we don't 
                // have to worry about it.
                String msg = adapter.add( text ) ;
                
                if( msg != null ) {
                    String preamble = "Sub-Category not added : " ;
                    Toast.makeText( this, preamble + msg, Toast.LENGTH_SHORT ).show() ;
                }
            }
            
            addSubCatTextView.setText( "" ) ;
            listView.requestFocus() ;
            InputMethodManager imm = ( InputMethodManager )getSystemService( Context.INPUT_METHOD_SERVICE ) ;
            imm.hideSoftInputFromWindow( addSubCatTextView.getWindowToken(),  0 ) ;
        }
    }

    /**
     * This method is called when the user long clicks on any particular 
     * sub category in the sub category list. In this case, we pop up a dialog 
     * in which the user can edit the sub category name and upon confirmation, 
     * the new name is updated in the database.
     * 
     * A error message is shown to the user in the following cases:
     * a) Any technical error
     * b) If the newly edited name is empty
     * c) If the modified name already exists in the database
     * 
     * @return This function returns a true, implying that it has consumed the
     *         event and the event should not be forward to other listeners.
     *         Hmm.. think this through! 
     */
    @Override
    public boolean onItemLongClick( AdapterView<?> parent, View view,
                                    int position, long id ) {
        
        CategoryDAO catDAO = DAOManager.getInstance().getCategoryDAO() ;

        // Get the name of the sub category being clicked upon.
        Integer subCatId = ( Integer )listView.getItemAtPosition( position ) ;
        String subCatName = catDAO.getSubCategoryName( subCatId ) ;

        // Prepare the dialog and show. The dialog will call back stringModified
        // method when the user has entered a valid input. 
        DialogFragment newFragment = new ModifyStringDialogFragment() ;
        
        Bundle bundle = new Bundle() ;
        bundle.putInt   ( "id",        subCatId ) ;
        bundle.putString( "inputText", subCatName  ) ;
        
        newFragment.setArguments( bundle ) ;
        newFragment.show( getFragmentManager(), "modifyText" ) ;
        
        return false ;
    }
    
    /**
     * This method is called when the user has reordered the list items, moving
     * the row at 'from' index to 'to' index. For us this translates to
     * changing the sequence number of the sub categories. We delegate the 
     * sequence change processing to the adapter.
     */
    public void drop( int from, int to ) {
        
        if( from != to ) {
            adapter.changeSequence( from, to ) ;
        }
    }

    /**
     * This method is called prior to calling the {@link #remove(int)} method
     * by the DSLV when the user has shown intent to remove a particular row.
     * The remove method will be call only if this method returns a true.
     * 
     * In this method we check if the sub category referred to by the rowId has
     * any associated expense items or if this is the only sub category for
     * the given category. If so this category can't be removed.
     * 
     * This method returns true only if there are no expense items which refer
     * to this category.
     */
    @Override
    public boolean canRemove( int rowId ) {
        
        int subCatId = adapter.getItem( rowId ) ;
        int catId    = ( Integer )catSpinner.getSelectedItem() ;
        
        ExpenseItemDAO expItemDAO = DAOManager.getInstance().getExpenseItemDAO() ;
        CategoryDAO    catDAO     = DAOManager.getInstance().getCategoryDAO() ;
        
        boolean canRemove = !expItemDAO.isSubCategoryUsed( subCatId ) && 
                            ( catDAO.getNumSubCategories( catId ) > 1 ) ; 
        
        if( !canRemove ) {
            DialogUtils.showMsgDialog( this, R.string.msg_cant_remove_subcat ) ;
        }
        
        return canRemove ;
    }
    
    /**
     * This method is called when the user has shown intention to remove a 
     * particular sub category and there are no expense items using this 
     * sub category.
     * 
     * @param rowId The row number of the list view which needs to be removed.
     */
    public void remove( int rowId ) {
        
        int subCatId = adapter.getItem( rowId ) ;
        adapter.remove( subCatId ) ;
    }

    /**
     * This method is called when the user has finished modifying the sub category
     * text. We ask the adapter to take care of the processing and notifying the
     * views displaying the sub category string.
     */
    @Override
    public void stringModified( int id, String modifiedText ) {
        adapter.changeSubCatName( id, modifiedText ) ;
    }

    /**
     * This method is called when the a new category is selected in the category
     * spinner. We refresh the DSLV adapter with the new category id.
     */
    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int position,
                                long id ) {
        
        // Call comes here if an expense category item is selected
        int selCatId = catSpinnerAdapter.getItem( position ) ;
        
        // Refresh the sub category adapter with th selected category id
        adapter.refreshSubCategories( selCatId ) ;
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {
        // DO NOTHING
    }
}
