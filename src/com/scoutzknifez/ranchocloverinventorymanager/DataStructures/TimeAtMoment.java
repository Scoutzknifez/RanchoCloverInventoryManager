package com.scoutzknifez.ranchocloverinventorymanager.DataStructures;

import lombok.Setter;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor @Getter @Setter
public class TimeAtMoment {
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;

    public TimeAtMoment(long time) {
        Date date = new Date(time);
        DateFormat format = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
        String formatted = format.format(date);
        String[] splits = formatted.split(":");
        try {
            year = Integer.parseInt(splits[0]);
            month = Integer.parseInt(splits[1]);
            day = Integer.parseInt(splits[2]);
            hour = Integer.parseInt(splits[3]);
            minute = Integer.parseInt(splits[4]);
            second = Integer.parseInt(splits[5]);
        } catch (Exception e) {
            System.out.println("Can not properly create TimeAtMoment object.");
        }
    }

    @Override
    public String toString() {
        boolean isPM = false;
        String builder;

        int realHour = getHour() + 1;

        if(getHour() > 12) {
            builder = (getHour() - 12) + ":";
            isPM = true;
        } else {
            builder = (getHour()) + ":";
        }

        if(getMinute() < 10) {
            builder += "0" + getMinute();
        } else {
            builder += getMinute();
        }

        builder += ":";

        if(getSecond() < 10) {
            builder += "0" + getSecond();
        } else {
            builder += getSecond();
        }

        if(isPM) {
            builder += " PM";
        } else {
            builder += " AM";
        }

        return builder;
    }
}
