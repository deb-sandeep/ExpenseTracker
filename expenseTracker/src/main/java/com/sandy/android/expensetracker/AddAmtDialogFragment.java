package com.sandy.android.expensetracker ;

import android.app.Activity ;
import android.app.AlertDialog ;
import android.app.Dialog ;
import android.app.DialogFragment ;
import android.content.DialogInterface ;
import android.os.Bundle ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.WindowManager.LayoutParams ;
import android.widget.EditText ;

/**
 * This class represents a dialog fragment to add an amount to an existing
 * expense item. This dialog is shown when a user opens a context menu on an 
 * expense item and chooses the add expense menu item.
 * 
 * Once the user adds an amount and the amount is not equal to zero (note that
 * the amount can be negative), a call back method onAddAmtDialogAmtSet
 * method is called on the AddAmtDialogFragmentListener attached to his
 * class instance.
 * 
 * @author Sandeep Deb
 */
public class AddAmtDialogFragment extends DialogFragment
    implements DialogInterface.OnClickListener {

    /**
     * The interface which needs to be implemented by a class which wants to
     * get notified when the amount entered is not equal to zero and the 
     * dialog is not cancelled by the user. 
     */
    public static interface AddAmtDialogFragmentListener {
        public void amountAdded( int amt ) ;
    }
    
    private EditText amtTextBox = null ;
    private Activity parentActivity = null ;
    private AddAmtDialogFragmentListener listener = null ;

    /**
     * This method is called when the dialog is being prepared for opening. 
     * We extract the references of the activity and keep it with us for use
     * later on.
     */
    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity ) ;
        this.listener = ( AddAmtDialogFragmentListener )activity ;
        this.parentActivity = activity ;
    }
    
    /**
     * This method is called before the dialog is made visible. We set up the 
     * layout and get references to the embedded views here.
     */
    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        
        AlertDialog dialog = null ;
        AlertDialog.Builder builder = new AlertDialog.Builder( parentActivity, 
                                                 AlertDialog.THEME_HOLO_DARK ) ;
        LayoutInflater inflater = parentActivity.getLayoutInflater() ;
        
        View view = inflater.inflate( R.layout.popup_add_amount, null ) ;
        builder.setView( view ) ;
        builder.setPositiveButton( "OK", this ) ;
        builder.setNegativeButton( "Cancel", this ) ;
        builder.setTitle( "Add amount to expense" ) ;
        
        dialog = builder.create() ;
        dialog.getWindow().setSoftInputMode( LayoutParams.SOFT_INPUT_STATE_VISIBLE ) ;
        dialog.setCancelable( false ) ;
        dialog.setCanceledOnTouchOutside( false ) ;

        // Extract the view references 
        this.amtTextBox = ( EditText )view.findViewById( R.id.popupAmtEditText ) ;
        this.amtTextBox.setTextColor( 0xFFFFFFFF ) ;
        this.amtTextBox.requestFocus() ;
        
        return dialog ;
    }

    /**
     * This is called when either of the Ok or Cancel buttons on the dialog is
     * pressed. If the OK button is pressed, we check if the amount entered is
     * not zero and if so, we call on the listener for processing.
     */
    @Override
    public void onClick( DialogInterface dialog, int which ) {
        
        if( which == DialogInterface.BUTTON_POSITIVE ) {
            String text = amtTextBox.getText().toString() ;
            text = ( text == null || text.trim().equals( "" ) ) ? "0" : text.trim() ;
            
            int amt = Integer.parseInt( text ) ;
            if( amt != 0 ) {
                listener.amountAdded( amt ) ;
            }
        }
        
        // Dismiss the dialog in all cases so that we can go back to the 
        // previous activity.
        dismiss() ;
    }
}
