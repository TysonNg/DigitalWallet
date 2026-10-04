import { z } from "zod";

export const loginSchema = z.object({
    email: z.string().email("Địa chỉ email không hợp lệ"),
    password: z.string().min(6, "Mật khẩu tối thiểu 6 ký tự"),
});

export type LoginInput = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
    fullName: z.string().min(2, "Họ và tên tối thiểu 2 ký tự").max(100, "Họ và tên quá dài"),
    email: z.string().email("Địa chỉ email không hợp lệ"),
    phoneNumber: z
        .string()
        .regex(/^(0|\+84)[3|5|7|8|9][0-9]{8}$/, "Số điện thoại Việt Nam không hợp lệ"),
    password: z
        .string()
        .min(8, "Mật khẩu tối thiểu 8 ký tự")
        .regex(/[A-Z]/, "Mật khẩu cần ít nhất 1 chữ hoa")
        .regex(/[0-9]/, "Mật khẩu cần ít nhất 1 chữ số"),
    dateOfBirth: z.string().refine(
        (dob) => {
            const birth = new Date(dob);
            const now = new Date();
            const age = now.getFullYear() - birth.getFullYear();
            return age >= 18;
        },
        { message: "Người dùng phải đủ 18 tuổi để mở ví điện tử" }
    ),
});

export type RegisterInput = z.infer<typeof registerSchema>;

export const verifyOtpSchema = z.object({
    email: z.string().email(),
    otp: z
        .string()
        .length(6, "Mã OTP phải đúng 6 chữ số")
        .regex(/^[0-9]+$/, "Mã OTP chỉ bao gồm chữ số"),
});

export type VerifyOtpInput = z.infer<typeof verifyOtpSchema>;
