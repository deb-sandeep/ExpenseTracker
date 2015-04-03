package com.sandy.android.expensetracker ;

import java.text.SimpleDateFormat ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Calendar ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Locale ;

import android.app.Activity ;
import android.app.AlertDialog ;
import android.app.AlertDialog.Builder ;
import android.app.DatePickerDialog ;
import android.app.DatePickerDialog.OnDateSetListener ;
import android.app.Dialog ;
import android.app.DialogFragment ;
import android.content.DialogInterface ;
import android.content.Intent ;
import android.os.Bundle ;
import android.util.Log ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.widget.AdapterView ;
import android.widget.AdapterView.OnItemSelectedListener ;
import android.widget.ArrayAdapter ;
import android.widget.Button ;
import android.widget.DatePicker ;
import android.widget.EditText ;
import android.widget.Spinner ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * This activity is used to edit an expense item, either new or existing. This
 * activity is launched by the MainActivity with hints on whether to edit an 
 * existing expense item or edit a new expense item, embedded in the intent. 
 * 
 * @author Sandeep Deb
 */
public class ExpenseEntryActivity extends Activity 
    implements OnItemSelectedListener, OnClickListener, DatePickerDialog.OnDateSetListener {
    
    /** A date format to parse and format dates in this class. */
    private static SimpleDateFormat SDF = new SimpleDateFormat( "dd-MMM-yyyy", Locale.US ) ;
    
    /** A reference to the expense item that is being edited by this activity. */
    private ExpenseItem expenseItem = null ;
    private CategoryDAO catDAO = null ;
    
    // References to all the views contained in this activity.
    private Button   dateSelectionBtn = null ;
    private Spinner  catSpinner       = null ;
    private Spinner  subCatSpinner    = null ;
    private Spinner  paidBySpinner    = null ;
    private EditText amtEditor        = null ;
    private EditText descEditor       = null ;
    private Button   okBtn            = null ;
    private Button   cancelBtn        = null ;
    
    // References to all the spinner adapters used in this activity. Note that 
    // the sub category spinner data model is dynamic and is decided based on 
    // the currently selected category. Hence we maintain a hash map of category
    // names versus the adapters, so that we can swap them pretty fast.
    private CatSubCatItemListAdapter catSpinnerAdapter    = null ;
    private ArrayAdapter<String>     paidBySpinnerAdapter = null ;
    private HashMap<Integer, CatSubCatItemListAdapter> subCatAdapterMap = 
                              new HashMap<Integer, CatSubCatItemListAdapter>() ;
    
    // A flag which is set to true, if the user decides to abort the edit. By
    // default this is true, so that when the user presses the device back
    // button the changes are discarded.
    private boolean discardChanges = true ;

    /**
     * This method is called on creation of this activity. We do housekeeping
     * work in this method, including pre population of the views.
     */
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState ) ;
        setContentView( R.layout.activity_expense_entry ) ;
        
        catDAO = DAOManager.getInstance().getCategoryDAO() ;

        // Get the references of views that belong to this activity
        getViewReferences() ;
        prepareViews() ;
        
        // Create the expense item based on whether we are editing an existing
        // expense item or creating a new expense item.
        createExpenseItem() ;

        // Pre-populate the view elements of this activity based on the expense
        // item we are editing
        prePopulateViews() ;
    }
    
    private void getViewReferences() {
        
        dateSelectionBtn = ( Button   ) findViewById( R.id.dateSelectionBtn   ) ;
        catSpinner       = ( Spinner  ) findViewById( R.id.categorySpinner    ) ;
        subCatSpinner    = ( Spinner  ) findViewById( R.id.subCategorySpinner ) ;
        paidBySpinner    = ( Spinner  ) findViewById( R.id.paidBySpinner      ) ;
        amtEditor        = ( EditText ) findViewById( R.id.amtEditText        ) ;
        descEditor       = ( EditText ) findViewById( R.id.descEditText       ) ;
        okBtn            = ( Button   ) findViewById( R.id.okBtn              ) ;
        cancelBtn        = ( Button   ) findViewById( R.id.cancelBtn          ) ;
    }
    
    private void prepareViews() {
        
        // Prepare the category spinner by creating and setting the adapter
        // and setting the selection listener
        List<Integer> catIds = catDAO.getCategoryIds() ;
        
        catSpinnerAdapter = new CatSubCatItemListAdapter( this, catIds, 
                                   CatSubCatItemListAdapter.CAT_LIST_ADAPTER ) ;
        catSpinner.setAdapter( catSpinnerAdapter ) ;
        catSpinner.setOnItemSelectedListener( this ) ;
        
        // Prepare the paid by spinner by creating and setting the adapter
        String[] options = getResources().getStringArray( R.array.paid_by_options ) ;
        ArrayList<String> optionsList = new ArrayList<String>( Arrays.asList( options ) ) ;
        
        paidBySpinnerAdapter = new ArrayAdapter( this, R.layout.spinner_item, 
                                                 optionsList ) ;
        paidBySpinner.setAdapter( paidBySpinnerAdapter ) ;
        
        // Add ok and cancel button's click listener to this activity
        okBtn.setOnClickListener( this ) ;
        cancelBtn.setOnClickListener( this ) ;
        dateSelectionBtn.setOnClickListener( this ) ;
    }
    
    private void createExpenseItem() {
        
        Bundle bdl = getIntent().getExtras() ; 
        if( bdl != null ) {
            expenseItem = ( ExpenseItem )bdl.getSerializable( MainActivity.EXPENSE_ITEM_TAG_KEY ) ;
        }
    }
    
    private void prePopulateViews() {
        
        dateSelectionBtn.setText( SDF.format( expenseItem.getDate() ) ) ;

        if( expenseItem.getCatId() != -1 ) {
            int position = catSpinnerAdapter.getPosition( expenseItem.getCatId() ) ;
            catSpinner.setSelection( position ) ;
        }

        if( expenseItem.getPaidBy() != null ) {
            int position = paidBySpinnerAdapter.getPosition( expenseItem.getPaidBy() ) ;
            paidBySpinner.setSelection( position ) ;
        }
        
        amtEditor.setText( Integer.toString( expenseItem.getAmount() ) ) ;
        
        if( expenseItem.getDescription() != null ) {
            descEditor.setText( expenseItem.getDescription() ) ;
        }
        
        // Set the cursor to the end in amount edit text box
        amtEditor.setSelection( amtEditor.length() ) ;
    }

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int position,
                                long id ) {
        
        // Call comes here if an expense category item is selected
        int selCatId = -1 ;
        CatSubCatItemListAdapter subCatAdapter = null ;
        
        selCatId = catSpinnerAdapter.getItem( position ) ;
        subCatAdapter = getSubCatAdapter( selCatId ) ;
        
        subCatSpinner.setAdapter( subCatAdapter ) ;
        if( expenseItem.getSubCatId() != -1 ) {
            int pos = subCatAdapter.getPosition( expenseItem.getSubCatId() ) ;
            subCatSpinner.setSelection( pos ) ;
        }
        subCatAdapter.notifyDataSetChanged() ;
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {}
    
    private CatSubCatItemListAdapter getSubCatAdapter( int catId ) {
        
        CatSubCatItemListAdapter adapter = subCatAdapterMap.get( catId ) ;
        
        if( adapter == null ) {
            
            List<Integer> subCatIdList = catDAO.getSubCategoryIds( catId ) ;
            adapter = new CatSubCatItemListAdapter( this, subCatIdList, 
                                CatSubCatItemListAdapter.SUBCAT_LIST_ADAPTER ) ;
            
            subCatAdapterMap.put( catId, adapter ) ;
        }
        return adapter ;
    }

    @Override
    public void onClick( View v ) {
        
        int viewId = v.getId() ;
        if( viewId == R.id.okBtn ) {
            discardChanges = false ;
            finish() ;
        }
        else if( viewId == R.id.cancelBtn ) {
            discardChanges = true ;
            finish() ;
        }
        else if( viewId == R.id.dateSelectionBtn ) {
            
            DialogFragment newFragment = new DatePickerFragment();
            
            Bundle bundle = new Bundle() ;
            Calendar cal = Calendar.getInstance() ;
            cal.setTime( expenseItem.getDate() ) ;
            
            bundle.putInt( "year",  cal.get( Calendar.YEAR ) ) ;
            bundle.putInt( "month", cal.get( Calendar.MONTH ) ) ;
            bundle.putInt( "day",   cal.get( Calendar.DAY_OF_MONTH ) ) ;
            
            newFragment.setArguments( bundle ) ;
            newFragment.show( getFragmentManager(), "datePicker" ) ;
        }
    }
    
    public void onDateSet( DatePicker view, int year, int month, int day ) {
        
        Calendar cal = Calendar.getInstance() ;
        cal.set( year, month, day ) ;
        dateSelectionBtn.setText( SDF.format( cal.getTime() ) ) ;
    }
    
    public void finish() {
        
        Intent data = new Intent() ;
        
        if( discardChanges ) {
            setResult( RESULT_CANCELED ) ;
        }
        else {
            // Populate expense item with the data in the dialog
            try {
                String amt = amtEditor.getText().toString() ;
                if( amt == null || amt.trim().equals( "" ) ) {
                    amt = "0" ;
                }
                
                expenseItem.setAmount     ( Integer.parseInt( amt ) ) ;
                expenseItem.setDate       ( SDF.parse( dateSelectionBtn.getText().toString() ) ) ;
                expenseItem.setCatId      ( ( Integer )catSpinner.getSelectedItem() ) ;
                expenseItem.setSubCatId   ( ( Integer )subCatSpinner.getSelectedItem() ) ;
                expenseItem.setPaidBy     ( paidBySpinner.getSelectedItem().toString() ) ;
                expenseItem.setDescription( descEditor.getText().toString() ) ;
            }
            catch( Exception e ) {
                Log.e( "ExpenseEntryActivity", "Could not populate expense item", e ) ;
            }
            
            data.putExtra( MainActivity.EXPENSE_ITEM_TAG_KEY, expenseItem ) ;
            setResult( RESULT_OK, data ) ;
        }

        expenseItem = null ;
        super.finish() ;
    }

    /**
     * This is called when the user presses the back button. We trap this to
     * show the user a confirmation dialog, lest all his changes be lost.
     */
    @Override
    public void onBackPressed() {
        
        Log.d( "Test", "Back button pressed" ) ;
        Builder builder = new AlertDialog.Builder( this, AlertDialog.THEME_HOLO_DARK ) ;
        builder.setMessage( "Do you want to discard your changes" ) ;
        builder.setCancelable( false ) ;
        builder.setTitle( "Confirm" ) ;
        
        builder.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int which ) {
                dialog.dismiss() ;
                ExpenseEntryActivity.super.onBackPressed() ;
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

    // =========================================================================
    //          Inner classes
    // =========================================================================
    public static class DatePickerFragment extends DialogFragment {
        
        private OnDateSetListener listener ;
        
        @Override
        public void onAttach( Activity activity ) {
            super.onAttach( activity ) ;
            this.listener = ( OnDateSetListener )activity ;
        }

        @Override
        public Dialog onCreateDialog( Bundle savedInstanceState ) {
            
            int year  = getArguments().getInt( "year" ) ;
            int month = getArguments().getInt( "month" ) ;
            int day   = getArguments().getInt( "day" ) ;
            
            final DatePickerDialog dateDialog = new DatePickerDialog( getActivity(), 
                         AlertDialog.THEME_HOLO_DARK, null, year, month, day ) ;
            dateDialog.setCancelable( false ) ;
            dateDialog.setCanceledOnTouchOutside( false ) ;
            
            dateDialog.setButton( DialogInterface.BUTTON_NEGATIVE, "Cancel", 
                new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int which ) {
                        dateDialog.dismiss() ;
                    }
            }) ;
            
            dateDialog.setButton( DialogInterface.BUTTON_POSITIVE, "Set", 
                new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int which ) {
                        Log.d( "ExpenseEntryActivity", "Date set" ) ;
                        
                        DatePicker dp = dateDialog.getDatePicker() ;
                        int day  = dp.getDayOfMonth() ;
                        int mth  = dp.getMonth() ;
                        int year = dp.getYear() ;
                        dateDialog.dismiss() ;
                        
                        listener.onDateSet( dp, year, mth, day ) ;
                    }
            }) ;
            
            return dateDialog ;
        }
    }
}
