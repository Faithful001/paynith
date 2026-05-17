package com.king.paysim.domain.linkedcard;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.linkedcard.dto.ConfirmCardDto;
import com.king.paysim.domain.linkedcard.dto.DirectCardChargeDto;
import com.king.paysim.domain.linkedcard.dto.LinkedCardResponse;
import com.king.paysim.domain.linkedcard.entity.LinkedCard;
import com.king.paysim.domain.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/linked-cards")
public class LinkedCardController {

    private final AuthUtil authUtil;
    private final LinkedCardService linkedCardService;

    public LinkedCardController(
            AuthUtil authUtil,
            LinkedCardService linkedCardService
    ){
        this.authUtil = authUtil;
        this.linkedCardService = linkedCardService;
    }

    @PostMapping("/direct-charge")
    public ResponseEntity<Object> directCardCharge(
            @RequestBody DirectCardChargeDto payload
    ) {
        String userId = authUtil.getAuthUserId();

        Object response = linkedCardService.initiateDirectCardCharge(payload, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Response<LinkedCard>> confirmCardLinking(
            @RequestBody ConfirmCardDto payload
    ) {
        User user = authUtil.getAuthUser();
        LinkedCard linkedCard = linkedCardService.confirmAndSaveCard(payload.txRef(), user);
        return ResponseEntity.ok(Response.success("Card linked", linkedCard));
    }

    @GetMapping
    public ResponseEntity<Response<List<LinkedCardResponse>>> getMyLinkedCards() {
        String userId = authUtil.getAuthUserId();
        List<LinkedCardResponse> linkedCards = linkedCardService.getUserLinkedCards(userId);
        return ResponseEntity.ok(Response.success("All linked cards", linkedCards));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Response<LinkedCardResponse>> setDefaultCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        LinkedCard card = linkedCardService.setDefaultCard(userId, id);
        return ResponseEntity.ok(Response.success("Card set successfully", LinkedCardResponse.fromEntity(card)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLinkedCard(
            @PathVariable String id
    ) {
        String userId = authUtil.getAuthUserId();
        linkedCardService.deleteLinkedCard(userId, id);
        return ResponseEntity.noContent().build();
    }
}