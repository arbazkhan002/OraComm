package com.gogreen.greenmachine.util;

import com.parse.ParseException;
import com.parse.ParseObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by arbkhan on 5/1/2015.
 */

public class Utils {
    private static Utils singleton = new Utils();
    /* Static 'instance' method */
    public static Utils getInstance( ) {
        return singleton;
    }
    private Date convertToDateObject(String s) {
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd h:m a");
        Calendar cal = Calendar.getInstance();
        String input = cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DATE)+" " +s;

        Date t = new Date();

        try {
            t = ft.parse(input);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static void fetchParseObject(ParseObject p){
        try{
            p.fetchIfNeeded();
        }
        catch (ParseException e){
            e.printStackTrace();
        };

    }
}
