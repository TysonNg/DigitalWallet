package com.tyson.digitalwallet.ddd.application.usecase.beneficiary;

import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.AddBeneficiaryCommand;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.UpdateBeneficiaryCommand;
import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.response.BeneficiaryResponse;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryUseCase {
    List<BeneficiaryResponse> getBeneficiaries(UUID userId);

    BeneficiaryResponse addBeneficiary(AddBeneficiaryCommand command);

    BeneficiaryResponse updateBeneficiary(UpdateBeneficiaryCommand command);

    void deleteBeneficiary(UUID beneficiaryId, UUID userId);
}
