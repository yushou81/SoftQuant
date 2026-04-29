package sample.cfg;

class RefundWorkflow {
    public String resolve(String orderState, boolean damaged, boolean opened) {
        switch (orderState) {
            case "PENDING":
                return "WAIT";
            case "SHIPPED":
                if (damaged) {
                    return "EXPEDITE";
                }
                return "TRACK";
            case "DELIVERED":
                if (opened && damaged) {
                    return "MANUAL_CHECK";
                }
                if (opened) {
                    return "PARTIAL_REFUND";
                }
                return "FULL_REFUND";
            default:
                return "UNSUPPORTED";
        }
    }
}
