package com.king.paysim.core.kafka;

import com.king.paysim.domain.wallet.WalletService;
import com.king.paysim.domain.wallet.dtos.CreateWalletDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageConsumer {
    private final WalletService walletService;

    public MessageConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = "wallet.create", groupId = "wallet-service")
    public void consumeWalletTopic(String message){
        String userId = message.replace("\"", "");
        try{
            CreateWalletDto payload = new CreateWalletDto(userId);
            this.walletService.create(payload);
        } catch (Exception err){
            log.info("Consume wallet topic error", err);
        }
    }
}
