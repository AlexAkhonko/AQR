package org.aqr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qr_codes")
@Data @NoArgsConstructor @AllArgsConstructor
public class QrCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // Текст QR (ссылка на фото)
    private String image; // URL изображения содержимого

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}

