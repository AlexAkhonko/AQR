package org.aqr.entity;

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
    @JoinColumn(name = "parent_container_id")
    private Container parentContainer;

    @OneToMany(mappedBy = "parentContainer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Container> childContainers = new ArrayList<>();
}

