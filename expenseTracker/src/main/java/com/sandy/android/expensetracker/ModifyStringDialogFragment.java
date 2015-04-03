package com.sandy.android.expensetracker ;

import com.sandy.android.expensetracker.util.DialogUtils ;

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
 * This class represents a dialog fragment to modify and return an input string.
 * 
 * This fragment expects the following arguments:
 * 
 * id - The identifier which will be returned in the call to the listener
 * inputText - The input text. Can be null.
 * 
 * 
 * @author Sandeep Deb
 */
public class ModifyStringDialogFragment extends DialogFragment
    implements DialogInterface.OnClickListener {

    /**
     * The interface which needs to be implemented by a class which wants to
     * get notified when the string is modified and the dialog is not cancelled 
     * by the user. 
     */
    public static interface ModifyStringDialogFragmentListener {
        public void stringModified( int id, String modifiedText ) ;
    }
    
    private int      id = 0 ;
    private String   originalText = null ;
    private EditText textBox = null ;
    private Activity parentActivity = null ;
    private ModifyStringDialogFragmentListener listener = null ;

    /**
     * This method is called when the dialog is being prepared for opening. 
     * We extract the references of the activity and keep it with us for use
     * later on.
     * 
     * If it's found that the activity implements ModifyStringDialogFragmentListener 
     * interface it is added as a listener.
     */
    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity ) ;
        this.parentActivity = activity ;
        
        if( activity instanceof ModifyStringDialogFragmentListener ) {
            setModifyStringDialogFragmentListener( 
                              ( ModifyStringDialogFragmentListener )activity ) ;
        }
    }
    
    /**
     * Registers a ModifyStringDialogFragmentListener listener with this
     * dialog fragment.
     */
    public void setModifyStringDialogFragmentListener( ModifyStringDialogFragmentListener listener ) {
        this.listener = listener ;
    }
    
    /**
     * This method is called before the dialog is made visible. We set up the 
     * layout and get references to the embedded views here.
     */
    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        
        // Extract the input text if any
        this.id = getArguments().getInt( "id" ) ;
        this.originalText = getArguments().getString( "inputText" ) ;
        this.originalText = ( originalText == null ) ? "" : originalText ;

        // Prepare the dialog.
        AlertDialog dialog = null ;
        AlertDialog.Builder builder = new AlertDialog.Builder( parentActivity, 
                                                 AlertDialog.THEME_HOLO_DARK ) ;
        LayoutInflater inflater = parentActivity.getLayoutInflater() ;
        
        View view = inflater.inflate( R.layout.popup_modify_string, null ) ;
        builder.setView( view ) ;
        builder.setPositiveButton( "OK", this ) ;
        builder.setNegativeButton( "Cancel", this ) ;
        builder.setTitle( "Modify text" ) ;
        
        dialog = builder.create() ;
        dialog.getWindow().setSoftInputMode( LayoutParams.SOFT_INPUT_STATE_VISIBLE ) ;
        dialog.setCancelable( false ) ;
        dialog.setCanceledOnTouchOutside( false ) ;

        // Extract the view references 
        this.textBox = ( EditText )view.findViewById( R.id.popupModifyStringEditText ) ;
        this.textBox.setTextColor( 0xFFFFFFFF ) ;
        this.textBox.requestFocus() ;
        this.textBox.setText( originalText ) ;
        this.textBox.setSelection( originalText.length() ) ;
        
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
            String text = textBox.getText().toString() ;
            if( text == null || text.trim().equals( "" ) ) {
                DialogUtils.showMsgDialog( parentActivity, R.string.msg_null_string ) ;
            }
            else if ( text.equals( originalText ) ) {
                DialogUtils.showMsgDialog( parentActivity, R.string.msg_same_string ) ;
            }
            else {
                listener.stringModified( id, text.trim() ) ;
                dismiss() ;
            }
        }
        else if( which == DialogInterface.BUTTON_NEGATIVE ) {
            dismiss() ;
        }
    }
}
