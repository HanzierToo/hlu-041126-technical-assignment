package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.api.StoreView;
import io.github.hanziertoo.masterdata.model.Store;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    /*
     * SELECT EXISTS (
     *     SELECT 1 FROM store WHERE branch_id = ? AND deleted = FALSE
     * );
     *
     * Does this branch have at least one store that has not been deleted?
     */
    boolean existsByBranchIdAndDeletedFalse(Long branchId);

    /*
     * SELECT s.*, b.*, p.*
     * FROM store s
     * JOIN branch b ON b.id = s.branch_id
     * JOIN province p ON p.id = b.province_id
     * WHERE s.id = ? AND s.active = TRUE AND s.deleted = FALSE
     *   AND b.active = TRUE AND b.deleted = FALSE
     *   AND p.active = TRUE AND p.deleted = FALSE;
     *
     * Find this store only if the store, its branch, and its province
     * are all active and not deleted. Load the branch and province with the store.
     */
    @Query("""
        select s from Store s join fetch s.branch b join fetch b.province p
        where s.id = :id and s.active = true and s.deleted = false
          and b.active = true and b.deleted = false and p.active = true and p.deleted = false
        """)
    Optional<Store> findVisible(@Param("id") Long id);

    /*
     * SELECT s.id, s.name, b.id, b.name, p.id, p.name
     * FROM store s
     * JOIN branch b ON b.id = s.branch_id
     * JOIN province p ON p.id = b.province_id
     * WHERE s.active = TRUE AND s.deleted = FALSE
     *   AND b.active = TRUE AND b.deleted = FALSE
     *   AND p.active = TRUE AND p.deleted = FALSE
     *   AND (p.id IN (?, ?) OR EXISTS (
     *       SELECT 1 FROM whitelist_store w WHERE w.store_id = s.id
     *   ))
     * ORDER BY s.id ASC
     * LIMIT ? OFFSET ?;
     *
     * Return one ordered page of visible stores from any matching
     * province, plus visible whitelisted stores, without returning a store twice.
     *
     * SELECT COUNT(s.id)
     * FROM store s
     * JOIN branch b ON b.id = s.branch_id
     * JOIN province p ON p.id = b.province_id
     * WHERE s.active = TRUE AND s.deleted = FALSE
     *   AND b.active = TRUE AND b.deleted = FALSE
     *   AND p.active = TRUE AND p.deleted = FALSE
     *   AND (p.id IN (?, ?) OR EXISTS (
     *       SELECT 1 FROM whitelist_store w WHERE w.store_id = s.id
     *   ));
     *
     * Count all stores that meet the same rules, before pagination,
     * so the response can include the total number of results and pages.
     * Two province placeholders are shown; the actual number depends on the matches.
     */
    @Query(value = """
        select new io.github.hanziertoo.masterdata.api.StoreView(s.id, s.name, b.id, b.name, p.id, p.name)
        from Store s join s.branch b join b.province p
        where s.active = true and s.deleted = false
          and b.active = true and b.deleted = false and p.active = true and p.deleted = false
          and (p.id in :provinceIds or exists (select w.storeId from WhitelistStore w where w.storeId = s.id))
        order by s.id asc
        """,
        countQuery = """
        select count(s) from Store s join s.branch b join b.province p
        where s.active = true and s.deleted = false
          and b.active = true and b.deleted = false and p.active = true and p.deleted = false
          and (p.id in :provinceIds or exists (select w.storeId from WhitelistStore w where w.storeId = s.id))
        """)
    Page<StoreView> search(@Param("provinceIds") Collection<Long> provinceIds, Pageable pageable);

    /*
     * SELECT s.id, s.name, b.id, b.name, p.id, p.name
     * FROM store s
     * JOIN branch b ON b.id = s.branch_id
     * JOIN province p ON p.id = b.province_id
     * JOIN whitelist_store w ON w.store_id = s.id
     * WHERE s.active = TRUE AND s.deleted = FALSE
     *   AND b.active = TRUE AND b.deleted = FALSE
     *   AND p.active = TRUE AND p.deleted = FALSE
     * ORDER BY s.id ASC
     * LIMIT ? OFFSET ?;
     *
     * Return one ordered page of whitelisted stores whose store,
     * branch, and province are all active and not deleted.
     *
     * SELECT COUNT(s.id)
     * FROM store s
     * JOIN branch b ON b.id = s.branch_id
     * JOIN province p ON p.id = b.province_id
     * JOIN whitelist_store w ON w.store_id = s.id
     * WHERE s.active = TRUE AND s.deleted = FALSE
     *   AND b.active = TRUE AND b.deleted = FALSE
     *   AND p.active = TRUE AND p.deleted = FALSE;
     *
     * Count all visible whitelisted stores before pagination,
     * so the response can include the total number of results and pages.
     */
    @Query(value = """
        select new io.github.hanziertoo.masterdata.api.StoreView(s.id, s.name, b.id, b.name, p.id, p.name)
        from Store s join s.branch b join b.province p, WhitelistStore w
        where w.storeId = s.id and s.active = true and s.deleted = false
          and b.active = true and b.deleted = false and p.active = true and p.deleted = false
        order by s.id asc
        """,
        countQuery = """
        select count(s) from Store s join s.branch b join b.province p, WhitelistStore w
        where w.storeId = s.id and s.active = true and s.deleted = false
          and b.active = true and b.deleted = false and p.active = true and p.deleted = false
        """)
    Page<StoreView> findVisibleWhitelisted(Pageable pageable);
}
