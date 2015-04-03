package com.sandy.android.expensetracker;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.HashMap ;
import java.util.List ;

import android.content.Context ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.BaseExpandableListAdapter ;
import android.widget.TextView ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * This is the adapter for the report list view. The report list view is an
 * Expandable list where each group is the expense category and the 
 * group items are the sub categories. Each shows the aggregated amount 
 * against itself. 
 * 
 * @author Sandeep Deb
 */
public class ReportListAdapter extends BaseExpandableListAdapter {
    
    // -------------------------------------------------------------------------
    //               Inner classes
    // -------------------------------------------------------------------------
    private class ExpenseReportRow implements Comparable<ExpenseReportRow> {
        
        int id ;
        int amt ;
        
        public ExpenseReportRow( int id, int amt ) {
            this.id = id ;
            this.amt = amt ;
        }
        
        public int compareTo( ExpenseReportRow another ) {
            return amt - another.amt ;
        }

        public boolean equals( Object o ) {
            return id == ((ExpenseReportRow)o).id ;
        }

        public int hashCode() {
            return id ;
        }
    }
    
    // The context under which this adapter operates
    private Context context = null ;
    
    // Data structure to hold the report data
    private List<ExpenseReportRow> expenseGroups = new ArrayList<ReportListAdapter.ExpenseReportRow>() ;
    private HashMap<Integer, List<ExpenseReportRow>> expenseGroupItems = new HashMap<Integer, List<ExpenseReportRow>>() ;
    int     totalAmt = 0 ;
    
    /**
     * While constructing the adapter, we access the expense item DAO and 
     * prepare the internal data structure, which we will returns during 
     * various method call backs.
     */
    public ReportListAdapter( Context context ) {
        initialize() ;
        this.context = context ;
    }
    
    /** A private utility method to initialize the internal data structures. */
    private void initialize() {
        
        List<ExpenseItem> expenseItems = DAOManager.getInstance().
                                         getExpenseItemDAO().getAllExpenseItems() ;

        List<ExpenseReportRow> groupItems = null ;
        ExpenseReportRow       groupRow   = null ;
        ExpenseReportRow       expenseRow = null ;
        
        for( ExpenseItem item : expenseItems ) {

            totalAmt += item.getAmount() ;
            
            expenseRow = new ExpenseReportRow( item.getSubCatId(), item.getAmount() ) ;
            groupRow   = new ExpenseReportRow( item.getCatId(),    item.getAmount() ) ;
            
            // See if expenses of the category that this expense item belongs
            // to have already been encountered. If so, we dig deep and add 
            // the amount to the cumulative amount.
            groupItems = expenseGroupItems.get( item.getCatId() ) ;
            if( groupItems == null ) {
                
                groupItems = new ArrayList() ;
                
                groupItems.add( expenseRow ) ;
                expenseGroupItems.put( item.getCatId(), groupItems ) ;
                expenseGroups.add( groupRow ) ;
            }
            else {
                int index = groupItems.indexOf( expenseRow ) ;
                if( index != -1 ) {
                    groupItems.get( index ).amt += expenseRow.amt ;
                }
                else {
                    groupItems.add( expenseRow ) ;
                }
                
                expenseGroups.get( expenseGroups.indexOf( groupRow ) ).amt += expenseRow.amt ;
            }
        }
        
        Collections.sort( expenseGroups ) ;
        for( List<ExpenseReportRow> rowList : expenseGroupItems.values() ) {
            Collections.sort( rowList ) ;
        }
    }
    
    public int getTotalAmt() {
        return this.totalAmt ;
    }
    
    public int getGroupCount() { 
        return expenseGroups.size() ; 
    }

    public int getChildrenCount( int groupPosition ) {
        List<ExpenseReportRow> childRows = null ;
        childRows = expenseGroupItems.get( expenseGroups.get( groupPosition ).id ) ;
        if( childRows != null ) {
            return childRows.size() ;
        }
        return 0 ;
    }

    public boolean hasStableIds() { 
        return true ; 
    }

    public boolean isChildSelectable( int groupPosition, int childPosition ) { 
        return false ; 
    }
    
    public long getGroupId( int groupPosition ) { 
        return groupPosition ; 
    }

    public long getChildId( int groupPosition, int childPosition ) { 
        return childPosition ; 
    }

    public Object getGroup( int groupPosition ) { 
        return expenseGroups.get( groupPosition ) ; 
    }

    public Object getChild( int groupPosition, int childPosition ) {
        return expenseGroupItems.get( expenseGroups.get( groupPosition ).id ).get( childPosition ) ;
    }

    public View getGroupView( int groupPosition, boolean isExpanded,
                              View convertView, ViewGroup parent ) {
        
        ExpenseReportRow reportRow = ( ExpenseReportRow )getGroup( groupPosition ) ;
        return getView( reportRow, convertView, true ) ;    
    }

    public View getChildView( int groupPosition, int childPosition,
                              boolean isLastChild, View convertView, 
                              ViewGroup parent ) {
        
        ExpenseReportRow reportRow = ( ExpenseReportRow )getChild( groupPosition, childPosition ) ;
        return getView( reportRow, convertView, false ) ;    
    }
    
    /**
     * Get the appropriate view for the given expense report row, populated
     * with appropriate data.
     */
    private View getView( ExpenseReportRow reportRow, View convertView, boolean isGroup ) {

        // First, we get the proper view inflated for this report row. Remember
        // that the expense category rows and the expense sub category rows
        // have different templates but the view elements are the same type.
        if( convertView == null ) {
            LayoutInflater inflater = ( LayoutInflater )context.getSystemService( Context.LAYOUT_INFLATER_SERVICE ) ;
            
            if( isGroup ) {
                convertView = inflater.inflate( R.layout.activity_report_listitem_group, null ) ;
            }
            else {
                convertView = inflater.inflate( R.layout.activity_report_listitem, null ) ;
            }
        }
        
        TextView head = null ; 
        TextView amt  = null ;
        
        // Now, depending upon whether we are dealing with a category or 
        // sub category row, we extract the right expense item head and amount
        // view references.
        if( isGroup ) {
            head = ( TextView ) convertView.findViewById( R.id.actReportCatLabel ) ;
            amt = ( TextView ) convertView.findViewById( R.id.actReportCatAmtLabel ) ;
        }
        else {
            head = ( TextView ) convertView.findViewById( R.id.actReportSubCatLabel ) ;
            amt = ( TextView ) convertView.findViewById( R.id.actReportSubCatAmtLabel ) ;
        }
        
        // Now we set the data in the views. If it is a group, we fetch the 
        // category name, else we fetch the sub category name and set it to 
        // the text view
        CategoryDAO catDAO = DAOManager.getInstance().getCategoryDAO() ;
        if( isGroup ) {
            head.setText( catDAO.getCategoryName( reportRow.id ) ) ;
        }
        else {
            head.setText( catDAO.getSubCategoryName( reportRow.id ) ) ;
        }
        
        amt.setText( "" + reportRow.amt ) ;
        
        return convertView ;
    }
}
