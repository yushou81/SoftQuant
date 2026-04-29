package sample.cfg;

class DiscountPolicy {
    public int calculateDiscount(int amount, boolean vipCustomer, boolean festivalSeason) {
        int rate = 0;

        if (vipCustomer) {
            rate += 10;
        }

        if (festivalSeason) {
            rate += 5;
        }

        if (amount >= 5000) {
            rate += 8;
        } else if (amount >= 2000) {
            rate += 4;
        }

        return Math.min(rate, 20);
    }
}
