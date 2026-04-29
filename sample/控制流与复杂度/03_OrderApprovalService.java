package sample.cfg;

class OrderApprovalService {
    public ApprovalResult approve(OrderRequest request) {
        if (request == null) {
            return ApprovalResult.REJECTED;
        }

        if (request.amount() > 10000) {
            if (request.vipCustomer()) {
                return ApprovalResult.MANAGER_REVIEW;
            }
            if (request.overdueInvoices() > 0) {
                return ApprovalResult.REJECTED;
            }
            return ApprovalResult.FINANCE_REVIEW;
        }

        if (request.amount() > 3000 && request.overdueInvoices() == 0) {
            return ApprovalResult.AUTO_APPROVED;
        }

        return ApprovalResult.MANUAL_REVIEW;
    }

    public enum ApprovalResult {
        AUTO_APPROVED,
        MANUAL_REVIEW,
        MANAGER_REVIEW,
        FINANCE_REVIEW,
        REJECTED
    }

    public record OrderRequest(int amount, boolean vipCustomer, int overdueInvoices) {}
}
