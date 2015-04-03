package com.sandy.android.expensetracker ;

import java.util.List ;

import android.app.Activity ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.ArrayAdapter ;
import android.widget.TextView ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;

/**
 * This is the list adapter for category and sub category items. This fetches
 * the category and sub category description at the time of rendering while
 * internally operating on a list of identifiers. 
 * 
 * @author Sandeep Deb
 */
public class CatSubCatItemListAdapter extends ArrayAdapter<Integer> {

    public static final int CAT_LIST_ADAPTER    = 0 ;
    public static final int SUBCAT_LIST_ADAPTER = 1 ;
    
    private CategoryDAO    catDAO     = null ;
    private int            adapterType= -1 ;
    private Activity       context    = null ;

    public CatSubCatItemListAdapter( Activity context, List<Integer> idList,
                                     int adapterType ) {

        super( context, R.layout.spinner_item ) ;
        
        this.context     = context ;
        this.adapterType = adapterType ;
        this.catDAO      = DAOManager.getInstance().getCategoryDAO() ;
        
        super.addAll( idList ) ;
    }

    /**
     * Returns the view required to display a particular list item as referred
     * to by the position. This method also tries to optimize the processing
     * by convering an existing view and populating it with displayable data.
     */
    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {

        TextView    rv   = ( TextView )convertView ;
        Integer     id   = super.getItem( position ) ;
        
        String itemName = null ;
        
        if( rv == null ) {
            LayoutInflater inflater = context.getLayoutInflater() ;
            rv = ( TextView )inflater.inflate( R.layout.spinner_item, null ) ;
        }

        if( adapterType == CAT_LIST_ADAPTER ) {
            itemName = catDAO.getCategoryName( id ) ;
            if( itemName == null ) itemName = "<Unknown Category>" ;
        }
        else {
            itemName = catDAO.getSubCategoryName( id ) ;
            if( itemName == null ) itemName = "<Unknown Sub-Category>" ;
        }
        
        rv.setText( itemName ) ;
        
        return rv ;
    }

    @Override
    public View getDropDownView( int position, View convertView, ViewGroup parent ) {
        return getView( position, convertView, parent ) ;
    }
}
