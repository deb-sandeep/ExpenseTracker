package com.sandy.android.expensetracker ;

import java.text.SimpleDateFormat ;
import java.util.Locale ;

import android.app.Activity ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.ArrayAdapter ;
import android.widget.TextView ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.db.ExpenseItemDAO ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * Specialized list adapter for displaying the expense item list in the 
 * main activity. We need a specialized adapter since the list items layout is 
 * customized.
 * 
 * @author Sandeep Deb
 */
public class ExpenseItemListAdapter extends ArrayAdapter<ExpenseItem> {

    private static SimpleDateFormat SDF = new SimpleDateFormat( "dd MMM", Locale.US ) ;
    
    private ExpenseItemDAO expItemDAO = null ;
    private CategoryDAO    catDAO     = null ;
    private Activity       activity   = null ;

    public class ViewHolder {
        TextView dateTV   = null ;
        TextView amtTV    = null ;
        TextView catTV    = null ;
        TextView subCatTV = null ;
        TextView descTV   = null ;
    }

    public ExpenseItemListAdapter( Activity context, int resource )
            throws Exception {

        super( context, resource ) ;
        this.activity = context ;
        this.expItemDAO = DAOManager.getInstance().getExpenseItemDAO() ;
        this.catDAO = DAOManager.getInstance().getCategoryDAO() ;
        super.addAll( this.expItemDAO.getAllExpenseItems() ) ;
    }

    /**
     * Returns the view required to display a particular list item as referred
     * to by the position. This method also tries to optimize the processing
     * by convering an existing view and populating it with displayable data.
     */
    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {

        View        rv   = convertView ;
        ViewHolder  vh   = null ;
        ExpenseItem item = super.getItem( position ) ;
        
        // Inflate a row view if and only if we can't reuse an existing one.
        // Remember, inflating a row view and finding views in the layout is
        // a very costly operation which slows down the UI if the list grows
        // big.
        if (rv == null) {
            LayoutInflater inflater = activity.getLayoutInflater() ;
            rv = inflater.inflate( R.layout.expense_list_item, null ) ;
            
            vh = new ViewHolder() ;
            
            // If we are inflating a new list view, we extract the embedded
            // views and store them as a tag in the view. This ensures that 
            // we don't have to do a findViewById every time we want to access
            // the view.
            vh.dateTV   = (TextView)rv.findViewById( R.id.expListItemDateTV ) ;
            vh.amtTV    = (TextView)rv.findViewById( R.id.expListItemAmtTV ) ;
            vh.catTV    = (TextView)rv.findViewById( R.id.expListItemCategoryTV ) ;
            vh.subCatTV = (TextView)rv.findViewById( R.id.expListItemSubCategoryTV ) ;
            vh.descTV   = (TextView)rv.findViewById( R.id.expListItemDescTV ) ;
            
            rv.setTag( vh ) ;
        }

        // We extract the references to the embedded views and set data into them
        // directly.
        vh = ( ViewHolder )rv.getTag() ;
        
        String catName = null ;
        String subCatName = null ;
        
        catName = catDAO.getCategoryName( item.getCatId() ) ;
        if( catName == null ) catName = "<Unknown Category>" ;
        
        subCatName = catDAO.getSubCategoryName( item.getSubCatId() ) ;
        if( subCatName == null ) subCatName = "<Unknown Sub-Category>" ;
        
        vh.dateTV.setText( SDF.format( item.getDate() ) ) ;
        vh.amtTV.setText( Integer.toString( item.getAmount() ) ) ;
        vh.catTV.setText( catName ) ;
        vh.subCatTV.setText( subCatName ) ;
        vh.descTV.setText( item.getDescription() ) ;
        
        // We also keep a reference to the expense item that this row view is
        // displaying as a tag element. This saves us the trouble of maintaining
        // auxiliary data structure to associate a row with the expense item 
        // or doing the additional processing of recreating the expense item
        // from the view data.
        rv.setTag( R.string.expense_item_tag, item ) ;

        return rv ;
    }
}
