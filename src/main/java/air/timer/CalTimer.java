package air.timer;

import java.util.ArrayList;
import java.util.regex.*;

public class CalTimer {
    public static String toTime(int timeins) {
        ArrayList<String> time = new ArrayList<>();

        int days = timeins / 86400;
        int hours = (timeins % 86400) / 3600;
        int minutes = (timeins % 3600) / 60;
        int seconds = timeins % 60;

        if (days != 0) time.add(days + "d");
        if (hours != 0) time.add(hours + "h");
        if (minutes != 0) time.add(minutes + "m");
        if (seconds != 0) time.add(seconds + "s");

        return String.join(" ",time);
    }

    public static int toSeconds(String timestr) {
        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(timestr);

        int days = 0, hours = 0, mins = 0, secs = 0;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 'd' -> days = value;
                case 'h' -> hours = value;
                case 'm' -> mins = value;
                case 's' -> secs = value;
            }
        }

        return days + hours + mins + secs;
    }
}