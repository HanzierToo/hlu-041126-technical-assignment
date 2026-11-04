package io.github.hanziertoo.masterdata.service;

import io.github.hanziertoo.masterdata.api.BranchView;
import io.github.hanziertoo.masterdata.api.PageResponse;
import io.github.hanziertoo.masterdata.api.StoreView;
import io.github.hanziertoo.masterdata.model.Branch;
import io.github.hanziertoo.masterdata.model.Province;
import io.github.hanziertoo.masterdata.model.Store;
import io.github.hanziertoo.masterdata.model.WhitelistStore;
import io.github.hanziertoo.masterdata.repository.BranchRepository;
import io.github.hanziertoo.masterdata.repository.ProvinceRepository;
import io.github.hanziertoo.masterdata.repository.StoreRepository;
import io.github.hanziertoo.masterdata.repository.WhitelistRepository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MasterDataService {

    public enum BranchDeletionPolicy {
        REJECT_IF_STORES_EXIST,
        HIDE_STORES
    }

    private final ProvinceRepository provinces;
    private final BranchRepository branches;
    private final StoreRepository stores;
    private final WhitelistRepository whitelist;
    private final ChangeService changes;
    private final int maxPageSize;
    private final BranchDeletionPolicy deletionPolicy;

    public MasterDataService(ProvinceRepository provinces, BranchRepository branches, StoreRepository stores,
            WhitelistRepository whitelist, ChangeService changes,
            @Value("${app.pagination.max-size}") int maxPageSize,
            @Value("${app.branch-deletion-policy}") BranchDeletionPolicy deletionPolicy) {
        if (maxPageSize < 1 || maxPageSize > 1000) {
            throw new IllegalArgumentException("Invalid maximum page size");
        }

        this.provinces = provinces;
        this.branches = branches;
        this.stores = stores;
        this.whitelist = whitelist;
        this.changes = changes;
        this.maxPageSize = maxPageSize;
        this.deletionPolicy = deletionPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreView> search(String provinceName, int page, int size) {
        if (provinceName == null || provinceName.isBlank() || provinceName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Province name must contain 1 to 120 characters");
        }

        List<Long> provinceIds = provinces.findByNameContainingAndActiveTrueAndDeletedFalse(provinceName.trim())
                .stream()
                .map(Province::getId)
                .toList();

        if (provinceIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Province not found");
        }

        return PageResponse.from(stores.search(provinceIds, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreView> listWhitelist(int page, int size) {
        return PageResponse.from(stores.findVisibleWhitelisted(pageRequest(page, size)));
    }

    @Transactional
    public BranchView updateBranch(Long id, String name, boolean active, String actor) {
        Branch branch = findBranchForUpdate(id);
        BranchView before = toBranchView(branch);

        branch.update(name.trim(), active);

        BranchView after = toBranchView(branch);
        changes.record(actor, "BRANCH", id, "UPDATE", before, after);
        return after;
    }

    @Transactional
    public void deleteBranch(Long id, String actor) {
        Branch branch = findBranchForUpdate(id);

        switch (deletionPolicy) {
            case REJECT_IF_STORES_EXIST -> rejectWhenStoresExist(id);
            case HIDE_STORES -> {
                // Visibility queries hide stores when their branch is soft-deleted.
            }
        }

        BranchView before = toBranchView(branch);
        branch.softDelete();
        changes.record(actor, "BRANCH", id, "DELETE", before, toBranchView(branch));
    }

    private void rejectWhenStoresExist(Long id) {
        if (stores.existsByBranchIdAndDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch contains non-deleted stores");
        }
    }

    @Transactional
    public void addWhitelist(Long storeId, String actor) {
        Store store = stores.findVisible(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visible store not found"));

        if (whitelist.existsById(storeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Store is already whitelisted");
        }

        whitelist.saveAndFlush(new WhitelistStore(storeId));
        changes.record(actor, "WHITELIST", storeId, "ADD", null, Map.of("storeId", store.getId()));
    }

    @Transactional
    public void removeWhitelist(Long storeId, String actor) {
        WhitelistStore entry = whitelist.findForUpdate(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Whitelist entry not found"));

        whitelist.delete(entry);
        changes.record(actor, "WHITELIST", storeId, "REMOVE", Map.of("storeId", storeId), null);
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > maxPageSize || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or page size");
        }
        return PageRequest.of(page, size);
    }

    private Branch findBranchForUpdate(Long id) {
        return branches.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    private BranchView toBranchView(Branch branch) {
        return new BranchView(branch.getId(), branch.getName(), branch.getProvince().getId(),
                branch.isActive(), branch.isDeleted());
    }
}
