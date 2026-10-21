package com.tyson.digitalwallet.ddd.infrastructure.ai.service;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class TransferIntentParserTest {
    @Test
    public void acceptsVietnameseMoneyAndKeepsRecipientAndNote() {
        for (String amount : new String[]{"50.000đ", "50,000 VND", "50k", "50 nghìn", "50000"}) {
            var result = TransferIntentParser.parse("Chuyển " + amount + " cho Nam nội dung ăn trưa");
            assertNotNull(amount, result);
            assertEquals(amount, 0, result.amount().compareTo(BigDecimal.valueOf(50000)));
            assertEquals("Nam", result.recipient());
            assertEquals("ăn trưa", result.note());
        }
        assertEquals(0, TransferIntentParser.parse("Chuyển 1,5 triệu cho Nam").amount().compareTo(BigDecimal.valueOf(1500000)));
    }

    @Test
    public void ambiguousAmountsAndPhoneNumbersDoNotBecomePayments() {
        assertNull(TransferIntentParser.parse("Chuyển cho 0901234567 số tiền 50k"));
        assertNull(TransferIntentParser.parse("Phí chuyển 50k cho Nam là bao nhiêu?"));
        for (String amount : new String[]{"50.00đ", "1,2,3 VND", "0đ", "999999999999999999999999đ"}) {
            assertThrows(amount, IllegalArgumentException.class,
                    () -> TransferIntentParser.parse("Chuyển " + amount + " cho Nam"));
        }
    }
}
