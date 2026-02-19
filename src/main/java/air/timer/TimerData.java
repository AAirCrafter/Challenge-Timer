package air.timer;

public class TimerData {
    public String name;
    public int time;
    public int defaultTime;
    public String color;
    public String type;
    public Boolean active;

    public Boolean bold;
    public Boolean italic;
    public Boolean underlined;
    public Boolean strikedthrough;
    public Boolean obfuscated;

    public TimerData(String name, int time, String color, String type, Boolean active) {
        this.name = name;
        this.time = time;
        this.defaultTime = time;
        this.color = color;
        this.type = type;
        this.active = active;

        this.bold = false;
        this.italic = false;
        this.underlined = false;
        this.strikedthrough = false;
        this.obfuscated = false;
    }

    @Override
    public String toString() {
        return name + ": " + time + "s";
    }
}
