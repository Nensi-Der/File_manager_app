package dev.al.FileManagerApplication.specifications;
import dev.al.FileManagerApplication.model.FolderEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class FolderSpecifications {

    public static Specification<FolderEntity> withFilters(Map<String, String> filters) {
        return (Root<FolderEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            for (Map.Entry<String, String> filter : filters.entrySet()) {
                String key = filter.getKey();
                String value = filter.getValue();

                switch (key) {
                    case "name":
                        predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%"));
                        break;
                    case "createdBy":
                        predicate = cb.and(predicate, cb.equal(root.get("createdBy"), value));
                        break;
                    // add other folder-specific filters here if needed, e.g. creation date
                }
            }
            return predicate;
        };
    }

    // Add other folder filters if needed, e.g. parent folder, created date, etc.
}
