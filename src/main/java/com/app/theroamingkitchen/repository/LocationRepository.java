package com.app.theroamingkitchen.repository;

import com.app.theroamingkitchen.models.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
