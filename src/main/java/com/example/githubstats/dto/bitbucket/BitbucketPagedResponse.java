package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Generic Paged Response DTO - Adjust fields based on actual Bitbucket pagination structure
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPagedResponse<T> {
    private List<T> values;
    private String next; // URL for the next page
    private Integer page;
    private Integer pagelen;
    private Integer size; // Total size if available

    // Getters and Setters
    public List<T> getValues() {
        return values;
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPagelen() {
        return pagelen;
    }

    public void setPagelen(Integer pagelen) {
        this.pagelen = pagelen;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}