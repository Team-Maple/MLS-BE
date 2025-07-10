package com.maple.api.job.repository;

import com.maple.api.job.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Integer> {
    
    @Query("SELECT j FROM Job j JOIN ItemJob ij ON j.jobId = ij.jobId WHERE ij.itemId = :itemId")
    List<Job> findByItemId(@Param("itemId") Integer itemId);
}