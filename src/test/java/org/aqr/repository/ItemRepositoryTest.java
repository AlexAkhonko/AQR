package org.aqr.repository;

import org.aqr.entity.Item;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ItemRepositoryTest {

    @Autowired ItemRepository repo;

    @Test
    void findByOwnerId_pagination() {
        Pageable pageable = PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("id").descending());
        Page<Item> page = repo.findByOwnerId(1L, pageable);  // alice

        Assertions.assertThat(page.getContent()).hasSize(5);
        Assertions.assertThat(page.getTotalElements()).isEqualTo(13);
    }

    @Test void findByOwnerIdAndTextLikeIgnoreCase() {
        List<Item> hats = repo.findByOwnerIdAndTextLikeIgnoreCase(1L, "%шляп%");
        Assertions.assertThat(hats).hasSizeGreaterThan(1)
                .extracting(Item::getText).contains("Красные шляпы", "Синие ШЛЯПЫ");
    }

    @Test void searchFullText_ftsMorphology() {
        List<Item> results = repo.searchFullText(1L, "шляпн");  // Находит "шляпы"
        Assertions.assertThat(results).isNotEmpty();
    }

    @Test void countByOwnerId() {
        Long count = repo.countByOwnerId(1L);
        Assertions.assertThat(count).isEqualTo(13L);
    }

    @Test void findTop10ByOwnerIdOrderByIdDesc() {
        List<Item> top = repo.findTop10ByOwnerIdOrderByIdDesc(1L);
        Assertions.assertThat(top).hasSize(10);
    }
}