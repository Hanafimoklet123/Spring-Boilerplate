package com.boilerplate.spring_boot.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Persistable {

    @Id
    @Column("user_id")
    private UUID id;

    private String name;
    private String age;
    private String address;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Transient
    @JsonIgnore
    private boolean newEntity;

    public void setNew(boolean newInstance) {
        this.newEntity = newInstance;
    }

    @JsonIgnore
    public boolean isNew() {
        return newEntity;
    }

}

