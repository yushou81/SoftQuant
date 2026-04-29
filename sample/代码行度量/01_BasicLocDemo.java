package sample.loc;

// BasicLocDemo: used to verify blank/comment/mixed/code/logical counting.
public class BasicLocDemo {

    private int total = 0; // mixed line

    /*
     * Multi-line block comment.
     * These lines should count as pure comments.
     */
    public int add(int left, int right) {
        int sum = left + right;
        total = sum;
        return sum;
    }

    public void reset() {
        total = 0;
    }
}
