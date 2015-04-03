package com.sandy.android.expensetracker ;

import android.app.Activity ;
import android.content.Intent ;
import android.os.Bundle ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.widget.ImageButton ;
import android.widget.Toast ;

import com.sandy.android.expensetracker.db.DBHelper ;
import com.sandy.android.expensetracker.util.SQLiteDBExporter ;

/**
 * This class is the settings activity of the Expense Tracker application. All
 * configuration and system specific activities will be moved under this 
 * activity.
 * 
 * This activity will show different actions in the following format.
 * 
 * +=======================================================================+
 * | <Action Name>                                            |            |
 * +----------------------------------------------------------+  <Go       |
 * | <Action description                                      |   Button>  |
 * |      ... multiline >                                     |            |
 * +=======================================================================+
 * 
 * On pressing the "Go" button, the user will be transitioned to the action
 * specific activity. 
 * 
 * @author Sandeep Deb (deb.sandeep@gmail.com)
 */
public class SettingsActivity extends Activity 
    implements OnClickListener {
    
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
        setContentView( R.layout.activity_settings ) ;
        
        ImageButton b = null ;
        
        b = ( ImageButton )findViewById( R.id.exportDBBtn ) ;
        b.setOnClickListener( this ) ;
        
        b = ( ImageButton )findViewById( R.id.manageExpenseCategoriesBtn ) ;
        b.setOnClickListener( this ) ;
        
        b = ( ImageButton )findViewById( R.id.manageExpenseSubCategoriesBtn ) ;
        b.setOnClickListener( this ) ;
    }

    /**
     * This method is called when any of the settings buttons is clicked. We
     * demultiplex processing based on the buttons id.
     */
    @Override
    public void onClick( View v ) {
        
        int id = v.getId() ;
        if( id == R.id.exportDBBtn ) {
            exportDatabase() ;
        }
        else if( id == R.id.manageExpenseCategoriesBtn ) {
            Intent intent = new Intent( this, ManageExpenseCategoryActivity.class ) ;
            startActivity( intent ) ;
        }
        else if( id == R.id.manageExpenseSubCategoriesBtn ) {
            Intent intent = new Intent( this, ManageExpenseSubCategoryActivity.class ) ;
            startActivity( intent ) ;
        }
    }
    
    /* ====================================================================== */
    // Private methods
    /* ====================================================================== */

    /** Exports the current database */
    private void exportDatabase() {
        
        SQLiteDBExporter exporter = null ;
        exporter = new SQLiteDBExporter( this.getPackageName(), 
                                         DBHelper.DB_NAME, 
                                         "ExpenseTracker" ) ;
        
        String msg = exporter.exportDatabase() ;
        if( msg == null ) {
            msg = "DB exported successfully" ;
        }
        else {
            msg = "DB Export failed - " + msg ;
        }
        
        Toast.makeText( this, msg, Toast.LENGTH_SHORT ).show() ;
    }
}
