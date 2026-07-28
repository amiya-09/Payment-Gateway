package com.springboot.razorpay.vault.entity;

import com.springboot.razorpay.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vault_card")
public class VaultCard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Column(length = 6, nullable = false)
    private String bin; // first 6 digits of a card

    @Column(nullable = false)
    private byte[] encryptedPan;

    @Column(nullable = false)
    private byte[] encryptedDek; // Dek is used to encrypt the PAN
    // First Generate a random String of 6 digits - called as Dek, using the dek we first encrypt the PAN
    // encrypted_pan = encrypt(PAN, DEK) , Now in future, we also need to decrypt the PAN
    // encrypted_dek = encrypt(DEK, master_key) we encrypt the DEK using the master key,
    // so that we can decrypt the DEK in future and then use the DEK to decrypt the PAN
    // DEK = decrypt(encrypted_dek, master_key) , PAN = decrypt(encrypted_pan, DEK)

    @Column(nullable = false)
    private String brand; // brand of the card : VISA , RUPAY

    @Column(nullable = false)
    private String expiryMonth;

    @Column(nullable = false)
    private String expiryYear;

    @Column(nullable = false)
    private String cardHolderName;

    private LocalDateTime deletedAt;
}
