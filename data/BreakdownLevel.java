package handsOn.circularEconomy.data;

public enum BreakdownLevel {
    LIGHT(0),
    EASY(1),
    AVERAGE(2),
    DIFFICULT(3),
    DEFINITIVE(4);

    private final int level;

    BreakdownLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }
}
