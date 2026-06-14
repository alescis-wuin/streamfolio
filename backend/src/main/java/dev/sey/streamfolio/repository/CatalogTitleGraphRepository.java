package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CatalogTitleGraphRepository {
    Page<CatalogTitle> findPageWithGraph(Specification<CatalogTitle> specification, Pageable pageable);
}
