package io.github.hanziertoo.masterdata.api;

public record BranchView(
        Long id,
        String name,
        Long provinceId,
        boolean active,
        boolean deleted) {
}
