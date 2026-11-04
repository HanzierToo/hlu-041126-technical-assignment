package io.github.hanziertoo.masterdata.api;

import io.github.hanziertoo.masterdata.service.MasterDataService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class MasterDataController {

    private final MasterDataService service;

    public MasterDataController(MasterDataService service) {
        this.service = service;
    }

    @GetMapping("/stores")
    public PageResponse<StoreView> search(@RequestParam String province,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.search(province, page, size);
    }

    @GetMapping("/whitelist")
    public PageResponse<StoreView> whitelist(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.listWhitelist(page, size);
    }

    @PutMapping("/branches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable @Positive Long id, @Valid @RequestBody BranchUpdate body, Principal principal) {
        service.updateBranch(id, body.name(), body.active(), principal.getName());
    }

    @DeleteMapping("/branches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id, Principal principal) {
        service.deleteBranch(id, principal.getName());
    }

    @PostMapping("/whitelist/{storeId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@PathVariable @Positive Long storeId, Principal principal) {
        service.addWhitelist(storeId, principal.getName());
    }

    @DeleteMapping("/whitelist/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable @Positive Long storeId, Principal principal) {
        service.removeWhitelist(storeId, principal.getName());
    }

    public record BranchUpdate(
            @NotBlank @Size(max = 120) String name,
            @NotNull Boolean active) {
    }
}
