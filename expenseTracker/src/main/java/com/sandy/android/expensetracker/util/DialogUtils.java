package com.sandy.android.expensetracker.util;

import android.app.AlertDialog ;
import android.content.Context ;
import android.content.DialogInterface ;

/**
 * This utility class contains methods for ease of dialog display.
 *  
 * @author Sandeep Deb
 */
public class DialogUtils {

    public static void showMsgDialog( Context ctx, int msgResourceId ) {

        AlertDialog.Builder builder = new AlertDialog.Builder( ctx, AlertDialog.THEME_HOLO_DARK ) ;
        builder.setMessage( msgResourceId ) ;
        builder.setTitle( "Message" ) ;
        builder.setPositiveButton( "Ok", new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int which ) {
                dialog.dismiss() ;
            }
         } ) ;
        builder.create().show() ;
    }
}
