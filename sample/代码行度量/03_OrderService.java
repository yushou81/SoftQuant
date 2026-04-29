package sample.loc.project;

public class OrderService {

    public int score(String level, int count) {
        if ("VIP".equals(level)) {
            return count * 3;
        }
        return count;
    }
}
