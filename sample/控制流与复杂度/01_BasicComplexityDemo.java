package sample.cfg;

class BasicComplexityDemo {
    public int straightLine(int value) {
        int result = value + 1;
        return result;
    }

    public int singleIf(int value) {
        if (value > 0) {
            return value;
        }
        return -value;
    }

    public int loopAndBranch(int value) {
        int sum = 0;
        for (int i = 0; i < value; i++) {
            if (i % 2 == 0) {
                sum += i;
            }
        }
        return sum;
    }
}
