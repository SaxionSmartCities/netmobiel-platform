package eu.netmobiel.commons.report;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

/**
 * This class converts between Instant en LocalDate string.
 * 
 * !!!!!!!!!!   Jaap: Doesn't work as expected. Save forlater use.  !!!!!!!!!!!!!!
 *
 * @param <T> Type of the bean to be manipulated
 * @param <I> Type of the index into multivalued fields
 * 
 * @author Jaap Reitsma
 */
public class ConvertInstantToLocalDate<T, I> extends AbstractBeanField<T, I> {
    
    /**
     * Converts text into an Instant. Not implemented.
     *
     * @param value String that should represent a Date.
     * @return Instant
     * @throws CsvDataTypeMismatchException   If anything other than the explicitly translated pairs is found
     */
    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        throw new UnsupportedOperationException("Not implemented");
    }
    
    /**
     * This method takes the current value of the field in question in the bean
     * passed in and converts it to a string.
     * 
     * @return the local date as a string
     * @throws CsvDataTypeMismatchException If the field is not a {@code Instant}. 
     */
    @Override
    protected String convertToWrite(Object value) throws CsvDataTypeMismatchException {
        String result = "";
        try {
            if (value != null) {
                LocalDate ld = ((Instant) value).atZone(ZoneId.of("Europe/Amsterdam")).toLocalDate();
                result = DateTimeFormatter.ISO_DATE.format(ld);
            }
        }
        catch(ClassCastException e) {
            CsvDataTypeMismatchException csve =
                    new CsvDataTypeMismatchException("Cannot convert object to local date string");
            csve.initCause(e);
            throw csve;
        }
        return result;
    }

}
