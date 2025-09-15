package dev.mfikri.widuriestock.repository;

public interface ProductSummary {
    Integer getId();
    String getName();
    String getDescription();
    Integer getCategoryId();
    String getCategoryName();
    String getImageLocation();
}
