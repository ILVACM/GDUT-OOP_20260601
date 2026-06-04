package com.cps.backend.common.api;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    
    public static <T> PageResult<T> of(org.springframework.data.domain.Page<T> pageData) {
        PageResult<T> r = new PageResult<>();
        r.setContent(pageData.getContent());
        r.setTotalElements(pageData.getTotalElements());
        r.setTotalPages(pageData.getTotalPages());
        r.setPage(pageData.getNumber());
        r.setSize(pageData.getSize());
        return r;
    }
}
