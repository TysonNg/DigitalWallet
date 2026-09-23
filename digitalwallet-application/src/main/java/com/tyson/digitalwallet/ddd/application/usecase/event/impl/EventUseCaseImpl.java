package com.tyson.digitalwallet.ddd.application.usecase.event.impl;

import com.tyson.digitalwallet.ddd.application.usecase.event.EventUseCase;
import org.springframework.stereotype.Service;

@Service
public class EventUseCaseImpl implements EventUseCase {
    @Override
    public String sayHi(String who) {
        return who + " Application";
    }
}
