package dev.al.FileManagerApplication.specifications;
import dev.al.FileManagerApplication.model.FileEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class FileSpecifications {

    public static Specification<FileEntity> withFilters(Map<String, String> filters) {
        return (Root<FileEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            for (Map.Entry<String, String> filter : filters.entrySet()) {
                String key = filter.getKey();
                String value = filter.getValue();

                switch (key) {
                    case "name":
                        predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%"));
                        break;
                    case "type":
                        predicate = cb.and(predicate, cb.equal(root.get("type"), value));
                        break;
                    case "createdBy":
                        predicate = cb.and(predicate, cb.equal(root.get("createdBy"), value));
                        break;
                    case "minSize":
                        predicate = cb.and(predicate, cb.ge(root.get("size"), Long.parseLong(value)));
                        break;
                    case "maxSize":
                        predicate = cb.and(predicate, cb.le(root.get("size"), Long.parseLong(value)));
                        break;
                    // add more filters as needed
                }
            }
            return predicate;
        };
    }
}
