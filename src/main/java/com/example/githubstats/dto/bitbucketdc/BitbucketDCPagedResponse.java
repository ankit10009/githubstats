package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCPagedResponse<T> {
    private int size; // Number of items on the current page
    private int limit; // Page size limit
    private boolean isLastPage;
    private List<T> values;
    private int start; // Starting index for this page
    @JsonProperty("nextPageStart")
    private Integer nextPageStart; // Starting index for the next page, null if last page

    // Getters & Setters
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public boolean isLastPage() { return isLastPage; }
    public void setLastPage(boolean lastPage) { isLastPage = lastPage; }
    public List<T> getValues() { return values; }
    public void setValues(List<T> values) { this.values = values; }
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }
    public Integer getNextPageStart() { return nextPageStart; }
    public void setNextPageStart(Integer nextPageStart) { this.nextPageStart = nextPageStart; }
}