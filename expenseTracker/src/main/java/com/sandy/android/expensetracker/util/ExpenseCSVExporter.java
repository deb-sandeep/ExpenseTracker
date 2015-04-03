package com.sandy.android.expensetracker.util;

import java.io.BufferedWriter ;
import java.io.File ;
import java.io.FileOutputStream ;
import java.io.IOException ;
import java.io.OutputStreamWriter ;
import java.text.SimpleDateFormat ;
import java.util.Calendar ;
import java.util.List ;
import java.util.Locale ;

import android.os.Environment ;
import android.util.Log ;

import com.sandy.android.expensetracker.db.CategoryDAO ;
import com.sandy.android.expensetracker.db.DAOManager ;
import com.sandy.android.expensetracker.vo.ExpenseItem ;

/**
 * This utility class is used to export the expense data to a CSV file in the
 * ExpenseData directory. 
 * 
 * @author 125806
 */
public class ExpenseCSVExporter {

    private static final String EOL = System.getProperty( "line.separator" ) ;
    private static final SimpleDateFormat SDF = new SimpleDateFormat( "MM/dd/yyyy", Locale.US ) ;
    
    private CSVQuoter   quoter = new CSVQuoter() ;
    private CategoryDAO catDAO = null ;
    
    /** Public not argument constructor. */
    public ExpenseCSVExporter() {
        catDAO = DAOManager.getInstance().getCategoryDAO() ;
    }
    
    /**
     * Exports the list of expense items into the ExpenseTracker folder 
     * as a CSV file stamped with the current date.
     * 
     * @param expenseItems A list of all expense items stored in the database
     *        which have not been exported.
     *        
     * @return The name of the file written.
     */
    public String exportExpenseItems( List<ExpenseItem> expenseItems ) {
        
        // Get the directory in which we will export the file. If the directory
        // does not exist, create it
        File dir = new File( Environment.getExternalStorageDirectory(), "ExpenseTracker" ) ;
        if( !dir.exists() ) {
            dir.mkdirs() ;
        }

        String fileName = getExportFileName() ;
        
        FileOutputStream fOut   = null ;
        BufferedWriter   writer = null ;
        
        try {
            fOut   = new FileOutputStream( new File( dir, fileName ) ) ;
            writer = new BufferedWriter( new OutputStreamWriter( fOut ) ) ;
            
            for( ExpenseItem item : expenseItems ) {
                writeExpenseItem( item, writer ) ;
                writer.append( EOL ) ;
            }
            
            writer.flush() ;
        } 
        catch( Exception e ) {
            fileName = null ;
            Log.e( LogTag.CSV_EXPORTER, "Error exporting file", e ) ;
        }
        finally {
            if( writer != null ) {
                try {
                    writer.close() ;
                } 
                catch (IOException e) {
                    Log.e( LogTag.CSV_EXPORTER, "Error closing CSV writer", e ) ;
                }
            }
        }
        
        return fileName ;
    }
    
    /**
     * Writes an expense item by converting into a CSV encoded item row. The
     * columns that would be exported are
     * 
     * 1. date in mm/dd/yyyy format
     * 2. category
     * 3. paid by
     * 4. sub category
     * 5. amount
     * 6. description
     */
    private void writeExpenseItem( ExpenseItem item, BufferedWriter writer ) 
        throws Exception {

        String catName = null ;
        String subCatName = null ;
        
        catName = catDAO.getCategoryName( item.getCatId() ) ;
        if( catName == null ) catName = "<Unknown Category>" ;
        
        subCatName = catDAO.getSubCategoryName( item.getSubCatId() ) ;
        if( subCatName == null ) subCatName = "<Unknown Sub-Category>" ;
        
        StringBuffer buffer = new StringBuffer() ;
        
        buffer.append( quoter.doQuoting( SDF.format( item.getDate() ) ) ) ;
        buffer.append( "," ) ;
        buffer.append( quoter.doQuoting( catName ) ) ;
        buffer.append( "," ) ;
        buffer.append( quoter.doQuoting( item.getPaidBy() ) ) ;
        buffer.append( "," ) ;
        buffer.append( quoter.doQuoting( subCatName ) ) ;
        buffer.append( "," ) ;
        buffer.append( quoter.doQuoting( "" + item.getAmount() ) ) ;
        buffer.append( "," ) ;
        buffer.append( quoter.doQuoting( item.getDescription() ) ) ;
        
        
        writer.write( buffer.toString() ) ;
    }
    
    /** Returns the name of the exported file based on the current date. */
    private String getExportFileName() {
        
        Calendar cal = Calendar.getInstance() ;
        StringBuffer buffer = new StringBuffer( "ExpenseLog-" ) ;
        buffer.append( cal.get( Calendar.DAY_OF_MONTH ) ).append( "-" ) ;
        buffer.append( cal.get( Calendar.MONTH ) + 1 ).append( "-" ) ;
        buffer.append( cal.get( Calendar.YEAR ) ).append( ".csv" ) ;
        
        return buffer.toString() ;
    }
}
