package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public interface CatalogTitleGraphRepository {
    Page<CatalogTitle> findPageWithGraph(Specification<CatalogTitle> specification, Pageable pageable);

    List<CatalogTitle> findAllWithGraph(Specification<CatalogTitle> specification, Sort sort);
}
