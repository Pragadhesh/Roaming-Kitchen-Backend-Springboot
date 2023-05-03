package com.app.theroamingkitchen.models;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Location")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double Latitude;

    private Double Longitude;

    public Location(Double latitude, Double longitude) {
    }
}
