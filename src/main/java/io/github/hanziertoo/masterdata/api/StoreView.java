package io.github.hanziertoo.masterdata.api;

public record StoreView(
        Long id,
        String name,
        Long branchId,
        String branchName,
        Long provinceId,
        String provinceName) {
}
