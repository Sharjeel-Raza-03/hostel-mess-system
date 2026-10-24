package com.hostel.mess.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Setting {

    @Id
    @Column(name = "\"key\"", length = 50)
    private String settingKey;

    @Column(name = "\"value\"", length = 50)
    private String settingValue;
}