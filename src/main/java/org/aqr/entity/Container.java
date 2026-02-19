package org.aqr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "containers")
@Data @NoArgsConstructor @AllArgsConstructor
public class Container {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private String image; // URL изображения контейнера

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Container parent;

    private String name;

    @JsonIgnore  // Если оставишь связи
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Container> childContainers = new ArrayList<>();

    @Column(name = "qr_code", length = 100)
    private String qrCode;
}

