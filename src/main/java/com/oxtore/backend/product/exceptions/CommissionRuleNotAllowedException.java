package com.oxtore.backend.product.exceptions;

public class CommissionRuleNotAllowedException extends RuntimeException {

    public CommissionRuleNotAllowedException() {
        super("Commission rule is not allowed. It is only permitted when SaleType=WHOLESALE and TransactionMode=CONSIGNMENT");
    }

    public CommissionRuleNotAllowedException(String message) {
        super(message);
    }
}