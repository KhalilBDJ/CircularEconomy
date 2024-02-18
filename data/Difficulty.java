package handsOn.circularEconomy.data;

public enum Difficulty {
    LIGHT(0),
    EASY(1),
    AVERAGE(2),
    DIFFICULT(3),
    DEFINITIVE(4);

    private final int value;

    private Difficulty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
