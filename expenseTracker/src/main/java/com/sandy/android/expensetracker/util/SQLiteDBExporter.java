// =============================================================================
//
// All information contained herein is, and remains the property of 
// Sandeep Deb (deb.sandeep@gmail.com). Dissemination of this information or 
// reproduction of this material is strictly forbidden unless prior written 
// permission is obtained from Sandeep Deb
//
// =============================================================================

package com.sandy.android.expensetracker.util;
import java.io.File ;
import java.io.FileInputStream ;
import java.io.FileOutputStream ;
import java.nio.channels.FileChannel ;

import android.annotation.SuppressLint ;
import android.os.Environment ;
import android.util.Log ;

/**
 * This utility class is used to export the SQLite database from the private
 * directory to a public directory. This is required because for non rooted
 * files the access to /data/data directory is restricted and hence if we have
 * to analyze the database, we need to get it exported first and then either
 * use ADB or load it in SQLite studio.
 * 
 * @author Sandeep Deb
 */
public class SQLiteDBExporter {

    @SuppressLint("SdCardPath")
    private static final String DATA_DIR = "/data/data/" ;
    private static final String DB_DIR   = "/databases/" ;
    private static final String LOG_TAG  = "SQLLiteDBExporter" ;
    
    private File currentDB = null ;
    private File backupDB  = null ;
    
    /** 
     * Constructor. 
     * 
     * @param pkgName The application package name. Usually obtained from 
     *        Context.getPackageName()
     * 
     * @param dbName The name of the SQLite database
     * 
     * @param pubDir The directory in the Environment.getExternalStorageDirectory()
     *        where the database needs to be exported. This can be null, in
     *        which case the database will be exported in the root public
     *        folder. 
     */
    public SQLiteDBExporter( String pkgName, String dbName, String pubDir ) {
        
        String backupDBPath = null ;
        
        if( pubDir == null ) {
            backupDBPath = dbName + ".db" ;
        }
        else {
            backupDBPath = pubDir + "/" + dbName + ".db" ;
        }
        
        currentDB = new File( DATA_DIR + pkgName + DB_DIR + dbName ) ;
        backupDB  = new File( Environment.getExternalStorageDirectory(), backupDBPath ) ;
    }
    
    /** 
     * This function exports the sqlite database into a public folder for analysis. 
     * 
     * @return null if the export is a success. A not null value would be the
     *         reason why the export failed.
     */
    @SuppressLint("SdCardPath")
    public String exportDatabase() {
        
        String msg = null ;
        
        FileChannel src = null ;
        FileChannel dst = null ;
        
        try {
            Log.d( LOG_TAG, "Exporting database" ) ;
            Log.d( LOG_TAG, "  Source -> " + currentDB.getAbsolutePath() ) ;
            Log.d( LOG_TAG, "  Dest   -> " + backupDB.getAbsolutePath() ) ;
            
            if( Environment.getExternalStorageDirectory().canWrite() ) {
                
                if (currentDB.exists()) {
                    
                    src = new FileInputStream( currentDB ).getChannel() ;
                    dst = new FileOutputStream( backupDB ).getChannel() ;
                    dst.transferFrom( src, 0, src.size() ) ;
                    src.close() ;
                    dst.close() ;
                }
                else {
                    Log.e( LOG_TAG, "Opps, source file does not exist" ) ;
                    msg = "Database missing" ;
                }
            }
            else {
                Log.e( LOG_TAG, "Opps, don't have write access to external storage" ) ;
                msg = "Write access missing" ;
            }
        } 
        catch ( Exception e ) {
            Log.e( LOG_TAG, "Opps, exception while exporting database", e ) ;
            msg = "Exception - " + e.getMessage() ;
        }
        
        return msg ;
    }
}
