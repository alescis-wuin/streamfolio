package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogTitleGraphRepositoryImpl implements CatalogTitleGraphRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<CatalogTitle> findPageWithGraph(Specification<CatalogTitle> specification, Pageable pageable) {
        List<CatalogTitle> pageSkeleton = findSkeleton(specification, pageable.getSort(), pageable);
        long total = count(specification);
        return new PageImpl<>(fetchGraph(pageSkeleton), pageable, total);
    }

    @Override
    public List<CatalogTitle> findAllWithGraph(Specification<CatalogTitle> specification, Sort sort) {
        return fetchGraph(findSkeleton(specification, sort, null));
    }

    private List<CatalogTitle> findSkeleton(Specification<CatalogTitle> specification, Sort sort, Pageable pageable) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CatalogTitle> query = builder.createQuery(CatalogTitle.class);
        Root<CatalogTitle> root = query.from(CatalogTitle.class);
        Predicate predicate = predicate(specification, root, query, builder);

        query.select(root).distinct(true);
        if (predicate != null) {
            query.where(predicate);
        }
        query.orderBy(orders(sort, root, builder));

        TypedQuery<CatalogTitle> typedQuery = entityManager.createQuery(query);
        if (pageable != null) {
            typedQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        return typedQuery.getResultList();
    }

    private List<CatalogTitle> fetchGraph(List<CatalogTitle> skeleton) {
        if (skeleton.isEmpty()) {
            return List.of();
        }
        List<Long> ids = skeleton.stream().map(CatalogTitle::getId).toList();
        Map<Long, Integer> orderById = orderById(ids);
        List<CatalogTitle> titles = fetchTitlesWithGenres(ids);
        fetchTitlesWithVideos(ids);
        titles.sort(Comparator.comparingInt(title -> orderById.getOrDefault(title.getId(), Integer.MAX_VALUE)));
        return titles;
    }

    private long count(Specification<CatalogTitle> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<CatalogTitle> root = query.from(CatalogTitle.class);
        Predicate predicate = predicate(specification, root, query, builder);

        query.select(builder.countDistinct(root));
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<CatalogTitle> fetchTitlesWithGenres(List<Long> ids) {
        return entityManager.createQuery("""
                select distinct title
                from CatalogTitle title
                left join fetch title.genres
                where title.id in :ids
                """, CatalogTitle.class)
            .setParameter("ids", ids)
            .getResultList();
    }

    private void fetchTitlesWithVideos(List<Long> ids) {
        entityManager.createQuery("""
                select distinct title
                from CatalogTitle title
                left join fetch title.videos
                where title.id in :ids
                """, CatalogTitle.class)
            .setParameter("ids", ids)
            .getResultList();
    }

    private Predicate predicate(Specification<CatalogTitle> specification,
                                Root<CatalogTitle> root,
                                CriteriaQuery<?> query,
                                CriteriaBuilder builder) {
        return specification == null ? null : specification.toPredicate(root, query, builder);
    }

    private List<jakarta.persistence.criteria.Order> orders(Sort sort, Root<CatalogTitle> root, CriteriaBuilder builder) {
        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        boolean sortedById = false;
        for (Sort.Order order : sort) {
            Expression<?> expression = root.get(order.getProperty());
            orders.add(order.isAscending() ? builder.asc(expression) : builder.desc(expression));
            sortedById = sortedById || "id".equals(order.getProperty());
        }
        if (!sortedById) {
            orders.add(builder.asc(root.get("id")));
        }
        return orders;
    }

    private Map<Long, Integer> orderById(List<Long> ids) {
        Map<Long, Integer> result = new HashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            result.put(ids.get(index), index);
        }
        return result;
    }
}
