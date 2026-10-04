"use client";

import { useAuthStore } from "../store/auth.store";
import { RegisterForm } from "./register-form";
import { VerifyOtpForm } from "./verify-otp-form";
import { AuthHeader } from "./auth-header";

export function RegisterView() {
    const step = useAuthStore((state) => state.registrationStep);

    return (
        <>
            <AuthHeader
                title={step === "register" ? "Mở tài khoản Ví điện tử" : "Xác thực mã OTP Email"}
                description={
                    step === "register"
                        ? "Điền thông tin định danh để tạo ví bảo mật"
                        : "Nhập mã số xác thực được gửi đến hộp thư của bạn"
                }
            />
            {step === "register" ? <RegisterForm /> : <VerifyOtpForm />}
        </>
    );
}
