package com.sandy.android.expensetracker.util ;

/**
 * The <code>CSVQuoter</code> is a helper class to encode a string for the CSV
 * file format.
 * 
 * @author Thomas Morgner.
 */
public class CSVQuoter {
    
    /** The separator used in the CSV file. */
    private char separator ;
    
    /** The quoting character or a single quote. */
    private char quate ;
    
    /** The double quote. This is a string containing the quate two times. */
    private String doubleQuate ;

    /**
     * Creates a new CSVQuoter, which uses a comma as the default separator.
     */
    public CSVQuoter() {
        this( ',', '"' ) ;
    }

    /**
     * Creates a new CSVQuoter with the given separator and quoting character.
     * 
     * @param separator the separator
     * @param quate the quoting character
     */
    public CSVQuoter(final char separator, final char quate) {
        this.separator = separator ;
        this.quate = quate ;
        this.doubleQuate = String.valueOf( quate ) + quate ;
    }

    /**
     * Encodes the string, so that the string can safely be used in CSV files.
     * If the string does not need quoting, the original string is returned
     * unchanged.
     * 
     * @param original the unquoted string.
     * @return The quoted string
     */
    public String doQuoting( final String original ) {
        
        final StringBuffer retval = new StringBuffer() ;
        retval.append( quate ) ;
        applyQuote( retval, original ) ;
        retval.append( quate ) ;
        return retval.toString() ;
    }

    /**
     * Decodes the string, so that all escape sequences get removed. If the
     * string was not quoted, then the string is returned unchanged.
     * 
     * @param nativeString the quoted string.
     * @return The unquoted string.
     */
    public String undoQuoting( final String nativeString ) {
        
        final StringBuffer b = new StringBuffer( nativeString.length() ) ;
        final int length = nativeString.length() - 1 ;
        int start = 1 ;

        int pos = start ;
        while (pos != -1) {
            pos = nativeString.indexOf( doubleQuate, start ) ;
            if (pos == -1) {
                b.append( nativeString.substring( start, length ) ) ;
            } 
            else {
                b.append( nativeString.substring( start, pos ) ) ;
                start = pos + 1 ;
            }
        }
        return b.toString() ;
    }

    /**
     * Applies the quoting to a given string, and stores the result in the
     * StringBuffer <code>b</code>.
     * 
     * @param b the result buffer
     * @param original the string, that should be quoted.
     */
    private void applyQuote( final StringBuffer b, final String original ) {
        // This solution needs improvements. Copy blocks instead of single
        // characters.
        final int length = original.length() ;

        for (int i = 0; i < length; i++) {
            final char c = original.charAt( i ) ;
            if( c == quate ) {
                b.append( doubleQuate ) ;
            } 
            else {
                b.append( c ) ;
            }
        }
    }

    /**
     * Gets the separator used in this quoter and the CSV file.
     * 
     * @return the separator (never <code>null</code>).
     */
    public char getSeparator() {
        return separator ;
    }

    /**
     * Returns the quoting character.
     * 
     * @return the quote character.
     */
    public char getQuate() {
        return quate ;
    }
}
