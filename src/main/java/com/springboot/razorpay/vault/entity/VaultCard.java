package com.springboot.razorpay.vault.entity;

import com.springboot.razorpay.common.entity.BaseEntity;
import com.springboot.razorpay.common.enums.CardBrand;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vault_card")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VaultCard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Column(length = 6, nullable = false)
    private String bin; // First 6 digits of the card PAN.

    @Column(nullable = false)
    private byte[] encryptedPan;

    @Column(nullable = false)
    private byte[] encryptedDek; // DEK is used to encrypt/decrypt PAN.
    // Flow:
    // 1) Generate random DEK.
    // 2) encrypted_pan = encrypt(PAN, DEK)
    // 3) encrypted_dek = encrypt(DEK, master_key)
    // 4) Later: DEK = decrypt(encrypted_dek, master_key), then PAN = decrypt(encrypted_pan, DEK)

    @Column(nullable = false)
    private CardBrand brand; // Card network brand, e.g., VISA/RUPAY.

    @Column(nullable = false)
    private String expiryMonth;

    @Column(nullable = false)
    private String expiryYear;

    @Column(nullable = false)
    private String cardHolderName;

    private LocalDateTime deletedAt;
}
