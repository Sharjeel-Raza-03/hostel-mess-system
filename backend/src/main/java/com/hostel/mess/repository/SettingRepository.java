package com.hostel.mess.repository;

import com.hostel.mess.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SettingRepository extends JpaRepository<Setting, String> {

    List<Setting> findBySettingKeyIn(Collection<String> keys);
}