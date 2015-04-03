package com.sandy.android.expensetracker;

import java.util.Date ;
import java.util.List ;

import android.app.Activity ;
import android.app.AlertDialog ;
import android.app.AlertDialog.Builder ;
import android.app.DialogFragment ;
import android.content.DialogInterface ;
import android.content.Intent ;
import android.os.Bundle ;
import android.util.Log ;
import android.view.ContextMenu ;
import android.view.ContextMenu.ContextMenuInfo ;
import android.view.Menu ;
import android.view.MenuItem ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.widget.AdapterView ;
import android.widget.AdapterView.AdapterContextMenuInfo ;
import android.widget.AdapterView.OnItemClickListener ;
import android.widget.Button ;
import android.widget.ListView ;
import android.widget.Toast ;

import com.sandy.android.expensetracker.AddAmtDialogFragment.AddAmtDialogFragmentListener ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.db.ExpenseItemDAO ;
import com.sandy.android.expensetracker.util.ExpenseCSVExporter ;
import com.sandy.android.expensetracker.util.LogTag ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;
import com.sandy.android.expensetracker.vo.ExpenseItem.ExpenseItemComparator ;

/**
 * This class is the main activity of the Expense Tracker application. This
 * activity contains the following views and associated actions.
 * 
 * ActionBar
 * -----------------------------------------------------------------------------
 * The action bar contains the following menu items
 * a) Export - Clicking on this menu item, exports all the expense items, which 
 *    have not been exported yet, into a CSV file
 *    
 * b) Report
 * 
 * ListView
 * -----------------------------------------------------------------------------
 * The list view is populated with all the expenses in the database, which has
 * not been exported. The view is sorted in the order of the expense date (most
 * recent first) followed by the expense item creation time.
 * 
 * Each item in the expense list view displays the expense date, category, 
 * sub category, description and the expense amount.
 * 
 * The list items support the following operations:
 * a) Simple click of any list item - leads to edit expense activity where the 
 *    expense item can be updated. 
 *    
 * b) Long click of any list item - shows a context menu, providing the user 
 *    with the following context options
 *    
 *    b.1) Add amount - Opens up a dialog in which the user can enter a signed
 *         amount to be added to the selected expense item
 *    b.2) Clone - Clones the selected expense item and opens up the edit 
 *         expense dialog
 *    b.3) Delete - Deletes the currently selected expense item.
 *  
 * AddExpense Button 
 * -----------------------------------------------------------------------------
 * Clicking on this button opens the edit expense activity, which helps create
 * a new expense item. 
 * 
 * @author Sandeep Deb (deb.sandeep@gmail.com)
 */
public class MainActivity extends Activity 
    implements OnClickListener, OnItemClickListener, AddAmtDialogFragmentListener {

    // Key to ferry an instance of ExpenseItem to sub activities and get
    // get them back via the Intent bundle.
    public static final String EXPENSE_ITEM_TAG_KEY  = ExpenseItem.class.getName() ;
    
    /**
     *  The request codes to use when launching a sub activity. These codes will
     *  be returned back via the callback when the sub activity returns
     */
    // Request code to start the edit expense activity for editing a new expense
    public static final int NEW_EXPENSE_EDIT_REQ_CD = 100 ;
    
    // Request code to start the edit expense activity for updating an expense
    public static final int UPD_EXPENSE_EDIT_REQ_CD = 200 ;
    
    // A reference to the add expense button which is a part of this activity
	private Button addExpenseBtn = null ;
	
	// A reference to the expense item list which is a part of this activity
	private ListView expenseList = null ;
	
	// The add amount dialog fragment
	private DialogFragment addAmtDialog = null ;

	// The adapter for the expense item list. Note that this is a custom list
	// adapter which creates a specialized view for the list items
	private ExpenseItemListAdapter listAdapter = null ;
    
	// A reference to keep track of the item being edited. We need this reference
	// to update the item when the editing sub activity returns. If the value
	// of this reference is -1, it implies a new expense is being edited.
    private int positionOfItemBeingEdited = -1 ;
    
    // The SQLite database helper and data access objects
    private ExpenseItemDAO expenseItemDAO = null ;
    
    // A custom comparator to sort the expense items
    private ExpenseItemComparator comparator = new ExpenseItemComparator() ;
    
    /* ====================================================================== */
    // Creating call back methods
    /* ====================================================================== */

    /**
     * This function is called during the creation of this activity. We 
     * capture references of views, link listeners to views and enhance views
     * with special adapters
     */
    @Override
	protected void onCreate( Bundle savedInstanceState ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.MAIN_ACTIVITY, "Entering onCreate" ) ;
        }
        
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize the database and data access objects
		DAOManager.initialize( this ) ;
		expenseItemDAO = DAOManager.getInstance().getExpenseItemDAO() ;
		        
		// Initialize the user interface		
		addExpenseBtn = ( Button )findViewById( R.id.addExpenseBtn ) ;
		addExpenseBtn.setOnClickListener( this ) ;
        
        // Create the add amount dialog
        addAmtDialog = new AddAmtDialogFragment() ;
		
		expenseList = ( ListView )findViewById( R.id.lv ) ;
		expenseList.setOnItemClickListener( this ) ;
		expenseList.setLongClickable( true ) ;
		registerForContextMenu( expenseList ) ;
		
		try {
	        if( BuildConfig.DEBUG ) {
	            Log.d( LogTag.MAIN_ACTIVITY, "Creating expense list adapter" ) ;
	        }
	        
		    listAdapter = new ExpenseItemListAdapter( 
                		                    this, R.layout.expense_list_item ) ;
            expenseList.setAdapter( listAdapter ) ;
        } 
		catch ( Exception e ) {
		    Log.e( LogTag.MAIN_ACTIVITY, "Exception loading expense items", e ) ;
        }
	}
    
    /**
     * This lifecycle event is called when the activity comes to the foreground
     * after one of the child activity finishes off. We refresh the list 
     * to display the latest of any changes or preferences. 
     */
    @Override
    protected void onRestart() {
        super.onRestart() ;
        listAdapter.notifyDataSetChanged() ;
    }
    
    /**
     * This function is called during the creation of this activity to load
     * the action bar option menu for this activity. We use the menu inflater
     * to attach our main action bar menu to this activity.
     * 
     * The menu action events will be received in the onOptionsItemSelected
     * method which is overridden in this class. 
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.MAIN_ACTIVITY, "Creating option menu" ) ;
        }
	    
        getMenuInflater().inflate(R.menu.menu_main_activity_action_bar, menu);
		return true;
	}
	
	/**
	 * This method is called to set up the context menu for the list view.
	 */
    @Override
    public void onCreateContextMenu( ContextMenu menu, View v,
                                     ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo ) ;
        getMenuInflater().inflate( R.menu.expense_item_context_menu, menu ) ;
    }
    
    /**
     * This method is called when this activity finishes. We close the database
     * here.
     */
    @Override
    public void finish() {
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.MAIN_ACTIVITY, "Activity finishing. Closing DB" ) ;
        }
        DAOManager.getInstance().closeDB() ;
        super.finish() ;
    }

    /* ====================================================================== */
    // Action call back methods
    /* ====================================================================== */
    /**
     * This function is called when the add expense button is clicked. We launch
     * the edit expense sub activity for creating a new expense item. 
     */
	@Override
	public void onClick( View v ) {
		
		if( v.getId() == R.id.addExpenseBtn ) {
	        if( BuildConfig.DEBUG ) {
	            Log.d( LogTag.MAIN_ACTIVITY, "Add Expense clicked" ) ;
	        }
	        positionOfItemBeingEdited = -1 ;
		    launchExpenseEdit( NEW_EXPENSE_EDIT_REQ_CD, null ) ;
		}
	}

    /**
     * This function is called on the short click of any of the expense list 
     * items. I consider it as a trigger for editing the expense item. The 
     * expense edit sub activity is launched.
     */
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position,
                             long id ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.MAIN_ACTIVITY, "Expense item is to be updated" ) ;
        }
        
        ExpenseItem item = ( ExpenseItem )view.getTag( R.string.expense_item_tag ) ;
        positionOfItemBeingEdited = position ;
        launchExpenseEdit( UPD_EXPENSE_EDIT_REQ_CD, item ) ;
    }
    
    /**
     * This method is called when the user presses any of the context menu items.
     * We capture the context menu item and fan out the processing accordingly. 
     */
    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        
        Log.d( LogTag.MAIN_ACTIVITY, "Context menu item clicked" ) ;
        
        // Get a reference to the expense item for which the context item was
        // clicked.
        int position = -1 ; 
        AdapterContextMenuInfo info = null ;
        ExpenseItem expenseItem = null ;
        
        info = ( AdapterContextMenuInfo )item.getMenuInfo() ;
        position = ( int )info.id ;
        expenseItem = listAdapter.getItem( position ) ;
        
        switch( item.getItemId() ) {
            
            case R.id.expense_item_ctx_menuitem_delete : {
                
                Log.d( LogTag.MAIN_ACTIVITY, "Delete context menu item clicked" ) ;
                expenseItemDAO.delete( expenseItem ) ;
                listAdapter.remove( expenseItem ) ;
                listAdapter.notifyDataSetChanged() ;
                Toast.makeText( this, "Expense deleted", Toast.LENGTH_SHORT ).show() ;
                
                break ;
            }
            case R.id.expense_item_ctx_menuitem_clone : {
                
                Log.d( LogTag.MAIN_ACTIVITY, "Clone context menu item clicked" ) ;
                
                // Create a cloned item from the selected item. Note that a 
                // cloned item is sent for editing even before it is inserted
                // into the database. Hence the id is set to -1. Also, the date
                // is set to current date so that the expense is created for 
                // today by default.
                ExpenseItem clone = new ExpenseItem() ;
                clone.copyDataFrom( expenseItem ) ;
                clone.setDate( new Date() ) ;
                clone.setId( -1 ) ;
                
                positionOfItemBeingEdited = -1 ;
                launchExpenseEdit( NEW_EXPENSE_EDIT_REQ_CD, clone ) ;
                break ;
            }
            case R.id.expense_item_ctx_menuitem_clone_ex : {
                
                Log.d( LogTag.MAIN_ACTIVITY, "Clone (*) context menu item clicked" ) ;
                
                // Create a cloned item from the selected item. Note that a 
                // cloned item is sent for editing even before it is inserted
                // into the database. Hence the id is set to -1. Since this is
                // an extended clone, we do not change the date.
                ExpenseItem clone = new ExpenseItem() ;
                clone.copyDataFrom( expenseItem ) ;
                clone.setDescription( "" ) ;
                clone.setAmount( 0 ) ;
                clone.setId( -1 ) ;
                
                positionOfItemBeingEdited = -1 ;
                launchExpenseEdit( NEW_EXPENSE_EDIT_REQ_CD, clone ) ;
                break ;
            }
            case R.id.expense_item_ctx_menuitem_addamt : {
                
                Log.d( LogTag.MAIN_ACTIVITY, "Add amount menu item clicked" ) ;
                positionOfItemBeingEdited = position ;
                addAmtDialog.show( getFragmentManager(), "AddAmtDialog" ) ;
                break ;
            }
                
        }
        
        return super.onContextItemSelected( item ) ;
    }

    /**
     * This method is called when the user tried to add a non zero amount to
     * an existing expense item. The expense item that the user wants to add
     * the amount to is referred to by the selected item position in list view
     */
    @Override
    public void amountAdded( int amt ) {

        // We fetch the selected expense item, add the amount to both the instance
        // and update the database and refresh the list adapter so that the view 
        // gets refreshed.
        ExpenseItem item = listAdapter.getItem( positionOfItemBeingEdited ) ;
        item.setAmount( item.getAmount() + amt ) ;
        
        expenseItemDAO.update( item ) ;
        listAdapter.notifyDataSetChanged() ;
    }
    
    /**
     * This function is called when the action bar option menu items are 
     * selected. We delegate processing to utility methods based on which of
     * the action bar buttons were clicked.
     */
    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        
        if( item.getItemId() == R.id.action_export ) {
            
            exportDataIntoCSV() ;
        }
        else if( item.getItemId() == R.id.action_settings ) {
            
            Intent intent = new Intent( this, SettingsActivity.class ) ;
            startActivity( intent ) ;
        }
        else if( item.getItemId() == R.id.action_delete_all ) {
            
            deleteAllExpenseItems() ;
        }
        else if( item.getItemId() == R.id.action_report ) {
            
            Intent intent = new Intent( this, ReportActivity.class ) ;
            startActivity( intent ) ;
        }
        
        return super.onOptionsItemSelected( item ) ;
    }

    /* ====================================================================== */
    // Sub activity return call back methods
    /* ====================================================================== */
    /**
     * This function is called when any of the sub activities launched by this
     * activity returns. The type of processing is decided by the request code.
     */
    @Override
    protected void onActivityResult( int requestCode, int resultCode,
                                     Intent data ) {
        
        // If this method is being called because of the return of edit expense
        // sub activity, delegate to proper internal method.
        if( requestCode == NEW_EXPENSE_EDIT_REQ_CD || 
            requestCode == UPD_EXPENSE_EDIT_REQ_CD ) {
            
            if( BuildConfig.DEBUG ) {
                Log.d( LogTag.MAIN_ACTIVITY, "Edit expense sub activity has returned" ) ;
            }
            editExpenseSubActivityReturn( requestCode, resultCode, data ) ;
        }
    }
    
    /* ====================================================================== */
    // Private methods for this class
    /* ====================================================================== */
    /**
     * This function launches the sub activity for editing an expense item. 
     * The {@link ExpenseEntryActivity} can be used for either creating a new
     * expense item or editing an existing one. The type of editing option
     * is provided to the sub activity via the request code.
     * 
     * In case we are using the sub activity to edit an existing expense item,
     * we pass the expense item to edit embedded in the intent against the 
     * {@link MainActivity}.EXPENSE_ITEM_TAG_KEY key.
     * 
     * @param editType Either NEW_EXPENSE_EDIT_REQ_CD or UPD_EXPENSE_EDIT_REQ_CD
     *        implying a new expense item edit or updating an existing expense
     *        item.
     *        
     * @param itemToEdit The expense item instance to edit. If the edit type
     *        is update and the itemToEdit is null an exception will be raised.
     *        If the edit type is new, item to edit can optionally be null in
     *        which case a new expense item will be created and sent for 
     *        editing.
     */
    private void launchExpenseEdit( int editType, ExpenseItem itemToEdit ) {
    
        Intent intent = new Intent( this, ExpenseEntryActivity.class ) ;
        
        if( editType == UPD_EXPENSE_EDIT_REQ_CD && itemToEdit == null ) {
            throw new IllegalArgumentException( "Item to edit can't be null " + 
                                              "when the edit type is update" ) ;
        }
        
        if( itemToEdit == null ) {
            itemToEdit = new ExpenseItem() ;
        }
            
        intent.putExtra( EXPENSE_ITEM_TAG_KEY, itemToEdit ) ;

        // We are launching the sub activity expecting it to return a response
        // back to us. The onActivityResult method will be called once the 
        // sub activity is finished. onActivityResult method further delegates
        // processing to editExpenseSubActivityReturn method.
        startActivityForResult( intent, editType ) ;
    }
    
    /**
     * This method is invoked when edit expense sub activity returns.
     */
    private void editExpenseSubActivityReturn( int requestCode, int resultCode, 
                                               Intent data ) {
        
        // We process only if the edit expense sub activity was not cancelled 
        // or aborted.
        if( resultCode == RESULT_OK ) {
            
            ExpenseItem expenseItem = ( ExpenseItem )data.getExtras().
                                       getSerializable( EXPENSE_ITEM_TAG_KEY ) ;

            if( requestCode == NEW_EXPENSE_EDIT_REQ_CD ) {
                
                expenseItem = expenseItemDAO.create( expenseItem ) ;
                listAdapter.insert( expenseItem, 0 ) ;
                
                Toast.makeText( this, "Expense added", Toast.LENGTH_SHORT ).show() ;
            }
            else if( requestCode == UPD_EXPENSE_EDIT_REQ_CD ) {
                // Note that the object to which expenseItemBeingEdited reference
                // points, is already in the adapter and hence need not be added
                // to the list again. Just refreshing the list will ensure that
                // the changed data is visible.
                expenseItemDAO.update( expenseItem ) ;

                // Update the item being edited with the edited data. Note that
                // When the sub activity returns the tag data being serializable
                // does not guarantee that the reference will remain the same
                // hence we do a value copy.
                ExpenseItem item = listAdapter.getItem( positionOfItemBeingEdited ) ;
                item.copyDataFrom( expenseItem ) ;
                
                Toast.makeText( this, "Expense updated", Toast.LENGTH_SHORT ).show() ;
            }

            // Sort the list adapter so that all the expense items are arranged
            // in their right order.
            listAdapter.sort( comparator ) ;
            listAdapter.notifyDataSetChanged() ;
        }
        
        return ;
    }
    
    /** Exports all the data in the database into a CSV file. */
    private void exportDataIntoCSV() {
        
        ExpenseCSVExporter exporter = new ExpenseCSVExporter() ;
        List<ExpenseItem> expenseItems = expenseItemDAO.getAllExpenseItems() ;
        
        if( !expenseItems.isEmpty() ) {
            String fileName = exporter.exportExpenseItems( expenseItems ) ;
            
            String msg = null ;
            if( fileName == null ) {
                msg = "Data could not be exported" ;
            }
            else {
                msg = "Data exported to " + fileName ;
            }
            
            Toast.makeText( this, msg, Toast.LENGTH_SHORT ).show() ;
        }
    }
    
    /** Deletes all expense items after user confirmation */
    private void deleteAllExpenseItems() {
        
        Builder builder = new AlertDialog.Builder( this, AlertDialog.THEME_HOLO_DARK ) ;
        builder.setMessage( "This will permanently delete all expense items" ) ;
        builder.setCancelable( false ) ;
        builder.setTitle( "Confirm" ) ;
        
        builder.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int which ) {
                dialog.dismiss() ;
                expenseItemDAO.deleteAll() ;
                listAdapter.clear() ;
                listAdapter.notifyDataSetChanged() ;
                Toast.makeText( MainActivity.this, "All data deleted", 
                                Toast.LENGTH_SHORT ).show() ;
            }
        } ) ;
        
        builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int which ) {
                dialog.dismiss() ;
            }
        } ) ;
        
        AlertDialog dialog = builder.create();
        dialog.show();            
    }
}
