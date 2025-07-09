package air.dev;

import java.util.ArrayList;

public class CalTimer {

    public static String toTime(int timeins) {
        ArrayList<String> time = new ArrayList<>();

        int days = timeins / 86400;
        int hours = (timeins % 86400) / 3600;
        int minutes = (timeins % 3600) / 60;
        int seconds = timeins % 60;

        if (days != 0) {
            time.add(days + "d");
        }
        if (hours != 0) {
            time.add(hours + "h");
        }
        if (minutes != 0) {
            time.add(minutes + "m");
        }
        if (seconds != 0) {
            time.add(seconds + "s");
        }

        return String.join(" ",time);

    }

    public static int toSeconds(String timestr) {
        int timeins = 0;
        StringBuilder numberBuffer = new StringBuilder();

        for (int i = 0; i < timestr.length(); i++) {
            char c = timestr.charAt(i);

            if (Character.isDigit(c)) {
                numberBuffer.append(c);
            } else {
                if (numberBuffer.isEmpty()) {
                    continue;
                }

                int value = Integer.parseInt(numberBuffer.toString());
                numberBuffer.setLength(0);

                switch (c) {
                    case 'd':
                        timeins += value * 86400;
                        break;
                    case 'h':
                        timeins += value * 3600;
                        break;
                    case 'm':
                        timeins += value * 60;
                        break;
                    case 's':
                        timeins += value;
                        break;
                    default:

                        break;
                }
            }
        }
        return timeins;
    }

}
