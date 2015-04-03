package com.sandy.android.expensetracker ;

import android.app.Activity ;
import android.os.Bundle ;
import android.util.Log ;
import android.widget.ExpandableListView ;
import android.widget.TextView ;

import com.sandy.android.expensetracker.util.LogTag ;

/**
 * This activity displays the expense data as a drill down report based on
 * expense categories and aggregated amount per sub category. This activity 
 * is launched from the {@link MainActivity} by clicking on the export action
 * bar menu item.
 * 
 * @author Sandeep Deb
 */
public class ReportActivity extends Activity {
    
    private ExpandableListView elv = null ;
    private TextView amt = null ;
    
    private ReportListAdapter listAdapter = null ;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        if( BuildConfig.DEBUG ) {
            Log.d( LogTag.REPORT_ACTIVITY, "Entering onCreate" ) ;
        }
        
        super.onCreate( savedInstanceState ) ;
        setContentView( R.layout.activity_report ) ;
        
        listAdapter = new ReportListAdapter( this ) ;
        
        this.elv = ( ExpandableListView )findViewById( R.id.reportListView ) ;
        this.elv.setAdapter( listAdapter ) ;
        
        this.amt = ( TextView )findViewById( R.id.actReportTotAmtLabel ) ;
        this.amt.setText( "" + listAdapter.getTotalAmt() ) ;
    }
}
