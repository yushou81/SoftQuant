package sample.cfg;

class DecisionRecognitionDemo {
    public int evaluate(String type, int score) {
        try {
            if ((score > 90 && score < 100) || "VIP".equals(type)) {
                return 3;
            } else if (score < 0) {
                return -1;
            }

            switch (type) {
                case "A":
                    return score > 60 ? 2 : 1;
                case "B":
                    return 1;
                default:
                    return 0;
            }
        } catch (RuntimeException ex) {
            return -2;
        }
    }

    public boolean needsManualReview(int amount, boolean urgent) {
        while (amount > 0) {
            if (urgent || amount > 5000) {
                return true;
            }
            amount -= 1000;
        }
        return false;
    }
}
