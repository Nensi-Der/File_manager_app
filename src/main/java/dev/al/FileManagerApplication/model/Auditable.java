package dev.al.FileManagerApplication.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedBy
    @Getter
    @Setter
    protected String createdBy;

    @CreatedDate
    @Getter
    @Setter
    protected LocalDateTime createdDate;

    @LastModifiedBy
    @Getter
    @Setter
    protected String lastModifiedBy;

    @LastModifiedDate
    @Getter
    @Setter
    protected LocalDateTime lastModifiedDate;


}