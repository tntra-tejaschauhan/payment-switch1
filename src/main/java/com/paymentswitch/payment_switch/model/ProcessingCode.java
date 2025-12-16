package com.paymentswitch.payment_switch.model;

import java.math.BigDecimal;

// Sealed interface for type-safe processing codes
public sealed interface ProcessingCode
        permits ProcessingCode.BalanceInquiry,
        ProcessingCode.Withdrawal,
        ProcessingCode.Purchase,
        ProcessingCode.Transfer,
        ProcessingCode.MiniStatement,
        ProcessingCode.Unknown {

    String code();
    String description();

    record BalanceInquiry(String code) implements ProcessingCode {
        public BalanceInquiry {
            if (code == null || !code.startsWith("31")) {
                throw new IllegalArgumentException("Invalid balance inquiry code");
            }
        }

        @Override
        public String description() {
            return "Balance Inquiry";
        }
    }

    record Withdrawal(String code, BigDecimal amount) implements ProcessingCode {
        public Withdrawal {
            if (code == null || !code.startsWith("01")) {
                throw new IllegalArgumentException("Invalid withdrawal code");
            }
        }

        @Override
        public String description() {
            return "Withdrawal";
        }
    }

    record Purchase(String code, BigDecimal amount) implements ProcessingCode {
        public Purchase {
            if (code == null || !code.startsWith("00")) {
                throw new IllegalArgumentException("Invalid purchase code");
            }
        }

        @Override
        public String description() {
            return "Purchase";
        }
    }

    record Transfer(String code, String fromAccount, String toAccount) implements ProcessingCode {
        public Transfer {
            if (code == null || !code.startsWith("40")) {
                throw new IllegalArgumentException("Invalid transfer code");
            }
        }

        @Override
        public String description() {
            return "Transfer";
        }
    }

    record MiniStatement(String code) implements ProcessingCode {
        public MiniStatement {
            if (code == null || !code.startsWith("38")) {
                throw new IllegalArgumentException("Invalid mini statement code");
            }
        }

        @Override
        public String description() {
            return "Mini Statement";
        }
    }

    record Unknown(String code) implements ProcessingCode {
        @Override
        public String description() {
            return "Unknown Transaction";
        }
    }

    // Factory method to parse processing code
    static ProcessingCode parse(String code, BigDecimal amount) {
        if (code == null || code.length() < 2) {
            return new Unknown(code);
        }

        String prefix = code.substring(0, 2);
        return switch (prefix) {
            case "31" -> new BalanceInquiry(code);
            case "01" -> new Withdrawal(code, amount);
            case "00" -> new Purchase(code, amount);
            case "40" -> new Transfer(code, null, null);
            case "38" -> new MiniStatement(code);
            default -> new Unknown(code);
        };
    }
}