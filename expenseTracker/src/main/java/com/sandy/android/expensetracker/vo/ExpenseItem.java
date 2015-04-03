// =============================================================================
//
// All information contained herein is, and remains the property of 
// Sandeep Deb (deb.sandeep@gmail.com). Dissemination of this information or 
// reproduction of this material is strictly forbidden unless prior written 
// permission is obtained from Sandeep Deb
//
// =============================================================================

package com.sandy.android.expensetracker.vo ;

import java.io.Serializable ;
import java.util.Comparator ;
import java.util.Date ;

/**
 * This class represents an expense item. Instances of this class will be
 * persisted in a relational database.
 * 
 * @author Sandeep Deb
 */
public class ExpenseItem implements Serializable {

    private static final long serialVersionUID = 1L ;

    private int     id               = -1 ;
    private Date    date             = null ;
    private int     catId            = -1 ;
    private int     subCatId         = -1 ;
    private String  paidBy           = null ;
    private int     amount           = 0 ;
    private String  description      = null ;
    
    public ExpenseItem() {
        date = new Date() ;
    }
    
    public void setId( int id ) {
        this.id = id ;
    }

    public int getId() {
        return this.id ;
    }
    
    public Date getDate() {
        return date ;
    }
    
    public void setDate(Date date) {
        this.date = date ;
    }
    
    public int getCatId() {
        return catId ;
    }
    
    public void setCatId( int id ) {
        this.catId = id ;
    }
    
    public int getSubCatId() {
        return subCatId ;
    }
    
    public void setSubCatId( int id ) {
        this.subCatId = id ;
    }
    
    public String getPaidBy() {
        return paidBy ;
    }
    
    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy ;
    }
    
    public int getAmount() {
        return amount ;
    }
    
    public void setAmount(int amount) {
        this.amount = amount ;
    }
    
    public String getDescription() {
        return description == null ? "" : description  ;
    }
    
    public void setDescription(String description) {
        this.description = description ;
    }
    
    public String toString() {
        return getDescription() ;
    }
    
    public void copyDataFrom( ExpenseItem srcItem ) {
        
        setId         ( srcItem.getId()          ) ;
        setDate       ( srcItem.getDate()        ) ;
        setCatId      ( srcItem.getCatId()       ) ;
        setSubCatId   ( srcItem.getSubCatId()    ) ;
        setPaidBy     ( srcItem.getPaidBy()      ) ;
        setAmount     ( srcItem.getAmount()      ) ;
        setDescription( srcItem.getDescription() ) ;
    }
    
    // =========================================================================
    // Expense Item generic comparator
    // =========================================================================
    /**
     * This is the comparator for the expense item instances which helps sort
     * them in the descending order. 
     * 
     * Though the objective seems simple, the logic is convoluted because of 
     * the following facts.
     * 
     * 1. The user only specifies the day mm/dd/yyyy on which the expense 
     *    occurred. During the day, the user might enter many entries and 
     *    expect them to be shown in the descending order of entry time. But 
     *    just by sorting them based on their expense date will not achive this
     *    result.
     *    
     * 2. It would seem logical, that we can alleviate the above challenge by
     *    sorting the expenses in the decreasing order of their identifiers which
     *    are monotonically increasing and auto generated. However, remember
     *    that the user can always change the date of an expense item after
     *    creation, effectively making this logic ineffective.
     *    
     * The optimal solution for this is to do two levels of sorting, first 
     * sort based on date. The most recent date appears on top. If the dates
     * are the same, we sort based on their id, the bigger id appears on the 
     * top.
     */
    public static class ExpenseItemComparator implements Comparator<ExpenseItem> {

        public int compare( ExpenseItem lhs, ExpenseItem rhs ) {
            
            long lhsId = lhs.getId() ;
            long rhsId = rhs.getId() ;
            long lhsDt = lhs.getDate().getTime() ;
            long rhsDt = rhs.getDate().getTime() ;
            
            if( lhsDt < rhsDt ) {
                return 1 ;
            }
            else if( lhsDt > rhsDt ) {
                return -1 ;
            }
            else {
                if( lhsId < rhsId ) {
                    return 1 ;
                }
                else if( lhsId > rhsId ) {
                    return -1 ;
                }
            }
            return 0 ;
        }
    }
}
