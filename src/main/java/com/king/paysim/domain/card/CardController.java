package com.king.paysim.domain.card;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.card.dto.ConfirmCardDto;
import com.king.paysim.domain.card.dto.DirectCardChargeDto;
import com.king.paysim.domain.card.dto.LinkedCardResponse;
import com.king.paysim.domain.card.entity.Card;
import com.king.paysim.domain.user.entity.User;
import jakarta.validation.Valid;
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
    public ResponseEntity<Response<List<LinkedCardResponse>>> getMyLinkedCards() {
        String userId = authUtil.getAuthUserId();
        List<LinkedCardResponse> cards = cardService.getUserLinkedCards(userId);
        return ResponseEntity.ok(Response.success("All linked cards", cards));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Response<LinkedCardResponse>> setDefaultCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        Card card = cardService.setDefaultCard(userId, id);
        return ResponseEntity.ok(Response.success(
                "Card set successfully",
                LinkedCardResponse.fromEntity(card))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLinkedCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        cardService.deleteLinkedCard(userId, id);
        return ResponseEntity.noContent().build();
    }
}