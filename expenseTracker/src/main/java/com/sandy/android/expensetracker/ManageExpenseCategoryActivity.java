package com.sandy.android.expensetracker ;

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
import android.widget.Button ;
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
 * This activity is used to manage the expense categories. This activity 
 * consists of the following interaction elements.
 * 
 * a) Drag sort list view which enables the user to 
 *    - delete and reorder expense categories
 *    - edit a name by long clicking on a row. This pops up a dialog which 
 *      enables the user to edit the name
 *      
 * b) An add category text view in which the user can enter a new name
 * 
 * c) A button besides the add category text box which when pressed adds the
 *    string in the add category text box as a new category
 * 
 * @author Sandeep Deb (deb.sandeep@gmail.com)
 */
public class ManageExpenseCategoryActivity extends Activity 
    implements OnClickListener, DropListener, RemoveListener, 
               OnItemLongClickListener, ModifyStringDialogFragmentListener {

    private DragSortListView listView;
    private ManageCategoryListAdapter adapter;
    private TextView addCatTextView ;
    
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
    
        try {
            setContentView( R.layout.activity_manage_expense_category ) ;
            
            adapter = new ManageCategoryListAdapter( this, R.layout.dslv_item_layout ) ;
            
            listView = ( DragSortListView )findViewById( R.id.manageCatListView ) ;
            listView.setLongClickable( true ) ;
            listView.setAdapter( adapter ) ;
            listView.setFocusable( true ) ;
            listView.requestFocus() ;
            listView.setDropListener( this ) ;
            listView.setRemoveListener( this ) ;
            listView.setOnItemLongClickListener( this ) ;
            
            Button b = ( Button )findViewById( R.id.addCatBtn ) ;
            b.setOnClickListener( this ) ;
            
            addCatTextView = ( TextView )findViewById( R.id.addCatTextArea ) ;
        } 
        catch (Exception e) {
            Log.e( "ManageExpenseCategoryActivity", "Could not create activity", e ) ;
        }
    }

    /**
     * This method is called when the add button is pressed by the user. We
     * pick up the text in the add category text view and depending upon it's
     * validity ask the adapter to add it.
     */
    @Override
    public void onClick( View v ) {
        
        if( v.getId() == R.id.addCatBtn ) {
            String text = addCatTextView.getText().toString() ;
            if( text != null && !text.trim().equals( "" ) ) {
                
                // Note that the adapter might do further checks to determine
                // if the request is valid for addition of a new category. If
                // not, the adapter will return an appropriate message as to
                // why the category was not added.
                
                // The adapter will notify the view for refresh, so we don't 
                // have to worry about it.
                String msg = adapter.add( text ) ;
                
                if( msg != null ) {
                    String preamble = "Category not added : " ;
                    Toast.makeText( this, preamble + msg, Toast.LENGTH_SHORT ).show() ;
                }
            }
            
            addCatTextView.setText( "" ) ;
            listView.requestFocus() ;
            InputMethodManager imm = ( InputMethodManager )getSystemService( Context.INPUT_METHOD_SERVICE ) ;
            imm.hideSoftInputFromWindow( addCatTextView.getWindowToken(),  0 ) ;
        }
    }

    /**
     * This method is called when the user long clicks on any particular 
     * category in the category list. In this case, we pop up a dialog in which
     * the user can edit the category name and upon confirmation, the new 
     * name is updated in the database.
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

        // Get the name of the category being clicked upon.
        Integer catId = ( Integer )listView.getItemAtPosition( position ) ;
        String catName = catDAO.getCategoryName( catId ) ;

        // Prepare the dialog and show. The dialog will call back stringModified
        // method when the user has entered a valid input. 
        DialogFragment newFragment = new ModifyStringDialogFragment() ;
        
        Bundle bundle = new Bundle() ;
        bundle.putInt   ( "id",        catId ) ;
        bundle.putString( "inputText", catName  ) ;
        
        newFragment.setArguments( bundle ) ;
        newFragment.show( getFragmentManager(), "modifyText" ) ;
        
        return false ;
    }
    
    /**
     * This method is called when the user has reordered the list items, moving
     * the row at 'from' index to 'to' index. For us this translates to
     * changing the sequence number of the categories. We delegate the 
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
     * In this method we check if the category referred to by the rowId has
     * any associated expense items. If so this category can't be removed.
     * 
     * This method returns true only if there are no expense items which refer
     * to this category.
     */
    @Override
    public boolean canRemove( int rowId ) {
        
        int catId = adapter.getItem( rowId ) ;
        ExpenseItemDAO expItemDAO = DAOManager.getInstance().getExpenseItemDAO() ;
        
        boolean canRemove = !expItemDAO.isCategoryUsed( catId ) ; 
        
        if( !canRemove ) {
            DialogUtils.showMsgDialog( this, R.string.msg_cant_remove_cat ) ;
        }
        
        return canRemove ;
    }
    
    /**
     * This method is called when the user has shown intention to remove a 
     * particular category and there are no expense items using this category.
     * 
     * @param rowId The row number of the list view which needs to be removed.
     */
    public void remove( int rowId ) {
        
        int catId = adapter.getItem( rowId ) ;
        adapter.remove( catId ) ;
    }

    /**
     * This method is called when the user has finished modifying the category
     * text. We ask the adapter to take care of the processing and notifying the
     * views displaying the category string.
     */
    @Override
    public void stringModified( int id, String modifiedText ) {
        adapter.changeCatName( id, modifiedText ) ;
    }
}
