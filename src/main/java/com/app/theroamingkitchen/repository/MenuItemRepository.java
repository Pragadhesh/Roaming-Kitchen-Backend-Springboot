package com.app.theroamingkitchen.repository;

import com.app.theroamingkitchen.models.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long>  {
}
