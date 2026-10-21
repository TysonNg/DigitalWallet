package com.tyson.digitalwallet.ddd.application.port.out.ai;

import java.util.List;

public interface VectorKnowledgePort {
    /**
     * Tìm kiếm các đoạn tài liệu chính sách liên quan nhất với câu hỏi
     * @param query Câu hỏi của người dùng
     * @param topK Số lượng kết quả tối đa cần lấy
     * @return Danh sách các đoạn văn bản chính sách khớp ngữ nghĩa
     */
    List<String> findRelevantPolicies(String query, int topK);
}
