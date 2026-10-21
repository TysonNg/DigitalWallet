package com.tyson.digitalwallet.ddd.infrastructure.ai.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Conservative offline grammar: amount must follow the transfer verb, never a phone number. */
final class TransferIntentParser {
    private static final Pattern INTENT = Pattern.compile(
            "(?iu)^\\s*(?:chuyển|chuyen|gửi|gui)(?:\\s+(?:tiền|tien|khoản|khoan))?\\s+"
            + "([0-9][0-9.,]*\\s*(?:triệu|trieu|nghìn|nghin|ngàn|ngan|tr|k|vnd|vnđ|đ)?)"
            + "\\s+(?:cho|tới|toi|đến|den)\\s+(.+?)"
            + "(?:\\s+(?:với nội dung|voi noi dung|nội dung|noi dung|lời nhắn|loi nhan)\\s+(.+))?\\s*$");

    record Transfer(BigDecimal amount, String recipient, String note) {}

    static Transfer parse(String query) {
        var matcher = INTENT.matcher(query);
        if (!matcher.matches()) return null;
        return new Transfer(parseAmount(matcher.group(1)), matcher.group(2).trim(),
                matcher.group(3) == null ? "Chuyển tiền qua trợ lý AI" : matcher.group(3).trim());
    }

    static BigDecimal parseAmount(String input) {
        String value = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replaceAll("\\s+", "");
        var matcher = Pattern.compile("^([0-9][0-9.,]*)(trieu|tr|nghin|ngan|k|vnd|d)?$").matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid amount");
        String number = matcher.group(1);
        String unit = matcher.group(2) == null ? "" : matcher.group(2);
        boolean scaled = Set.of("trieu", "tr", "nghin", "ngan", "k").contains(unit);
        if (number.matches("[1-9][0-9]{0,2}(?:\\.[0-9]{3})+") || number.matches("[1-9][0-9]{0,2}(?:,[0-9]{3})+")) {
            number = number.replace(".", "").replace(",", "");
        } else if (scaled && number.matches("[0-9]+(?:[.,][0-9]{1,2})?")) {
            number = number.replace(',', '.');
        } else if (!number.matches("[0-9]+")) throw new IllegalArgumentException("Ambiguous amount");
        BigDecimal multiplier = unit.startsWith("tr") ? BigDecimal.valueOf(1000000)
                : scaled ? BigDecimal.valueOf(1000) : BigDecimal.ONE;
        BigDecimal amount = new BigDecimal(number).multiply(multiplier).stripTrailingZeros();
        if (amount.signum() <= 0 || amount.scale() > 0 || amount.compareTo(BigDecimal.valueOf(9007199254740991L)) > 0)
            throw new IllegalArgumentException("Invalid VND amount");
        return amount;
    }
}
