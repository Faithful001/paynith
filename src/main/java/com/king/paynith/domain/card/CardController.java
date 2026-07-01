package com.king.paynith.domain.card;

import com.king.paynith.common.response.Response;
import com.king.paynith.common.util.AuthUtil;
import com.king.paynith.domain.card.dto.ConfirmCardDto;
import com.king.paynith.domain.card.dto.DirectCardChargeDto;
import com.king.paynith.domain.card.dto.CardResponse;
import com.king.paynith.domain.card.entity.Card;
import com.king.paynith.domain.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final AuthUtil authUtil;
    private final CardService cardService;

    public CardController(
            AuthUtil authUtil,
            CardService cardService
    ){
        this.authUtil = authUtil;
        this.cardService = cardService;
    }

    @PostMapping("/direct-charge")
    public ResponseEntity<Object> directCardCharge(
            @RequestBody DirectCardChargeDto payload
    ) {
        String userId = authUtil.getAuthUserId();

        Object response = cardService.initiateDirectCardCharge(payload, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Response<Card>> confirmCardLinking(
            @RequestBody ConfirmCardDto payload
    ) {
        User user = authUtil.getAuthUser();
        Card card = cardService.confirmAndSaveCard(payload.txRef(), user);
        return ResponseEntity.ok(Response.success("Card linked", card));
    }

    @GetMapping
    public ResponseEntity<Response<List<CardResponse>>> getMyCards() {
        String userId = authUtil.getAuthUserId();
        List<CardResponse> cards = cardService.getUserCards(userId);
        return ResponseEntity.ok(Response.success("All cards", cards));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Response<CardResponse>> setDefaultCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        Card card = cardService.setDefaultCard(userId, id);
        return ResponseEntity.ok(Response.success(
                "Card set successfully",
                CardResponse.fromEntity(card))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        cardService.deleteCard(userId, id);
        return ResponseEntity.noContent().build();
    }
}