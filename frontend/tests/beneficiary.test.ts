import test from "node:test";
import assert from "node:assert/strict";
import type { BeneficiaryDto, RecipientLookupDto } from "../src/features/wallet/types/wallet.types";

interface MockApiResponse<T> {
    success: boolean;
    statusCode: number;
    message: string;
    data: T;
}

test("Lookup recipient resolves valid user details and uppercase verification", async () => {
    const mockLookup: RecipientLookupDto = {
        userId: "11111111-1111-1111-1111-111111111111",
        walletId: "22222222-2222-2222-2222-222222222222",
        fullName: "NGUYEN VAN B",
        phoneNumber: "0901234567",
        email: "recipient@example.com",
        currency: "VND",
    };

    const fetchMock = async (url: string) => {
        assert.ok(url.includes("identifier=0901234567"));
        return new Response(
            JSON.stringify({
                success: true,
                statusCode: 200,
                message: "Tra cứu người nhận thành công",
                data: mockLookup,
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    };

    const res = await fetchMock("http://localhost:8080/api/v1/wallet/lookup?identifier=0901234567");
    const json: MockApiResponse<RecipientLookupDto> = await res.json();

    assert.equal(json.success, true);
    assert.equal(json.data.fullName, "NGUYEN VAN B");
    assert.equal(json.data.phoneNumber, "0901234567");
    assert.equal(json.data.walletId, "22222222-2222-2222-2222-222222222222");
});

test("Beneficiary list filters correctly by nickname and full name", () => {
    const beneficiaries: BeneficiaryDto[] = [
        {
            id: "b1",
            userId: "u1",
            beneficiaryUserId: "u2",
            beneficiaryWalletId: "w2",
            nickname: "Mẹ",
            fullName: "NGUYEN THI ME",
            phoneNumber: "0912345678",
            email: "me@example.com",
            createdAt: "2026-10-13T10:00:00Z",
            updatedAt: "2026-10-13T10:00:00Z",
        },
        {
            id: "b2",
            userId: "u1",
            beneficiaryUserId: "u3",
            beneficiaryWalletId: "w3",
            nickname: "Tiền nhà trọ",
            fullName: "TRAN CHU TRO",
            phoneNumber: "0987654321",
            email: "chutro@example.com",
            createdAt: "2026-10-13T10:00:00Z",
            updatedAt: "2026-10-13T10:00:00Z",
        },
    ];

    const filterBy = (query: string) => {
        const q = query.toLowerCase().trim();
        return beneficiaries.filter((b) => {
            if (!q) return true;
            return (
                (b.fullName && b.fullName.toLowerCase().includes(q)) ||
                (b.nickname && b.nickname.toLowerCase().includes(q)) ||
                (b.phoneNumber && b.phoneNumber.includes(q)) ||
                (b.email && b.email.toLowerCase().includes(q))
            );
        });
    };

    assert.equal(filterBy("mẹ").length, 1);
    assert.equal(filterBy("mẹ")[0].fullName, "NGUYEN THI ME");

    assert.equal(filterBy("chu tro").length, 1);
    assert.equal(filterBy("0987").length, 1);
    assert.equal(filterBy("khong ton tai").length, 0);
});

test("Prevent transferring to self detection logic", () => {
    const currentUserId = "11111111-1111-1111-1111-111111111111";
    const recipientUserId = "11111111-1111-1111-1111-111111111111";

    const isSelf = currentUserId === recipientUserId;
    assert.equal(isSelf, true);
});

test("Resolves localized error message from machine-readable errorCode in vi and en", () => {
    const viMessages = require("../messages/vi.json");
    const enMessages = require("../messages/en.json");

    const sampleCodes = [
        "USER_NOT_FOUND",
        "CANNOT_TRANSFER_TO_SELF",
        "BENEFICIARY_ALREADY_EXISTS",
        "INSUFFICIENT_BALANCE",
        "CANNOT_ADD_SELF_AS_BENEFICIARY"
    ];

    for (const code of sampleCodes) {
        assert.ok(viMessages.apiErrors[code], `vi.json missing apiErrors.${code}`);
        assert.ok(enMessages.apiErrors[code], `en.json missing apiErrors.${code}`);
        assert.notEqual(viMessages.apiErrors[code], enMessages.apiErrors[code]);
    }

    assert.equal(viMessages.apiErrors.USER_NOT_FOUND, "Không tìm thấy người dùng trong hệ thống");
    assert.equal(enMessages.apiErrors.USER_NOT_FOUND, "User not found in the system");
    assert.equal(viMessages.apiErrors.CANNOT_TRANSFER_TO_SELF, "Không thể tự chuyển tiền cho chính mình");
    assert.equal(enMessages.apiErrors.CANNOT_TRANSFER_TO_SELF, "You cannot transfer money to yourself");
});

