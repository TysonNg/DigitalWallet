package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.wallet.CreateWalletRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.wallet.WalletResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;

    public WalletController(CreateWalletUseCase createWalletUseCase) {
        this.createWalletUseCase = createWalletUseCase;
    }

    @PostMapping
    public ResponseEntity<WalletResponseDto> create(@RequestBody CreateWalletRequestDto createWalletRequestDto){
        WalletResponse response = createWalletUseCase.createWallet(createWalletRequestDto.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponseDto.from(response));
    }
}
