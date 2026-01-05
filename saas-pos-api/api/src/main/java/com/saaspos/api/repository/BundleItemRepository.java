package com.saaspos.api.repository;

import com.saaspos.api.model.BundleItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface BundleItemRepository extends JpaRepository<BundleItem, UUID> {

    // Borrar por el ID del Padre (El Pack)
    @Modifying
    @Transactional
    @Query("DELETE FROM BundleItem b WHERE b.bundleProduct.id = :bundleId")
    void deleteByBundleProductId(@Param("bundleId") UUID bundleId);

    // Borrar por el ID del Hijo (Si borras un perfume, que salga de los packs)
    @Modifying
    @Transactional
    @Query("DELETE FROM BundleItem b WHERE b.componentProduct.id = :componentId")
    void deleteByComponentProductId(@Param("componentId") UUID componentId);
}